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

    // 2. Enforce caps atomically.
    const now = new Date();
    const monthKey = now.toISOString().slice(0, 7); // YYYY-MM (UTC)
    const dayKey = now.toISOString().slice(0, 10); // YYYY-MM-DD (UTC)

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
        if (cfg.killSwitch === true) {
          return { allow: false, status: 503, code: "kill_switch", message: "Service temporarily unavailable." };
        }

        const g = globalSnap.data();
        const globalCount = g && g.month === monthKey ? g.count || 0 : 0;
        if (globalCount >= monthlyBudget) {
          return { allow: false, status: 503, code: "monthly_budget", message: "Service is at capacity for this month. Please try again later." };
        }

        const u = userSnap.data();
        const userCount = u && u.day === dayKey ? u.count || 0 : 0;
        if (userCount >= perUserDailyCap) {
          return { allow: false, status: 429, code: "daily_cap", message: "You've reached today's analysis limit. Please try again tomorrow." };
        }

        tx.set(globalRef, { month: monthKey, count: globalCount + 1 }, { merge: true });
        tx.set(userRef, { day: dayKey, count: userCount + 1 }, { merge: true });
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
