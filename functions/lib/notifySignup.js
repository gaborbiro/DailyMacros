const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const { NOTIFY_EMAIL, NOTIFY_APP_PASSWORD } = require("./secrets");

const db = admin.firestore();

/**
 * Best-effort "someone signed up" email, shared by the waitlist and Android
 * beta signup functions (one Gmail account emails itself). Never throws: a
 * mail problem must not fail (or slow down) the signup itself. `transport` is
 * injectable for tests.
 *
 * The caller owns the wording: `text(pending)` gets the number of
 * `status == "pending"` docs in `countPendingIn`, or null when
 * `countPendingIn` is omitted (nothing to count, so no query is made).
 */
async function notifySignup({ senderName, subject, countPendingIn, text, transport }) {
  try {
    const to = NOTIFY_EMAIL.value();
    const pass = NOTIFY_APP_PASSWORD.value();
    if (!transport && (!to || !pass)) return;
    const pending = countPendingIn
      ? (await db.collection(countPendingIn).where("status", "==", "pending").count().get()).data().count
      : null;
    const mailer = transport || nodemailer.createTransport({
      host: "smtp.gmail.com", port: 465, secure: true,
      auth: { user: to, pass },
      connectionTimeout: 8000, socketTimeout: 8000,
    });
    await mailer.sendMail({
      from: `${senderName} <${to}>`,
      to,
      subject,
      text: text(pending),
    });
  } catch (e) {
    logger.warn("Signup notification failed", e);
  }
}

module.exports = { notifySignup };
