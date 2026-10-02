/**
 * Landing-page funnel counters: page views and Android-beta CTA clicks.
 * Waitlist signups and completed beta signups aren't counted here - they're
 * read directly off the `waitlist` and `testers` collections (see
 * lib/signupHandler.js), so all four funnel numbers together are:
 *   1. stats/counters.page_view
 *   2. count of waitlist/*
 *   3. stats/counters.beta_cta_click
 *   4. count of testers/*
 *
 * POST /api/event { type } increments stats/counters.<type>. No PII is
 * stored - the event type is the only input, and it's checked against a
 * whitelist.
 */
const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { FieldValue } = require("firebase-admin/firestore");

const db = admin.firestore();

const EVENT_TYPES = new Set(["page_view", "beta_cta_click"]);

exports.trackEvent = onRequest(
  {
    region: "us-central1", // must match the Hosting rewrite in firebase.json.
    maxInstances: 3,
    memory: "256MiB",
    timeoutSeconds: 10,
    cors: false, // same-origin via Hosting; no cross-origin caller needed.
  },
  async (req, res) => {
    res.set("Cache-Control", "no-store");
    if (req.method !== "POST") {
      res.status(405).json({ error: "method_not_allowed" });
      return;
    }

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const type = body.type;
    if (!EVENT_TYPES.has(type)) {
      res.status(400).json({ error: "invalid_type" });
      return;
    }

    try {
      await db.doc("stats/counters").set({ [type]: FieldValue.increment(1) }, { merge: true });
      res.status(200).json({ ok: true });
    } catch (e) {
      logger.error("trackEvent failed", e);
      res.status(500).json({ error: "server_error" });
    }
  },
);
