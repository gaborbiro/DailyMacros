/**
 * DailyMacros OpenAI proxy (Firebase Cloud Function, 2nd gen).
 *
 * Purpose: keep the OpenAI key server-side and enforce spending caps so a bug,
 * an abuser, or organic growth can never run the bill past a known ceiling.
 *
 * Flow for a normal request:
 *   1. Verify the caller's Firebase ID token (anonymous auth is fine) -> uid.
 *   2. In one Firestore transaction, check the kill switch, the global monthly
 *      request budget, and the per-user daily cap. Increment both counters.
 *   3. Forward the JSON body verbatim to OpenAI's /v1/responses with the real
 *      key and return OpenAI's status + body unchanged, so the Android client's
 *      existing response/error parsing keeps working.
 *
 * Metering is by REQUEST COUNT (a tripwire), not exact token cost: ~3000
 * requests/month ~= $30 at ~$0.01/call. Tune the numbers live in Firestore at
 * config/limits without redeploying.
 *
 * Overrides (all live in Firestore, no redeploy):
 *   - config/limits.unlimitedClientIds: [ "apple-fox-moon", ... ] — clients on
 *     this list bypass the per-user DAILY cap entirely (still counted, still
 *     subject to the global monthly budget). Use it to permanently unlock
 *     yourself: put your own three-word id (from the app's Settings screen)
 *     here once.
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

// Set with: firebase functions:secrets:set OPENAI_KEY
const OPENAI_KEY = defineSecret("OPENAI_KEY");

const OPENAI_URL = "https://api.openai.com/v1/responses";

// Fallbacks used only if config/limits is missing a field.
const DEFAULT_PER_USER_DAILY_CAP = 15;
const DEFAULT_MONTHLY_REQUEST_BUDGET = 3000;

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

    // Three-word client id the user sees in Settings. Stored on the usage doc so
    // a support email maps to a row. Clamped so a bad/spoofed header can't bloat
    // the document; null if absent (older app builds).
    const clientId = (req.get("X-Client-Id") || "").slice(0, 64) || null;

    // 2. Enforce caps atomically.
    const now = new Date();
    const monthKey = now.toISOString().slice(0, 7); // YYYY-MM (UTC)
    const utcDayKey = now.toISOString().slice(0, 10); // YYYY-MM-DD (UTC)

    const configRef = db.doc("config/limits");
    const globalRef = db.doc("usage/global");
    const userRef = db.doc(`usage_users/${uid}`);

    let decision;
    try {
      decision = await db.runTransaction(async (tx) => {
        const [configSnap, globalSnap, userSnap] = await Promise.all([
          tx.get(configRef),
          tx.get(globalRef),
          tx.get(userRef),
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
        const userCount = u.utcDay === utcDayKey ? u.count || 0 : 0;
        // Allowlisted clients skip the per-user daily cap (still counted below,
        // still bounded by the global monthly budget checked above).
        const isUnlimited = clientId != null && unlimitedClientIds.includes(clientId);
        if (!isUnlimited && userCount >= perUserDailyCap) {
          return { allow: false, status: 429, code: "daily_cap", message: "You've reached today's analysis limit. Please try again tomorrow." };
        }

        tx.set(globalRef, { month: monthKey, count: globalCount + 1 }, { merge: true });
        const userUpdate = { utcDay: utcDayKey, count: userCount + 1, lastSeen: now.toISOString() };
        if (clientId != null) userUpdate.clientId = clientId;
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
