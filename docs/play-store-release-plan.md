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
Key is a GitHub secret injected at build time (not in git). R8/ProGuard obfuscation is on for release builds. Set a tight spending cap in your OpenAI account and monitor usage. Acceptable for v1 — add a proxy in v2 if abuse occurs.
