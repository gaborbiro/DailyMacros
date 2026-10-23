/**
 * Secrets shared by more than one function file. Defined once here so every
 * file references the same SecretParam instances instead of redeclaring the
 * same secret names.
 *
 * Set with: firebase functions:secrets:set OPENAI_KEY
 *           firebase functions:secrets:set NOTIFY_EMAIL
 *           firebase functions:secrets:set NOTIFY_APP_PASSWORD
 */
const { defineSecret } = require("firebase-functions/params");

// The app's proxy (index.js) and the landing-page demo (demo.js).
const OPENAI_KEY = defineSecret("OPENAI_KEY");

// The landing page's signup functions (testers.js, waitlist.js).
const NOTIFY_EMAIL = defineSecret("NOTIFY_EMAIL");
const NOTIFY_APP_PASSWORD = defineSecret("NOTIFY_APP_PASSWORD");

module.exports = { OPENAI_KEY, NOTIFY_EMAIL, NOTIFY_APP_PASSWORD };
