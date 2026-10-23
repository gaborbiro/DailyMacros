/**
 * Builds an onRequest handler for a simple "collect an email into a Firestore
 * collection" signup form. Shared by the landing page's waitlist (primary
 * CTA, waitlist.js) and Android beta (secondary CTA, testers.js) forms - both
 * are the same shape: a honeypot field, strict email validation, dedupe by
 * hash, and a best-effort notification email.
 *
 * Abuse protection is deliberately light (these are pages whose worst case is
 * junk rows in a table you review by hand): a honeypot field, strict email
 * validation, a tiny body shape, and maxInstances to bound cost. No App Check
 * / reCAPTCHA.
 */
const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { FieldValue } = require("firebase-admin/firestore");
const crypto = require("crypto");
const { normalizeEmail, dedupeKey } = require("./signupEmail");
const { notifySignup } = require("./notifySignup");

const db = admin.firestore();

function createSignupHandler({ collection, notifyLabel, notifyNote, notifySecrets }) {
  const [NOTIFY_EMAIL, NOTIFY_APP_PASSWORD] = notifySecrets;

  return onRequest(
    {
      region: "us-central1", // must match the Hosting rewrite in firebase.json.
      maxInstances: 3,
      memory: "256MiB",
      secrets: notifySecrets,
      timeoutSeconds: 30,
      cors: false, // same-origin via Hosting; no cross-origin caller needed.
    },
    async (req, res) => {
      res.set("Cache-Control", "no-store");
      if (req.method !== "POST") {
        res.status(405).json({ error: "method_not_allowed" });
        return;
      }

      const body = req.body && typeof req.body === "object" ? req.body : {};

      // Honeypot: real users never see or fill this field. Pretend success so
      // bots get no signal to adapt to.
      if (typeof body.website === "string" && body.website !== "") {
        res.status(200).json({ ok: true });
        return;
      }

      const email = normalizeEmail(body.email);
      if (!email) {
        res.status(400).json({ error: "invalid_email", message: "That doesn't look like a valid email address." });
        return;
      }

      const id = crypto.createHash("sha256").update(dedupeKey(email)).digest("hex");
      try {
        await db.doc(`${collection}/${id}`).create({
          email,
          status: "pending", // flipped to e.g. "added"/"notified" once acted on.
          source: "landing-page",
          createdAt: FieldValue.serverTimestamp(),
        });
        await notifySignup({
          collection,
          label: notifyLabel,
          email,
          note: notifyNote,
          to: NOTIFY_EMAIL.value(),
          pass: NOTIFY_APP_PASSWORD.value(),
        });
        res.status(200).json({ ok: true });
      } catch (e) {
        // ALREADY_EXISTS (gRPC code 6): same person submitting twice. Treat as
        // success and don't reveal anything about the existing record.
        if (e && e.code === 6) {
          res.status(200).json({ ok: true, alreadyRegistered: true });
          return;
        }
        logger.error(`${collection} signup failed`, e);
        res.status(500).json({ error: "server_error", message: "Something went wrong. Please try again in a moment." });
      }
    },
  );
}

module.exports = { createSignupHandler };
