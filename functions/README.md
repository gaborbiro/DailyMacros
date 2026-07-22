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

   (If you skip this, the function falls back to 15 / 3000 / off / no
   unlimited clients.)

---

## Deploy

```bash
cd functions && npm install && cd ..
firebase deploy --only functions,firestore:rules
```

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
  request; no redeploy.
- **Emergency stop:** set `config/limits.killSwitch = true`. All proxied
  requests immediately return 503 until you flip it back.
- **See usage:** `usage/global` holds the current month's count;
  `usage_users/{uid}` holds each device's daily count, plus `clientId` (the
  three-word id shown in the app's Settings) and `lastSeen`.
- **Find a user from a support email:** they quote their three-word id (e.g.
  `apple-fox-moon`). Console → Firestore → `usage_users` → query where
  `clientId == apple-fox-moon`. The document id is their Firebase auth uid; the
  fields show today's count and last-seen time.
- **Unlock yourself permanently:** add your own three-word id to
  `config/limits.unlimitedClientIds`. Those clients skip the per-user daily cap
  (they're still counted and still bounded by the global monthly budget).
- **Give a user more room today:** open their `usage_users/{uid}` doc (found via
  `clientId` above) and edit `count`. Set it to `0` to restore their full daily
  allowance, or to a negative number (e.g. `-10`) to grant that many extra
  requests on top of the cap. It resets to normal at the next UTC day; takes
  effect on their next request, no redeploy. (Only meaningful when `utcDay` is
  today — if `utcDay` is stale they already have a fresh allowance.)
- **Watch cost:** the Blaze budget alert (step 2) plus your OpenAI account's
  own hard usage limit are the outer backstops behind the in-function cap.
