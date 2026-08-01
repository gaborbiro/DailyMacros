/**
 * Play subscription verification (Firebase Cloud Function, 2nd gen).
 *
 * Purpose: independently confirm a Play Billing purchase with Google (never
 * trusting whatever the client claims about its own subscription state), and
 * record the verified result in Firestore so `openaiProxy` can gate requests
 * on it. See docs/play-store-release-plan.md item #4 and the plan this
 * shipped from for the full design rationale (why this is a separate
 * function from openaiProxy, why RTDN is deferred, etc).
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
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { GoogleAuth } = require("google-auth-library");
const { google } = require("googleapis");

const db = admin.firestore();

const PACKAGE_NAME = "dev.gaborbiro.dailymacros";
const PLAY_SERVICE_ACCOUNT = "play-developer-api@dailymacros-9fab8.iam.gserviceaccount.com";

const androidPublisherAuth = new GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/androidpublisher"],
});

function sendError(res, status, code, message) {
  res.status(status).json({ error: { message, type: "verify_error", code } });
}

/**
 * Maps a Play `subscriptionState` (see purchases.subscriptionsv2.get) plus the
 * line item's expiry to the state we persist. A canceled subscription still
 * counts as entitled until its paid period actually runs out — this makes
 * `openaiProxy`'s gate self-healing against natural expiry without needing a
 * fresh verify call at expiry time (see plan's "Why this is still correct
 * without RTDN").
 */
function toEntitlementState(subscriptionState, expiryTimeMillis) {
  if (subscriptionState === "SUBSCRIPTION_STATE_ACTIVE") return "active";
  if (subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD") return "grace";
  if (subscriptionState === "SUBSCRIPTION_STATE_CANCELED") return "canceled";
  return "expired";
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

    let subscription;
    try {
      const authClient = await androidPublisherAuth.getClient();
      const androidPublisher = google.androidpublisher({ version: "v3", auth: authClient });
      const response = await androidPublisher.purchases.subscriptionsv2.get({
        packageName: PACKAGE_NAME,
        token: purchaseToken,
      });
      subscription = response.data;
    } catch (e) {
      logger.error("Play Developer API lookup failed", e);
      sendError(res, 502, "play_api_error", "Could not verify purchase with Play.");
      return;
    }

    const lineItem = (subscription.lineItems || [])[0];
    if (!lineItem) {
      sendError(res, 502, "play_api_error", "Play response had no line items.");
      return;
    }
    const expiryTimeMillis = new Date(lineItem.expiryTime).getTime();
    const state = toEntitlementState(subscription.subscriptionState, expiryTimeMillis);

    try {
      await db.doc(`subscriptions/${uid}`).set(
        {
          state,
          productId: lineItem.productId || productId,
          expiryTimeMillis,
          updatedAt: Date.now(),
        },
        { merge: true },
      );
    } catch (e) {
      logger.error("Failed to persist subscription state", e);
      sendError(res, 500, "storage_error", "Could not store verified subscription state.");
      return;
    }

    res.status(200).json({ state, expiryTimeMillis });
  },
);
