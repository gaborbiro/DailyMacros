# DailyMacros OpenAI proxy

A single Firebase Cloud Function (`openaiProxy`) that sits between the app and
OpenAI. It exists to do two things the app can't do safely on its own:

1. **Hide the OpenAI key.** The key lives only as a deployed secret, never in
   the APK, so it can't be extracted by decompiling the app.
2. **Cap spending.** Every request is metered in Firestore against a per-user
   daily cap and a global monthly budget, with a manual kill switch. A bug, an
   abuser, or organic growth can never push the bill past a known ceiling.

Metering is by **request count** (a tripwire), not exact token cost:
~3000 requests/month ≈ $30 at ~$0.01/call. Tune it live in Firestore — no
redeploy needed.

---

## One-time setup (Firebase console + CLI)

These steps need your Firebase login and can only be done by you.

1. **Install & log in to the Firebase CLI**
   ```bash
   npm install -g firebase-tools
   firebase login
   ```

2. **Upgrade the project to the Blaze plan.** Cloud Functions cannot make
   outbound calls to OpenAI on the free Spark plan. Blaze is pay-as-you-go with
   a generous free tier. In the console: *Project Settings → Usage and billing
   → Modify plan → Blaze*. Then set a **budget alert** (e.g. email at $10 /
   $25 / $40) so billing itself has a backstop.

3. **Enable Anonymous Authentication.** Console → *Authentication → Sign-in
   method → Anonymous → Enable*. This gives each device a stable `uid` for the
   per-user cap without any login screen.

4. **Create the Firestore database.** Console → *Firestore Database → Create
   database → Native mode → (pick the us-central region to match the
   function)*.

5. **Store the OpenAI key as a secret**
   ```bash
   firebase functions:secrets:set OPENAI_KEY
   # paste your OpenAI key when prompted
   ```

6. **Seed the limits document** so you can tune caps without redeploying.
   Console → Firestore → create collection `config`, document `limits`:
   | Field | Type | Value |
   |---|---|---|
   | `perUserDailyCap` | number | `15` |
   | `monthlyRequestBudget` | number | `3000` |
   | `killSwitch` | boolean | `false` |
   | `unlimitedClientIds` | array (optional) | `[]` |
   | `preSubRecognitionTotalCap` | number (optional) | `6` |
   | `preSubRecognitionSuccessCap` | number (optional) | `3` |
   | `preSubAnalysisTotalCap` | number (optional) | `6` |
   | `preSubAnalysisSuccessCap` | number (optional) | `3` |

   (If you skip this, the function falls back to 15 / 3000 / off / no
   unlimited clients / 6+3 per pre-subscription feature.)

---

## Subscription verification setup (one-time, for `verifySubscription`)

`verifySubscription` independently confirms Play Billing purchases with
Google's own Play Developer API before `openaiProxy` will trust them — never
the client's own claim. Unlike `OPENAI_KEY`, no secret/key file is involved:
the function runs as its own dedicated GCP service account, and Application
Default Credentials pick that identity up automatically.

1. **Enable the Android Publisher API** on this GCP project: Cloud Console →
   APIs & Services → Library → "Google Play Android Developer API" → Enable.
2. **Create the dedicated service account** (no key export needed):
   ```bash
   gcloud iam service-accounts create play-developer-api \
     --project=dailymacros-9fab8 \
     --display-name="Play Developer API (verifySubscription)"
   ```
3. **Grant it Firestore write access** (Admin SDK writes still go through IAM
   even though `firestore.rules` denies client access):
   ```bash
   gcloud projects add-iam-policy-binding dailymacros-9fab8 \
     --member="serviceAccount:play-developer-api@dailymacros-9fab8.iam.gserviceaccount.com" \
     --role="roles/datastore.user"
   ```
4. **Play Console → Users and permissions → Invite new user**: paste
   `play-developer-api@dailymacros-9fab8.iam.gserviceaccount.com`, grant "View
   app information (read-only)" + "View financial data", scoped to the
   DailyMacros app only.
5. **Play Console**: internal testing track + add yourself as a license
   tester. Purchases only complete against a signed, un-suffixed
   `applicationId` build — the `.debug`/`.qa` build types can never complete
   a real purchase.

Enforcement is always on: every caller needs an active/grace/still-in-paid-period
subscription, except for the small pre-subscription allowance described in
`index.js`'s header comment (the `recognition`/`analysis` features only, each
gated by its own total-attempts and success caps) and the `unlimitedClientIds`
allowlist (use this for your own test devices — see "Operating it" below).

---

## Real-time developer notifications (RTDN) setup (one-time)

`verifySubscription` only runs when the client sees a purchase token it
hasn't verified before — which happens once, at initial purchase. Nothing
client-side ever re-checks a purchase it already thinks it verified, so
without RTDN a `users/{uid}` doc's subscription fields go stale the moment the user
cancels, lapses, enters a grace period, or renews — `openaiProxy` would keep
trusting that frozen snapshot indefinitely. RTDN closes that gap: Play
publishes a Pub/Sub message on every subscription lifecycle event, and
`onSubscriptionNotification` re-verifies with Play and refreshes the doc in
response.

These steps need your GCP/Play Console access; nobody else can do them.

1. **Create the Pub/Sub topic** (name must match `PLAY_RTDN_TOPIC` in
   `subscriptions.js`):
   ```bash
   gcloud pubsub topics create play-rtdn --project=dailymacros-9fab8
   ```
2. **Let Play publish to it.** Google publishes RTDN messages from a single
   fixed service account shared by every Play developer:
   ```bash
   gcloud pubsub topics add-iam-policy-binding play-rtdn \
     --project=dailymacros-9fab8 \
     --member="serviceAccount:google-play-developer-notifications@system.gserviceaccount.com" \
     --role="roles/pubsub.publisher"
   ```
3. **Play Console → your app → Monetize → Monetization setup → Real-time
   developer notifications**: paste the full topic name,
   `projects/dailymacros-9fab8/topics/play-rtdn`, save, then click **Send
   test notification** — a `testNotification` payload should show up in this
   function's logs once deployed (step 4).
4. **Deploy** (see below). `onSubscriptionNotification`'s Pub/Sub trigger
   provisions its own subscription on the `play-rtdn` topic automatically;
   there's nothing extra to wire up on the GCP side.

No Android app changes are needed — RTDN is entirely server-side.

### Refunds and chargebacks (not covered by RTDN)

Play has **no push notification** for a voided purchase (refund/chargeback)
— RTDN only ever carries `subscriptionNotification`, `oneTimeProductNotification`,
or `testNotification`. Catching refunds needs a separate pull:
`checkVoidedPurchases` polls the Voided Purchases API every 6 hours and
revokes `users/{uid}`'s subscription for any purchase token that got voided
(only if that's still the uid's *current* token — a stale void can't clobber
a newer, legitimate resubscription).

No manual GCP/Play Console setup is needed for this one beyond what's
already granted to `play-developer-api` above — `firebase deploy` creates
its own Cloud Scheduler job automatically. **Do** test it against a real
refunded test purchase (Play Console → Order management → refund a test
order) before trusting it — the Voided Purchases API's field/enum names in
the code are per Google's documented shape but haven't been exercised
against a live response here.

### Recovering a lost `users/{uid}` doc

If a subscriber's `users/{uid}` doc is ever lost (an accidental Firestore
delete, most likely) they'd look like a brand-new user to `openaiProxy`.
Two separate durable mappings (each its own collection, unaffected by
`users/{uid}` being deleted) exist specifically so this is recoverable:
`purchaseTokens/{token} -> {uid}` and `clientIds/{clientId} -> {uid}` — the
first recovers the uid → purchase token link, the second means even just a
support email quoting a three-word id is enough to find the uid at all.

- **Automatic**: `openaiProxy` self-heals the first time it sees a
  not-yet-entitled request from a uid whose doc doesn't exist at all — it
  checks `purchaseTokens` for a token on file, and if found, re-verifies with
  Play and retries the same request before denying it. No waiting required,
  but it only fires when that uid happens to make a request.
- **Manual**: `repairSubscription`, an on-demand HTTP endpoint, for fixing a
  specific user immediately (e.g. right after you notice you deleted the
  wrong doc, or from a support email) instead of waiting for them to call in.
  Gated by a shared secret rather than per-user auth — there's no admin-role
  concept in this app, and this is a developer tool, not an in-app feature.
  One-time setup — the secret needs setting in **two** places, same value in
  both (Firebase's Secret Manager, so the function can check incoming calls
  against it; a GitHub Actions repo secret, so the workflow below can supply
  it without you ever typing it in at call time):
  ```bash
  firebase functions:secrets:set ADMIN_REPAIR_KEY
  # paste a random secret string when prompted
  ```
  Then repo → Settings → Secrets and variables → Actions → New repository
  secret → name it `ADMIN_REPAIR_KEY`, same value as above.

  **Trigger it:** Actions tab → "Repair subscription" → Run workflow → fill
  in either `uid` or `client_id` → Run. Works from the GitHub mobile app just
  as well as a desktop — the whole point, since this needs no phone-side HTTP
  client or manually-copied secret. See
  `.github/workflows/repair-subscription.yml`.

  Or call the endpoint directly if you'd rather:
  ```bash
  curl -s -X POST \
    "https://us-central1-dailymacros-9fab8.cloudfunctions.net/repairSubscription" \
    -H "X-Admin-Key: <the secret you set above>" \
    -H "Content-Type: application/json" \
    -d '{"clientId": "apple-fox-moon"}'
  # or: -d '{"uid": "<their Firebase auth uid>"}'
  ```

  Either way, returns `404 not_found` if `clientIds`/`purchaseTokens` has
  nothing on file (never subscribed, or the mapping itself was also lost —
  this can't help with that case). `clientId` → uid isn't guaranteed unique
  at scale (see `repairSubscription`'s doc comment) — double-check with the
  user if a repair looks like it hit the wrong account.

---

## Deploy

```bash
cd functions && npm install && cd ..
firebase deploy --only functions,firestore:rules
```

### Deploy from GitHub (no local `firebase login`)

`.github/workflows/firebase-deploy.yml` runs this same deploy non-interactively,
authenticated as a service account instead of a personal Google login. It's
**manual only, by design** — no path-based auto-detection on merge, since a
functions deploy changes real enforcement behavior and that's a call worth
making deliberately each time, not inferring from which files a PR happened to
touch. Trigger it yourself: Actions tab → "Deploy Firebase functions" → Run
workflow → pick `functions`, `firestore:rules`, or both.

One-time setup (needs your GCP/GitHub access; nobody else can do this part):

1. **Create the service account:**
   ```bash
   gcloud iam service-accounts create firebase-deployer \
     --project=dailymacros-9fab8 \
     --display-name="Firebase deploy (GitHub Actions)"
   ```
2. **Grant it the roles `firebase deploy` needs** (functions + Firestore rules,
   2nd-gen Cloud Functions build via Cloud Build/Artifact Registry):
   ```bash
   SA="firebase-deployer@dailymacros-9fab8.iam.gserviceaccount.com"
   for ROLE in roles/firebase.admin roles/cloudfunctions.admin \
               roles/cloudbuild.builds.editor roles/artifactregistry.admin \
               roles/iam.serviceAccountUser roles/storage.admin; do
     gcloud projects add-iam-policy-binding dailymacros-9fab8 \
       --member="serviceAccount:$SA" --role="$ROLE"
   done
   ```
   If a deploy still fails on a missing permission, the error names the exact
   role to add — GCP IAM for 2nd-gen functions is finicky enough that this list
   may need a follow-up grant.
3. **Generate a key and add it as a GitHub secret** named
   `FIREBASE_SERVICE_ACCOUNT_JSON` (repo → Settings → Secrets and variables →
   Actions):
   ```bash
   gcloud iam service-accounts keys create /tmp/firebase-deployer-key.json \
     --iam-account="$SA"
   ```
   Paste the file's contents as the secret value, then delete the local key file.

That's it — once the secret exists, the workflow is ready to run from the Actions
tab whenever you want to deploy.

The deploy prints the function URL, e.g.
`https://us-central1-dailymacros-9fab8.cloudfunctions.net/openaiProxy`.
Note it — the Android client will point at it.

---

## Smoke tests

**Health / latency of the added hop** (no auth, returns instantly). Run it a
few times to see warm vs. cold timing. With `minInstances: 1` it should be
consistently fast:
```bash
curl -s -w "\n%{time_total}s\n" \
  "https://us-central1-dailymacros-9fab8.cloudfunctions.net/openaiProxy?health=1"
```

**Auth rejection** (should return 401):
```bash
curl -s -X POST \
  "https://us-central1-dailymacros-9fab8.cloudfunctions.net/openaiProxy" \
  -H "Content-Type: application/json" -d '{}'
```

Full end-to-end (real OpenAI call + real latency) is exercised once the Android
client is wired to send its Firebase ID token — that's the follow-up step.

---

## Operating it

- **Tune caps:** edit `config/limits` in Firestore. Takes effect on the next
  request; no redeploy. This includes the four `preSub*Cap` fields that
  control how much a not-yet-subscribed user can use `recognition` and
  `analysis` before being asked to subscribe (see `index.js`'s header
  comment) — a user's own progress toward those caps lives on their
  `users/{uid}` doc (`preSubRecognitionTotal`/`Success`,
  `preSubAnalysisTotal`/`Success`).
- **Unlock a test device from subscription enforcement entirely:** add its
  three-word id to `config/limits.unlimitedClientIds` (see below) — cleaner
  than watching it burn through the pre-subscription allowance.
- **Emergency stop:** set `config/limits.killSwitch = true`. All proxied
  requests immediately return 503 until you flip it back.
- **See usage:** `usage/global` holds the current month's count;
  `users/{uid}` holds each device's daily count (`dailyCapCount`, valid for
  the UTC date in `dailyCapUtcDate`), plus `clientId` (the
  three-word id shown in the app's Settings), `lastSeen`, and that same
  user's subscription fields (`subscriptionState`, `subscriptionProductId`,
  `subscriptionExpiryTimeMillis`, `subscriptionPurchaseToken`,
  `subscriptionUpdatedAt`, and `subscriptionVoidedAt` if ever revoked) — one
  doc holds the full picture for a given uid, usage and subscription alike.
  `subscriptionState` is Google's own `subscriptionState` value verbatim
  (lowercased, prefix stripped) — `active`, `pending`, `paused`,
  `in_grace_period`, `on_hold`, `canceled`, `expired`, `unspecified` — plus
  the synthetic `revoked` written only by `checkVoidedPurchases`.
- **Find a user from a support email:** they quote their three-word id (e.g.
  `apple-fox-moon`). Console → Firestore → `clientIds/apple-fox-moon` — a
  direct doc lookup, no query needed, and it works even if `users/{uid}` was
  lost (that's the reason this mapping exists as its own collection instead
  of only living on the profile doc). Its one field, `uid`, is the Firebase
  auth uid; open `users/{uid}` from there for today's count, last-seen time,
  and subscription state. (Not guaranteed unique at scale — see
  `repairSubscription`'s doc comment in `subscriptions.js`.)
- **Unlock yourself permanently:** add your own three-word id to
  `config/limits.unlimitedClientIds`. Those clients skip the per-user daily cap
  (they're still counted and still bounded by the global monthly budget).
- **Give a user more room today:** open their `users/{uid}` doc (found via
  `clientId` above) and edit `dailyCapCount`. Set it to `0` to restore their
  full daily allowance, or to a negative number (e.g. `-10`) to grant that
  many extra requests on top of the cap. It resets to normal at the next UTC
  day; takes effect on their next request, no redeploy. (Only meaningful when
  `dailyCapUtcDate` is today — if it's stale they already have a fresh
  allowance.)
- **Watch cost:** the Blaze budget alert (step 2) plus your OpenAI account's
  own hard usage limit are the outer backstops behind the in-function cap.
