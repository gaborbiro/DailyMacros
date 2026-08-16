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
 * Subscription gate: a subscription (subscriptions/{uid}, written by
 * verifySubscription in subscriptions.js) is ALWAYS required, for everyone
 * not on the unlimitedClientIds allowlist, with one exception: a brand-new,
 * not-yet-subscribed user gets a small pre-subscription allowance for the
 * "recognition" and "analysis" features (see PRESUB_FEATURES below) so they
 * can experience real value before being asked to pay. Every other feature
 * (weekly/ongoing insights, or a request with no recognised X-Feature header)
 * requires a subscription unconditionally.
 *
 * Pre-subscription allowance: each gated feature has two independent,
 * lifetime (never-reset) per-user caps in usage_users/{uid}:
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
 * Once either cap is hit for a feature, that feature requires a real
 * subscription. Tune the caps live in Firestore at config/limits without
 * redeploying (see functions/README.md).
 *
 * Metering is by REQUEST COUNT (a tripwire), not exact token cost: ~3000
 * requests/month ~= $30 at ~$0.01/call. Tune the numbers live in Firestore at
 * config/limits without redeploying.
 *
 * Overrides (all live in Firestore, no redeploy):
 *   - config/limits.unlimitedClientIds: [ "apple-fox-moon", ... ] — clients on
 *     this list bypass the per-user DAILY cap and the subscription gate
 *     (including the pre-subscription feature caps) entirely, but are still
 *     counted and still subject to the global monthly budget. Use it to
 *     permanently unlock yourself: put your own three-word id (from the app's
 *     Settings screen) here once.
 *   - To give one user more room today, edit their usage_users/{uid}.count:
 *     0 restores their full daily allowance, a negative value (e.g. -10) grants
 *     that many extra requests on top of the cap. It resets to normal at the
 *     next UTC day.
 *
 * Every request records the caller's three-word client id (X-Client-Id header)
 * and last-seen time on usage_users/{uid}, so a support email that quotes the
 * id maps to a row: query usage_users where clientId == "apple-fox-moon".
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

exports.verifySubscription = require("./subscriptions").verifySubscription;

// Set with: firebase functions:secrets:set OPENAI_KEY
const OPENAI_KEY = defineSecret("OPENAI_KEY");

const OPENAI_URL = "https://api.openai.com/v1/responses";

// Fallbacks used only if config/limits is missing a field.
const DEFAULT_PER_USER_DAILY_CAP = 15;
const DEFAULT_MONTHLY_REQUEST_BUDGET = 3000;

/**
 * The only two features a not-yet-subscribed user may use at all, and how to
 * look up/store their allowance. Anything else (weekly insights, ongoing
 * insights, a request with no/unrecognised X-Feature header) has no
 * pre-subscription allowance - see the gate below.
 */
const PRESUB_FEATURES = {
  recognition: {
    totalField: "preSubRecognitionTotal",
    successField: "preSubRecognitionSuccess",
    totalCapField: "preSubRecognitionTotalCap",
    successCapField: "preSubRecognitionSuccessCap",
    defaultTotalCap: 6,
    defaultSuccessCap: 3,
  },
  analysis: {
    totalField: "preSubAnalysisTotal",
    successField: "preSubAnalysisSuccess",
    totalCapField: "preSubAnalysisTotalCap",
    successCapField: "preSubAnalysisSuccessCap",
    defaultTotalCap: 6,
    defaultSuccessCap: 3,
  },
};

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
    // useful" signal from the client (see PRESUB_FEATURES doc above) - not an
    // OpenAI call, just a Firestore increment. Never trusted for the TOTAL
    // cap, only for the softer SUCCESS cap.
    if (req.query.report !== undefined) {
      const feature = req.body && req.body.feature;
      if (feature !== "recognition" && feature !== "analysis") {
        sendError(res, 400, "invalid_request", "feature must be 'recognition' or 'analysis'.");
        return;
      }
      try {
        await db.doc(`usage_users/${uid}`).set(
          { [PRESUB_FEATURES[feature].successField]: admin.firestore.FieldValue.increment(1) },
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

    // Which AI feature this call is for (see PRESUB_FEATURES); null/unrecognised
    // means "not eligible for a pre-subscription allowance".
    const featureKey = req.get("X-Feature") || null;

    // 2. Enforce caps atomically.
    const now = new Date();
    const monthKey = now.toISOString().slice(0, 7); // YYYY-MM (UTC)
    const utcDayKey = now.toISOString().slice(0, 10); // YYYY-MM-DD (UTC)

    const configRef = db.doc("config/limits");
    const globalRef = db.doc("usage/global");
    const userRef = db.doc(`usage_users/${uid}`);
    const subRef = db.doc(`subscriptions/${uid}`);

    let decision;
    try {
      decision = await db.runTransaction(async (tx) => {
        const [configSnap, globalSnap, userSnap, subSnap] = await Promise.all([
          tx.get(configRef),
          tx.get(globalRef),
          tx.get(userRef),
          tx.get(subRef),
        ]);

        const cfg = configSnap.data() || {};
        const perUserDailyCap = cfg.perUserDailyCap ?? DEFAULT_PER_USER_DAILY_CAP;
        const monthlyBudget = cfg.monthlyRequestBudget ?? DEFAULT_MONTHLY_REQUEST_BUDGET;
        const unlimitedClientIds = Array.isArray(cfg.unlimitedClientIds) ? cfg.unlimitedClientIds : [];
        if (cfg.killSwitch === true) {
          return { allow: false, status: 503, code: "kill_switch", message: "Service temporarily unavailable." };
        }

        const g = globalSnap.data();
        const globalCount = g && g.month === monthKey ? g.count || 0 : 0;
        if (globalCount >= monthlyBudget) {
          return { allow: false, status: 503, code: "monthly_budget", message: "Service is at capacity for this month. Please try again later." };
        }

        const u = userSnap.data() || {};

        // Allowlisted clients skip both the daily cap (checked below) and the
        // subscription gate entirely (developer testing, reviewers) - still
        // counted, still bounded by the global monthly budget checked above.
        const isUnlimited = clientId != null && unlimitedClientIds.includes(clientId);
        let preSubIncrementField = null;
        if (!isUnlimited) {
          const sub = subSnap.data();
          const nowMillis = Date.now();
          const entitled = !!sub && (
            sub.state === "active" ||
            sub.state === "grace" ||
            (sub.state === "canceled" && sub.expiryTimeMillis > nowMillis)
          );
          if (!entitled) {
            const feature = PRESUB_FEATURES[featureKey];
            if (!feature) {
              return { allow: false, status: 403, code: "subscription_required", message: "A subscription is required to continue using AI analysis." };
            }
            const totalCap = cfg[feature.totalCapField] ?? feature.defaultTotalCap;
            const successCap = cfg[feature.successCapField] ?? feature.defaultSuccessCap;
            const totalCount = u[feature.totalField] || 0;
            const successCount = u[feature.successField] || 0;
            if (totalCount >= totalCap || successCount >= successCap) {
              return { allow: false, status: 403, code: "subscription_required", message: "A subscription is required to continue using AI analysis." };
            }
            preSubIncrementField = feature.totalField;
          }
        }

        // Allowlisted clients skip the per-user daily cap (still counted below,
        // still bounded by the global monthly budget checked above).
        const userCount = u.utcDay === utcDayKey ? u.count || 0 : 0;
        if (!isUnlimited && userCount >= perUserDailyCap) {
          return { allow: false, status: 429, code: "daily_cap", message: "You've reached today's analysis limit. Please try again tomorrow." };
        }

        tx.set(globalRef, { month: monthKey, count: globalCount + 1 }, { merge: true });
        const userUpdate = { utcDay: utcDayKey, count: userCount + 1, lastSeen: now.toISOString() };
        if (clientId != null) userUpdate.clientId = clientId;
        if (preSubIncrementField != null) userUpdate[preSubIncrementField] = (u[preSubIncrementField] || 0) + 1;
        tx.set(userRef, userUpdate, { merge: true });
        return { allow: true };
      });
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
