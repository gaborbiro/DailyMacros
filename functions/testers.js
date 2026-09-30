/**
 * Closed-test signup for the public landing page (web/).
 *
 * The page POSTs { email, website } to /api/join (a Firebase Hosting rewrite to
 * this function, so the browser never talks to Firestore or needs Firebase
 * config). Docs land in `testers/{sha256(dedupeKey)}`; firestore.rules already
 * denies all client access, so this Admin SDK write is the only way in.
 *
 * Abuse protection is deliberately light (this is a temporary page whose worst
 * case is junk rows in a table you review by hand): a honeypot field, strict
 * email validation, a tiny body limit, and maxInstances to bound cost. No App
 * Check / reCAPTCHA - see the notes in the PR/hand-off for the trade-off.
 */

const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { FieldValue } = require("firebase-admin/firestore");
const crypto = require("crypto");

const db = admin.firestore();

// Pragmatic: one @, no spaces, a dot in the domain. Real deliverability is
// proven when you add the address to the Play Console list.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const MAX_EMAIL_LENGTH = 254;

/** Trim + lowercase, and fold googlemail.com into gmail.com. Null if invalid. */
function normalizeEmail(raw) {
  if (typeof raw !== "string") return null;
  const email = raw.trim().toLowerCase();
  if (email.length > MAX_EMAIL_LENGTH || !EMAIL_RE.test(email)) return null;
  const at = email.lastIndexOf("@");
  const local = email.slice(0, at);
  const domain = email.slice(at + 1) === "googlemail.com" ? "gmail.com" : email.slice(at + 1);
  return `${local}@${domain}`;
}

/**
 * Key used only for de-duplication. Gmail ignores dots in the local part, so
 * jane.doe@ and janedoe@ are the same Google account and must not count twice.
 */
function dedupeKey(email) {
  const [local, domain] = email.split("@");
  return domain === "gmail.com" ? `${local.replace(/\./g, "")}@${domain}` : email;
}

exports.joinTest = onRequest(
  {
    region: "us-central1", // must match the Hosting rewrite in firebase.json.
    maxInstances: 3,
    memory: "256MiB",
    timeoutSeconds: 15,
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
      await db.doc(`testers/${id}`).create({
        email,
        status: "pending", // you flip this to "added" once the address is in Play Console.
        source: "landing-page",
        createdAt: FieldValue.serverTimestamp(),
      });
      res.status(200).json({ ok: true });
    } catch (e) {
      // ALREADY_EXISTS (gRPC code 6): same person submitting twice. Treat as
      // success and don't reveal anything about the existing record.
      if (e && e.code === 6) {
        res.status(200).json({ ok: true, alreadyRegistered: true });
        return;
      }
      logger.error("Tester signup failed", e);
      res.status(500).json({ error: "server_error", message: "Something went wrong. Please try again in a moment." });
    }
  },
);

exports._test = { normalizeEmail, dedupeKey };
