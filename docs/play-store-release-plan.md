# Play Store Release Plan

## Already done
- Prompt cleanup: split recognition/analysis prompts, Customise AI editor UI, "underestimate" instruction
- Google Drive backup (export/import via `appDataFolder`)

---

## Remaining (priority order)

### 1. Privacy policy page *(required, ~1 day)*
- Static page on GitHub Pages (or any hosting)
- Content: photos sent to OpenAI for analysis, nothing stored by developer, link to OpenAI's privacy policy
- Add URL to Play Console listing + app's About/Settings screen

### 2. Play Store listing materials *(required, ~1 day)*
- Short description (80 chars), long description (~4000 chars)
- Screenshots: 2–8 phone screenshots (Pixel-sized, 9:16)
- Feature graphic: 1024×500px banner

### 3. Data safety form *(required, ~2 hrs)*
- Manual form in Play Console — no code
- Key answers:
  - No data collected/stored by the developer
  - Photos shared with OpenAI (third party) for app functionality
  - No encryption at rest (no storage)

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

**Status:** Cloud Function written and ready to deploy. Android client rewiring (anonymous auth + proxy routing in `AuthInterceptor`) is the follow-up step once the deployed function is confirmed working.
