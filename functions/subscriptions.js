/**
 * Play subscription verification (Firebase Cloud Function, 2nd gen).
 *
 * Purpose: independently confirm a Play Billing purchase with Google (never
 * trusting whatever the client claims about its own subscription state), and
 * record the verified result in Firestore so `openaiProxy` can gate requests
 * on it. Two entry points write that verified state: `verifySubscription`
 * (an explicit client call, right after a purchase) and
 * `onSubscriptionNotification` (Play's Real-Time Developer Notifications,
 * firing on every later lifecycle event — renewal, cancellation, grace,
 * expiry — so the record doesn't go stale between client-triggered checks).
 * See docs/play-store-release-plan.md item #4 for the original design this
 * shipped from, and functions/README.md for the RTDN setup.
 *
 * Auth: this function runs as its own dedicated service account
 * (play-developer-api@<project>.iam.gserviceaccount.com, configured via the
 * `serviceAccount` option below) which has been granted "View financial
 * data" access to this app in Play Console. That grant is what lets
 * `google-auth-library`'s Application Default Credentials call the Play
 * Developer API here with no key file to manage or rotate, unlike
 * `OPENAI_KEY` in index.js.
 */

const { onRequest } = require("firebase-functions/v2/https");
const { onMessagePublished } = require("firebase-functions/v2/pubsub");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { GoogleAuth } = require("google-auth-library");
const { google } = require("googleapis");

const db = admin.firestore();

const PACKAGE_NAME = "dev.gaborbiro.dailymacros";
const PLAY_SERVICE_ACCOUNT = "play-developer-api@dailymacros-9fab8.iam.gserviceaccount.com";

// Pub/Sub topic Play Console's "Real-time developer notifications" setting
// publishes to. The topic and the IAM grant letting Play's publishing service
// account post to it are one-time manual setup (see functions/README.md) —
// deploying `onSubscriptionNotification` only provisions the subscription
// that reads from it.
const PLAY_RTDN_TOPIC = "play-rtdn";

const androidPublisherAuth = new GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/androidpublisher"],
});

function sendError(res, status, code, message) {
  res.status(status).json({ error: { message, type: "verify_error", code } });
}

/**
 * Maps a Play `subscriptionState` (see purchases.subscriptionsv2.get) to the
 * state we persist. A canceled subscription still counts as entitled until
 * its paid period actually runs out — `openaiProxy`'s gate checks
 * expiryTimeMillis for exactly this reason.
 */
function toEntitlementState(subscriptionState) {
  if (subscriptionState === "SUBSCRIPTION_STATE_ACTIVE") return "active";
  if (subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD") return "grace";
  if (subscriptionState === "SUBSCRIPTION_STATE_CANCELED") return "canceled";
  return "expired";
}

/**
 * The one place that calls the Play Developer API and persists the result.
 * Used by both the client-triggered `verifySubscription` HTTP call and
 * `onSubscriptionNotification` (RTDN), so every path that can learn a
 * purchase changed goes through the same lookup + storage logic.
 *
 * Also (re)writes `purchaseTokens/{purchaseToken} -> { uid }`: an RTDN
 * message only carries the purchase token, never the Firebase uid, so this
 * is how `onSubscriptionNotification` finds whose `subscriptions/{uid}` doc
 * to refresh.
 */
async function verifyAndStore(uid, purchaseToken, productIdHint) {
  const authClient = await androidPublisherAuth.getClient();
  const androidPublisher = google.androidpublisher({ version: "v3", auth: authClient });
  const response = await androidPublisher.purchases.subscriptionsv2.get({
    packageName: PACKAGE_NAME,
    token: purchaseToken,
  });
  const subscription = response.data;

  const lineItem = (subscription.lineItems || [])[0];
  if (!lineItem) {
    throw new Error("Play response had no line items.");
  }
  const expiryTimeMillis = new Date(lineItem.expiryTime).getTime();
  const state = toEntitlementState(subscription.subscriptionState);

  await Promise.all([
    db.doc(`subscriptions/${uid}`).set(
      {
        state,
        productId: lineItem.productId || productIdHint,
        expiryTimeMillis,
        purchaseToken,
        updatedAt: Date.now(),
      },
      { merge: true },
    ),
    db.doc(`purchaseTokens/${purchaseToken}`).set({ uid }, { merge: true }),
  ]);

  return { state, expiryTimeMillis };
}

exports.verifySubscription = onRequest(
  {
    region: "us-central1",
    serviceAccount: PLAY_SERVICE_ACCOUNT,
    timeoutSeconds: 30,
    memory: "256MiB",
    cors: false,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      sendError(res, 405, "method_not_allowed", "Only POST is supported.");
      return;
    }

    const authHeader = req.get("Authorization") || "";
    const match = authHeader.match(/^Bearer (.+)$/);
    if (!match) {
      sendError(res, 401, "unauthenticated", "Missing Firebase ID token.");
      return;
    }
    let uid;
    try {
      const decoded = await admin.auth().verifyIdToken(match[1]);
      uid = decoded.uid;
    } catch (e) {
      sendError(res, 401, "unauthenticated", "Invalid Firebase ID token.");
      return;
    }

    const purchaseToken = req.body && req.body.purchaseToken;
    const productId = req.body && req.body.productId;
    if (!purchaseToken || !productId) {
      sendError(res, 400, "invalid_request", "purchaseToken and productId are required.");
      return;
    }

    let result;
    try {
      result = await verifyAndStore(uid, purchaseToken, productId);
    } catch (e) {
      logger.error("Subscription verification failed", e);
      sendError(res, 502, "play_api_error", "Could not verify purchase with Play.");
      return;
    }

    res.status(200).json(result);
  },
);

/**
 * Play's Real-Time Developer Notifications: fires on every subscription
 * lifecycle event (renewed, canceled, entered/left grace, on hold, expired,
 * ...) so `subscriptions/{uid}` stays current without depending on the
 * client ever reopening the app or re-verifying a purchase token it already
 * thinks it has verified. See functions/README.md for the one-time Pub/Sub
 * topic + Play Console wiring this depends on.
 *
 * Deliberately ignores `notificationType` beyond routing — Google's own
 * guidance is to treat any subscriptionNotification as "go re-fetch the
 * current truth from the Play Developer API", not to infer state from the
 * type code.
 */
exports.onSubscriptionNotification = onMessagePublished(
  { topic: PLAY_RTDN_TOPIC, region: "us-central1", serviceAccount: PLAY_SERVICE_ACCOUNT },
  async (event) => {
    let payload;
    try {
      payload = event.data.message.json;
    } catch (e) {
      logger.error("RTDN message was not valid JSON", e);
      return;
    }

    if (payload.testNotification) {
      logger.info("RTDN test notification received", payload.testNotification);
      return;
    }
    if (payload.packageName && payload.packageName !== PACKAGE_NAME) {
      logger.warn("RTDN for unexpected package, ignoring", { packageName: payload.packageName });
      return;
    }
    const notification = payload.subscriptionNotification;
    if (!notification) {
      logger.info("RTDN with no subscriptionNotification, ignoring", payload);
      return;
    }

    const { purchaseToken, subscriptionId, notificationType } = notification;
    const mapping = await db.doc(`purchaseTokens/${purchaseToken}`).get();
    if (!mapping.exists) {
      // Most likely this is the very first notification for a brand-new
      // purchase and the client's own verifySubscription call (which creates
      // this mapping) just hasn't landed yet. Nothing to refresh yet; the
      // client-triggered verify will populate subscriptions/{uid} shortly.
      logger.warn("RTDN for unknown purchaseToken, skipping", { notificationType });
      return;
    }
    const { uid } = mapping.data();

    try {
      await verifyAndStore(uid, purchaseToken, subscriptionId);
      logger.info("RTDN processed", { uid, notificationType });
    } catch (e) {
      logger.error("RTDN re-verify failed", e);
      throw e; // Let Pub/Sub retry delivery.
    }
  },
);

// How far back to look each run. Re-scanning this window every run (rather
// than tracking a "since last run" cursor) means a missed/failed run just
// gets picked up by the next one — simpler and self-healing, at the cost of
// re-processing a few days of already-handled entries each time (harmless:
// revoking an already-revoked doc is a no-op write).
const VOIDED_PURCHASES_LOOKBACK_MILLIS = 3 * 24 * 60 * 60 * 1000; // 3 days

/**
 * Refunds and chargebacks are NOT covered by RTDN — Play has no push
 * notification for them. The only way to learn about one is to poll the
 * separate Voided Purchases API, which is what this does (Google's own
 * guidance is to check it at least daily; this runs every 6 hours).
 *
 * A voided purchase only revokes `subscriptions/{uid}` if that uid's stored
 * `purchaseToken` still matches the voided one — if the user has since
 * bought a fresh subscription with a new token, this old void is stale news
 * and must not clobber the new, legitimate purchase.
 *
 * NOTE: field/enum names below (`type`, `productType`, `tokenPagination`)
 * are per Google's documented Voided Purchases API shape at the time this
 * was written — verify against the current Play Developer API docs and test
 * against a real refunded test purchase (Play Console → Order management →
 * refund) before relying on this in production.
 */
exports.checkVoidedPurchases = onSchedule(
  {
    schedule: "every 6 hours",
    region: "us-central1",
    serviceAccount: PLAY_SERVICE_ACCOUNT,
    timeoutSeconds: 120,
  },
  async () => {
    const authClient = await androidPublisherAuth.getClient();
    const androidPublisher = google.androidpublisher({ version: "v3", auth: authClient });

    const now = Date.now();
    const startTime = String(now - VOIDED_PURCHASES_LOOKBACK_MILLIS);
    const endTime = String(now);

    let pageToken;
    let revoked = 0;
    let seen = 0;
    do {
      const response = await androidPublisher.purchases.voidedpurchases.list({
        packageName: PACKAGE_NAME,
        startTime,
        endTime,
        type: 2, // subscriptions only
        token: pageToken,
      });

      const voidedPurchases = response.data.voidedPurchases || [];
      for (const voided of voidedPurchases) {
        seen++;
        const purchaseToken = voided.purchaseToken;
        if (!purchaseToken) continue;

        const mapping = await db.doc(`purchaseTokens/${purchaseToken}`).get();
        if (!mapping.exists) continue; // never verified this token, nothing to revoke

        const { uid } = mapping.data();
        const subRef = db.doc(`subscriptions/${uid}`);
        const subSnap = await subRef.get();
        const sub = subSnap.data();
        if (sub && sub.purchaseToken === purchaseToken) {
          await subRef.set(
            { state: "revoked", expiryTimeMillis: 0, voidedAt: Date.now(), updatedAt: Date.now() },
            { merge: true },
          );
          revoked++;
          logger.info("Revoked subscription for voided purchase", { uid, purchaseToken });
        }
      }

      pageToken = response.data.tokenPagination && response.data.tokenPagination.nextPageToken;
    } while (pageToken);

    logger.info("checkVoidedPurchases run complete", { seen, revoked });
  },
);
