const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

const db = admin.firestore();

/**
 * Best-effort "someone signed up" email, shared by the waitlist and Android
 * beta signup functions (one Gmail account emails itself). Never throws: a
 * mail problem must not fail (or slow down) the signup itself. `transport` is
 * injectable for tests.
 */
async function notifySignup({ collection, label, email, note, to, pass, transport }) {
  try {
    if (!transport && (!to || !pass)) return;
    const pending = (await db.collection(collection).where("status", "==", "pending").count().get()).data().count;
    const mailer = transport || nodemailer.createTransport({
      host: "smtp.gmail.com", port: 465, secure: true,
      auth: { user: to, pass },
      connectionTimeout: 8000, socketTimeout: 8000,
    });
    await mailer.sendMail({
      from: `Daily Macros <${to}>`,
      to,
      subject: `New Daily Macros ${label}: ${email}`,
      text: `${email} just signed up (${label}).\n\n${pending} pending in the ${collection} collection.` +
        (note ? ` ${note}` : ""),
    });
  } catch (e) {
    logger.warn("Signup notification failed", e);
  }
}

module.exports = { notifySignup };
