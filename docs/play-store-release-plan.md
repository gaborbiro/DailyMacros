# Play Store Release Plan

## Already done
- Prompt cleanup: split recognition/analysis prompts, Customise AI editor UI, "underestimate" instruction
- Google Drive backup (export/import via `appDataFolder`)

---

## Remaining (priority order)

### 1. Privacy policy page *(required, ~1 day)*
- Draft written: `PRIVACY.md` at repo root (covers on-device storage, the OpenAI
  data flow + proxy, optional Drive backup, anonymous telemetry, identifiers,
  permissions, retention). Still TODO: fill in the `<your-contact-email>`
  placeholder.
- Host it. The in-app links currently point at
  `https://github.com/gaborbiro/DailyMacros/blob/master/PRIVACY.md`
  (string resources `settings_privacy_policy_url` / `welcome_privacy_policy_url`).
  If hosting on GitHub Pages or elsewhere, update those two strings to match.
- In-app links: done (Settings "Privacy & data" section + Welcome-screen notice).
- Remaining: add the final URL to the Play Console listing.

### 2. Play Store listing materials *(required, ~1 day)*
- Short description (80 chars), long description (~4000 chars)
- Screenshots: 2–8 phone screenshots (Pixel-sized, 9:16)
- Feature graphic: 1024×500px banner

### 3. Data safety form *(required, ~2 hrs)*
- Manual form in Play Console — no code
- Key answers (kept in sync with actual app behaviour — see `PRIVACY.md`):
  - **Shared with a third party (OpenAI):** the photos and meal descriptions the
    user enters, and — for weekly insights — their recent diary and targets, are
    sent to OpenAI to provide meal recognition / nutrition analysis. Not stored
    by the developer.
  - **Collected by the developer (limited):** anonymous usage analytics and crash
    diagnostics via Firebase, plus a random per-device identifier and usage
    counters stored by the proxy in Firestore to enforce usage limits. None of
    this is linked to a user's identity, and no meal photos/diary content is
    collected. (The earlier "no data collected by the developer" answer was
    inaccurate.)
  - **Stored on-device:** the user's diary (photos, descriptions, nutrition) and
    settings live on the device; optional backup goes to the user's *own* Google
    Drive.
  - **Encryption in transit:** yes — all requests use HTTPS/TLS.
  - **Encryption at rest:** yes. Android encrypts app-private storage at rest by
    default (File-Based Encryption tied to the device lock screen), so the diary
    database and settings are encrypted on the flash storage. The BYO OpenAI key
    additionally gets app-level encryption (Android Keystore, AES-GCM). (The
    earlier "no encryption at rest (no storage)" answer was inaccurate — the
    diary *is* stored on-device, and it *is* encrypted at rest by the OS.)

### 4. Subscription via Play Billing + free trial *(revenue, ~3–5 days)*
- Define subscription product + free trial period in Play Console (trial = no code)
- Add `com.android.billingclient:billing-ktx` dependency
- `BillingClient` wrapper: connect, query purchases, `acknowledgePurchase`
- Gate AI queries behind active subscription check; show paywall when unsubscribed
- Hilt-inject the wrapper; tie into `FoodRecognitionUseCase` and `NutrientAnalysisUseCase`

---

## Release checklist

- [ ] Privacy policy URL live and linked in Play Console
- [ ] Data safety form completed in Play Console
- [ ] Play Store listing text, screenshots, feature graphic uploaded
- [ ] Subscription product created in Play Console with free trial configured
- [ ] Play Billing integrated and gating AI queries
- [ ] Bump `appVersionName` + `appVersionCode` in `app/build.gradle.kts`
- [ ] Build signed AAB (Android Studio → Build → Generate Signed App Bundle → production key → release)
- [ ] Upload AAB to Play Console → Internal testing → promote to production
- [ ] Merge to `release` branch → CI creates Git tag + GitHub Release

## API key / spending risk
Moving to a **Firebase Cloud Function proxy** (`functions/`, project `dailymacros-9fab8`) so the key never ships in the APK and spending is capped server-side.

- App (keyless users) → `openaiProxy` Cloud Function → OpenAI. The function holds the key as a deployed secret and meters every request in Firestore.
- **Caps:** per-user daily cap + global monthly request budget (tripwire, ~3000 req/mo ≈ $30) + manual kill switch, all tunable live in `config/limits` without redeploy.
- **Latency:** function is US-region + `minInstances: 1` (kept warm) so the added hop is ~50–200ms against a multi-second model call — negligible. Cold starts avoided by the warm instance.
- **BYO-key (hidden dev path):** if a personal key is set in Settings, the app still calls OpenAI directly, bypassing the proxy.
- Setup + deploy runbook: `functions/README.md`. Requires Blaze plan, Anonymous Auth, and Firestore enabled.

**Status:** ✅ **Deployed and validated.** The `openaiProxy` Cloud Function is live (Node 22, `us-central1`, `minInstances: 1`) holding `OPENAI_KEY` as a deployed secret and enforcing per-user daily + global monthly caps via `config/limits` in Firestore, with a manual kill switch. The embedded `CHATGPT_API_KEY` is removed from the app; `AuthInterceptor` routes keyless users through the proxy with an anonymous Firebase ID token, while the hidden BYO-key path still calls OpenAI directly. Verified end-to-end on device (real analysis + acceptable latency) and cap logic confirmed in Firestore. The app has no key fallback, so the proxy must stay live for shipped builds.
