/**
 * Secrets shared by the landing page's signup functions (testers.js,
 * waitlist.js). Defined once here so both files reference the same
 * SecretParam instances instead of redeclaring the same secret names.
 *
 * Set with: firebase functions:secrets:set NOTIFY_EMAIL
 *           firebase functions:secrets:set NOTIFY_APP_PASSWORD
 */
const { defineSecret } = require("firebase-functions/params");

const NOTIFY_EMAIL = defineSecret("NOTIFY_EMAIL");
const NOTIFY_APP_PASSWORD = defineSecret("NOTIFY_APP_PASSWORD");

module.exports = { NOTIFY_EMAIL, NOTIFY_APP_PASSWORD };
