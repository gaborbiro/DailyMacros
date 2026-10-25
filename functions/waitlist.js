/**
 * Launch waitlist signup for the public landing page (web/), reached via the
 * primary "Get notified when Daily Macros launches" CTA. Unlike testers.js
 * (the Android beta), this only asks for an email - no Google Play
 * requirement, no 14-day opt-in.
 *
 * The page POSTs { email, website } to /api/waitlist (a Firebase Hosting
 * rewrite to this function). Docs land in `waitlist/{sha256(dedupeKey)}`;
 * firestore.rules already denies all client access, so the Admin SDK write in
 * lib/signupHandler.js is the only way in.
 */

const { createSignupHandler } = require("./lib/signupHandler");

exports.joinWaitlist = createSignupHandler({
  collection: "waitlist",
  notification: ({ email }) => ({
    senderName: "Daily Macros",
    subject: `New Daily Macros waitlist signup: ${email}`,
    countPendingIn: "waitlist",
    text: (pending) => `${email} just signed up (waitlist signup).\n\n${pending} pending in the waitlist collection.`,
  }),
});
