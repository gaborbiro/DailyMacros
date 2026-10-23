/**
 * Shared email validation/dedup logic for the landing page's two signup forms
 * (waitlist.js for the primary CTA, testers.js for the Android beta CTA).
 */

// Pragmatic: one @, no spaces, a dot in the domain. Real deliverability is
// proven when the address is actually used (added to Play Console, or
// emailed at launch).
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

module.exports = { normalizeEmail, dedupeKey };
