/**
 * DailyMacros OpenAI proxy (Firebase Cloud Function, 2nd gen).
 *
 * Purpose: keep the OpenAI key server-side and enforce spending caps so a bug,
 * an abuser, or organic growth can never run the bill past a known ceiling.
 *
 * Flow for a normal request:
 *   1. Verify the caller's Firebase ID token (anonymous auth is fine) -> uid.
 *   2. In one Firestore transaction, check the kill switch, the global monthly
 *      request budget, the subscription gate (with a small pre-subscription
 *      allowance for the two AI features new users need before they'll ever
 *      pay), and the per-user daily cap. Increment the relevant counters.
 *   3. Forward the JSON body verbatim to OpenAI's /v1/responses with the real
 *      key and return OpenAI's status + body unchanged, so the Android client's
 *      existing response/error parsing keeps working.
 *
 * Subscription gate: a subscription (users/{uid}.subscription*, written by
 * verifySubscription in subscriptions.js) is ALWAYS required, for everyone
 * not on the unlimitedClientIds allowlist, with one exception: a brand-new,
 * not-yet-subscribed user gets a small pre-subscription allowance, shared
 * across the "recognition" and "analysis" features (see PRESUB_ELIGIBLE_
 * FEATURES below), so they can experience real value before being asked to
 * pay. Every other feature (weekly/ongoing insights, or a request with no
 * recognised X-Feature header) requires a subscription unconditionally.
 *
 * Pre-subscription allowance: a single pool, shared between both eligible
 * features, of two lifetime (never-reset) per-user caps in users/{uid}:
 *   - a TOTAL-attempts cap, incremented server-side before every allowed
 *     call (same "count it even if it fails" philosophy as the caps below) -
 *     this is the real ceiling and is never trusted to the client.
 *   - a SUCCESS cap, incremented only when the client determines the call
 *     actually produced something useful (food recognised, macros
 *     analysed), via a self-reported call to this same function with
 *     ?report=1 (see near the bottom of the handler). This is NOT
 *     independently verified - the client could lie and always report no
 *     success - but that's an acceptable trust model because the
 *     server-verified TOTAL cap still bounds a lying client just the same,
 *     it just costs them their full total allowance instead of tripping
 *     the success cap first.
 * A call to either feature counts against the same pool, so once either cap
 * is hit, BOTH features require a real subscription from then on - there's
 * no way to keep using one feature after exhausting the allowance on the
 * other. Tune the caps live in Firestore at config/limits without
 * redeploying (see functions/README.md).
 *
 * Metering is by REQUEST COUNT (a tripwire), not exact token cost: ~3000
 * requests/month ~= $30 at ~$0.01/call. Tune the numbers live in Firestore at
 * config/limits without redeploying.
 *
 * Overrides (all live in Firestore, no redeploy):
 *   - config/limits.unlimitedClientIds: [ "apple-fox-moon", { "clientId":
 *     "brave-tiger-dune", "note": "why they got this" }, ... ] — clients on
 *     this list bypass the per-user DAILY cap and the subscription gate
 *     (including the pre-subscription feature caps) entirely, but are still
 *     counted and still subject to the global monthly budget. Entries can be
 *     a bare three-word id, or an object with a "note" field to record who
 *     the grant is for and why - the note is never read by this function, it's
 *     just there so you can tell entries apart later. Use it to permanently
 *     unlock yourself: put your own three-word id (from the app's Settings
 *     screen) here once.
 *   - To give one user more room today, edit their users/{uid}.dailyCapCount:
 *     0 restores their full daily allowance, a negative value (e.g. -10) grants
 *     that many extra requests on top of the cap. It resets to normal at the
 *     next UTC day.
 *
 * Every request records the caller's three-word client id (X-Client-Id header)
 * and last-seen time on users/{uid} - AND writes it to a separate
 * clientIds/{clientId} -> {uid} doc, unconditionally, before any allow/deny
 * decision. That second write is the one that matters for support: it's a
 * direct-by-id lookup (no query needed), and unlike the copy on users/{uid}
 * it survives that doc being lost entirely - the whole reason it exists.
 *
 * Self-healing a lost users/{uid} doc: if a subscriber's doc is ever missing
 * (e.g. an accidental Firestore delete) they'd otherwise be treated as a
 * brand-new user until their next real Play lifecycle event triggers RTDN.
 * Instead, the first time we see !userSnap.exists on a not-entitled request,
 * we check the separate (delete-immune) purchaseTokens collection for a
 * token on file for this uid; if found, we re-verify with Play and retry the
 * same request once with fresh data rather than denying it. See
 * subscriptions.js's repairSubscription for the manual, on-demand version of
 * the same repair.
 *
 * Counters are incremented BEFORE the upstream call, so a failed OpenAI call
 * still consumes quota. That is intentional for a safety cap: we would rather
 * slightly over-count than let retries slip past the ceiling.
 *
 * Day/month boundaries are UTC.
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const subscriptions = require("./subscriptions");
exports.verifySubscription = subscriptions.verifySubscription;
exports.onSubscriptionNotification = subscriptions.onSubscriptionNotification;
exports.checkVoidedPurchases = subscriptions.checkVoidedPurchases;
exports.repairSubscription = subscriptions.repairSubscription;

// Set with: firebase functions:secrets:set OPENAI_KEY
const OPENAI_KEY = defineSecret("OPENAI_KEY");

const OPENAI_URL = "https://api.openai.com/v1/responses";

// Fallbacks used only if config/limits is missing a field.
const DEFAULT_PER_USER_DAILY_CAP = 15;
const DEFAULT_MONTHLY_REQUEST_BUDGET = 3000;

// subscriptionState values (see subscriptions.js's toStoredState) that count
// as entitled, alongside the expiryTimeMillis check below. Everything else
// Google can report (on_hold, paused, pending, unspecified, expired) is
// deliberately excluded here rather than folded into some catch-all state.
const ENTITLED_STATES = new Set(["active", "in_grace_period", "canceled"]);

/**
 * The only two features a not-yet-subscribed user may use at all. Anything
 * else (weekly insights, ongoing insights, a request with no/unrecognised
 * X-Feature header) has no pre-subscription allowance - see the gate below.
 * Both features draw from the same shared pool (PRESUB_* fields below), so
 * usage of one counts against the other's remaining allowance too.
 */
const PRESUB_ELIGIBLE_FEATURES = new Set(["recognition", "analysis"]);
const PRESUB_TOTAL_FIELD = "preSubTotal";
const PRESUB_SUCCESS_FIELD = "preSubSuccess";
const PRESUB_TOTAL_CAP_FIELD = "preSubTotalCap";
const PRESUB_SUCCESS_CAP_FIELD = "preSubSuccessCap";
const PRESUB_DEFAULT_TOTAL_CAP = 9;
const PRESUB_DEFAULT_SUCCESS_CAP = 6;

/** OpenAI-shaped error envelope so the client's existing error parser handles it. */
function sendError(res, status, code, message) {
  res.status(status).json({ error: { message, type: "proxy_error", code } });
}

exports.openaiProxy = onRequest(
  {
    region: "us-central1", // co-located with OpenAI; keep the upstream hop short.
    secrets: [OPENAI_KEY],
    minInstances: 1, // keep one instance warm -> no cold-start latency spikes.
    timeoutSeconds: 120,
    memory: "512MiB", // base64 image payloads can be a few MB.
    cors: false,
  },
  async (req, res) => {
    // Health check: no auth, returns immediately. Use it to measure the added
    // network hop (app -> function -> app) in isolation from the OpenAI call.
    if (req.query.health !== undefined) {
      res.status(200).json({ ok: true });
      return;
    }

    if (req.method !== "POST") {
      sendError(res, 405, "method_not_allowed", "Only POST is supported.");
      return;
    }

    // 1. Authenticate.
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

    // Self-reported "that pre-subscription call actually produced something
    // useful" signal from the client (see PRESUB_ELIGIBLE_FEATURES doc above)
    // - not an OpenAI call, just a Firestore increment. Never trusted for the
    // TOTAL cap, only for the softer SUCCESS cap.
    if (req.query.report !== undefined) {
      const feature = req.body && req.body.feature;
      if (!PRESUB_ELIGIBLE_FEATURES.has(feature)) {
        sendError(res, 400, "invalid_request", "feature must be 'recognition' or 'analysis'.");
        return;
      }
      try {
        await db.doc(`users/${uid}`).set(
          { [PRESUB_SUCCESS_FIELD]: admin.firestore.FieldValue.increment(1) },
          { merge: true },
        );
        res.status(200).json({ ok: true });
      } catch (e) {
        logger.error("Outcome report failed", e);
        sendError(res, 500, "storage_error", "Could not record outcome.");
      }
      return;
    }

    // Three-word client id the user sees in Settings. Stored on the usage doc so
    // a support email maps to a row. Clamped so a bad/spoofed header can't bloat
    // the document; null if absent (older app builds).
    const clientId = (req.get("X-Client-Id") || "").slice(0, 64) || null;

    // Which AI feature this call is for (see PRESUB_ELIGIBLE_FEATURES);
    // null/unrecognised means "not eligible for a pre-subscription allowance".
    const featureKey = req.get("X-Feature") || null;

    // 2. Enforce caps atomically.
    const now = new Date();
    const monthKey = now.toISOString().slice(0, 7); // YYYY-MM (UTC)
    const dailyCapUtcDateKey = now.toISOString().slice(0, 10); // YYYY-MM-DD (UTC)

    const configRef = db.doc("config/limits");
    const globalRef = db.doc("usage/global");
    const userRef = db.doc(`users/${uid}`);

    const runCapCheck = async (tx) => {
      const [configSnap, globalSnap, userSnap] = await Promise.all([
        tx.get(configRef),
        tx.get(globalRef),
        tx.get(userRef),
      ]);

      const cfg = configSnap.data() || {};
      const perUserDailyCap = cfg.perUserDailyCap ?? DEFAULT_PER_USER_DAILY_CAP;
      const monthlyBudget = cfg.monthlyRequestBudget ?? DEFAULT_MONTHLY_REQUEST_BUDGET;
      // Each entry is either a bare three-word id, or { clientId, note } so you
      // can record who a grant belongs to and why (see header comment above).
      const unlimitedClientIds = Array.isArray(cfg.unlimitedClientIds)
        ? cfg.unlimitedClientIds.map((entry) => (typeof entry === "string" ? entry : entry?.clientId))
        : [];

      const u = userSnap.data() || {};
      // Allowlisted clients skip both the daily cap (checked below) and the
      // subscription gate entirely (developer testing, reviewers) - still
      // counted, still bounded by the global monthly budget checked above.
      const isUnlimited = clientId != null && unlimitedClientIds.includes(clientId);
      const nowMillis = Date.now();
      // expiryTimeMillis is checked for every state, not just "canceled":
      // onSubscriptionNotification (RTDN) refreshes this doc on every real
      // lifecycle event, so a stale expiry is a signal something's wrong (a
      // missed notification, a lapsed client that never re-verified) rather
      // than something to trust indefinitely.
      const entitled = u.subscriptionExpiryTimeMillis > nowMillis
        && ENTITLED_STATES.has(u.subscriptionState);

      // Firestore transactions require every read to happen before any
      // write, so this conditional lookup - the one read whose need only
      // becomes clear after the reads above - has to happen here, before
      // the clientIds write below, rather than inline further down where
      // it's logically triggered.
      //
      // !userSnap.exists means this uid has never completed a single
      // allowed request (the only place userRef is ever written is below,
      // on success) - either a genuinely brand-new user, or a uid whose
      // record was lost (e.g. an accidental Firestore delete). Tell them
      // apart via purchaseTokens, a separate document unaffected by
      // users/{uid} being wiped, before treating this as "never
      // subscribed". Only worth the extra read on this rare, bounded case -
      // not on every denied request from a real never-subscribed user
      // (their doc exists once they've had any allowed call, so this
      // doesn't recur for them).
      let selfHealPurchaseToken = null;
      if (!isUnlimited && !entitled && !userSnap.exists) {
        const tokenQuery = await tx.get(
          db.collection("purchaseTokens").where("uid", "==", uid).limit(1),
        );
        if (!tokenQuery.empty) {
          selfHealPurchaseToken = tokenQuery.docs[0].id;
        }
      }

      // All reads are done - everything from here on is a write or an
      // early return, never another tx.get().

      // Written unconditionally, before any allow/deny decision below, so it
      // survives even on a denied request and even a users/{uid} delete: a
      // separate document (mirrors purchaseTokens) is the only way a
      // support agent who's just been told a three-word id, and nothing
      // else, can ever get back to a uid once the profile doc holding
      // clientId is gone.
      if (clientId != null) {
        tx.set(db.doc(`clientIds/${clientId}`), { uid }, { merge: true });
      }

      if (cfg.killSwitch === true) {
        return { allow: false, status: 503, code: "kill_switch", message: "Service temporarily unavailable." };
      }

      const g = globalSnap.data();
      const globalCount = g && g.month === monthKey ? g.count || 0 : 0;
      if (globalCount >= monthlyBudget) {
        return { allow: false, status: 503, code: "monthly_budget", message: "Service is at capacity for this month. Please try again later." };
      }

      let preSubIncrementField = null;
      if (!isUnlimited && !entitled) {
        if (selfHealPurchaseToken) {
          return { allow: false, code: "self_heal", selfHealPurchaseToken };
        }
        if (!PRESUB_ELIGIBLE_FEATURES.has(featureKey)) {
          return { allow: false, status: 403, code: "subscription_required", message: "A subscription is required to continue using AI analysis." };
        }
        const totalCap = cfg[PRESUB_TOTAL_CAP_FIELD] ?? PRESUB_DEFAULT_TOTAL_CAP;
        const successCap = cfg[PRESUB_SUCCESS_CAP_FIELD] ?? PRESUB_DEFAULT_SUCCESS_CAP;
        const totalCount = u[PRESUB_TOTAL_FIELD] || 0;
        const successCount = u[PRESUB_SUCCESS_FIELD] || 0;
        if (totalCount >= totalCap || successCount >= successCap) {
          return { allow: false, status: 403, code: "subscription_required", message: "A subscription is required to continue using AI analysis." };
        }
        preSubIncrementField = PRESUB_TOTAL_FIELD;
      }

      // Allowlisted clients skip the per-user daily cap (still counted below,
      // still bounded by the global monthly budget checked above).
      const userCount = u.dailyCapUtcDate === dailyCapUtcDateKey ? u.dailyCapCount || 0 : 0;
      if (!isUnlimited && userCount >= perUserDailyCap) {
        return { allow: false, status: 429, code: "daily_cap", message: "You've reached today's analysis limit. Please try again tomorrow." };
      }

      tx.set(globalRef, { month: monthKey, count: globalCount + 1 }, { merge: true });
      const userUpdate = { dailyCapUtcDate: dailyCapUtcDateKey, dailyCapCount: userCount + 1, lastSeen: now.toISOString() };
      if (clientId != null) userUpdate.clientId = clientId;
      if (preSubIncrementField != null) userUpdate[preSubIncrementField] = (u[preSubIncrementField] || 0) + 1;
      tx.set(userRef, userUpdate, { merge: true });
      return { allow: true };
    };

    let decision;
    try {
      decision = await db.runTransaction(runCapCheck);
      if (decision.code === "self_heal") {
        logger.warn("users/{uid} missing but a purchase token is on file, self-healing", { uid, purchaseToken: decision.selfHealPurchaseToken });
        let healed = false;
        try {
          await subscriptions.verifyAndStore(uid, decision.selfHealPurchaseToken, null);
          healed = true;
        } catch (e) {
          logger.error("Self-heal verify failed", e);
        }
        if (healed) {
          decision = await db.runTransaction(runCapCheck);
        }
        if (decision.code === "self_heal") {
          // Repair didn't stick (Play API failure, or something odd) - don't
          // leak the internal retry signal to the client, just deny normally.
          decision = { allow: false, status: 403, code: "subscription_required", message: "A subscription is required to continue using AI analysis." };
        }
      }
    } catch (e) {
      logger.error("Cap transaction failed", e);
      sendError(res, 500, "cap_check_failed", "Could not verify usage limits.");
      return;
    }

    if (!decision.allow) {
      logger.info("Request blocked", { uid, reason: decision.code });
      sendError(res, decision.status, decision.code, decision.message);
      return;
    }

    // 3. Forward to OpenAI, return the response verbatim.
    try {
      const upstream = await fetch(OPENAI_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${OPENAI_KEY.value()}`,
        },
        body: JSON.stringify(req.body),
      });
      const text = await upstream.text();
      res.status(upstream.status).set("Content-Type", "application/json").send(text);
    } catch (e) {
      logger.error("Upstream OpenAI call failed", e);
      sendError(res, 502, "upstream_error", "Upstream request failed.");
    }
  },
);
