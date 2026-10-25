/**
 * Closed-test signup for the public landing page (web/).
 *
 * The page POSTs { email, website, platform } to /api/join (a Firebase Hosting
 * rewrite to this function, so the browser never talks to Firestore or needs
 * Firebase config). Docs land in `testers/{sha256(dedupeKey)}` (iOS:
 * `sha256("ios:" + dedupeKey)`); firestore.rules already denies all client
 * access, so the Admin SDK write in lib/signupHandler.js is the only way in.
 */

const { createSignupHandler } = require("./lib/signupHandler");

// A missing platform means Android: the original page (and any cached copy of
// it) never sent one. Existing Android docs also have no `platform` field.
const PLATFORMS = new Set(["android", "ios"]);

function platformList(body) {
  const platform = body.platform === undefined ? "android" : body.platform;
  if (!PLATFORMS.has(platform)) {
    return { invalid: { error: "invalid_platform", message: "Please choose Android or iPhone." } };
  }
  const ios = platform === "ios";
  return {
    // Android ids stay exactly as before (existing docs keep de-duplicating);
    // iOS ids are namespaced so one email can be on both lists.
    keyPrefix: ios ? "ios:" : "",
    fields: {
      platform,
      // Android: you flip "pending" to "added" once the address is in Play
      // Console. iOS has nothing to approve, so it never counts as pending.
      status: ios ? "waitlist" : "pending",
    },
  };
}

function notification({ email, fields: { platform } }) {
  if (platform === "ios") {
    return {
      senderName: "Daily Macros tests",
      subject: `New Daily Macros iPhone waitlist signup: ${email}`,
      text: () => `${email} joined the iPhone waitlist (no action needed; see testers with platform "ios" in Firestore).`,
    };
  }
  return {
    senderName: "Daily Macros tests",
    subject: `New Daily Macros tester: ${email}`,
    countPendingIn: "testers",
    text: (pending) => `${email} just signed up for the closed test.\n\n${pending} pending in total. ` +
      `Add them in Play Console, then set status to "added" in Firestore (testers collection).`,
  };
}

exports.joinTest = createSignupHandler({
  collection: "testers",
  variant: platformList,
  notification,
});
