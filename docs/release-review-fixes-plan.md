# Release 1.14 review: follow-up work plan

Complex fixes from the two-axis review of the release diff (`origin/release...master`,
PR #186). Each item is meant to land as its own PR so it can be reviewed separately.
Items are ordered by value and by dependency: earlier items unblock later ones.

The easy fixes merged in #190. They are listed at the end for reference.

Status key: ⬜ not started · 🔄 in progress · 🟨 needs a decision first · ✅ done

## Status (2026-10-25)

| # | Item | Status | PR |
|---|---|---|---|
| — | Easy fixes, Health Connect availability crash | ✅ merged | #190 |
| 1 | Voice input through the ViewModel | ✅ merged | #188 |
| 2 | Quick-log single path | ✅ merged | #189 |
| 3 | Edit saving and edit rules out of ModalViewModel/ModalUiMapper | ✅ merged | #195 |
| 4 | AiInferenceSection values computed in the mapper | ✅ merged | #195 |
| 5 | Meal reminders layering and runner split, plus the alarm receiver error handling | ✅ merged | #192 |
| 6 | Backup conflict use case renamed | ✅ merged | #190 |
| 7 | Error-mapping dedupe | ⬜ next | — |
| 8 | Shake key checked in CI | ✅ merged | #190 |
| 9 | Health Connect states, meal types, stale-entry cleanup | ✅ merged | #193 |
| 10 | Shake privacy disclosure | ✅ merged | #190 |
| 11 | Shake behind the `shake_enabled` flag | ✅ merged | #190 |
| 12 | `testers.js` on the shared `lib/` helpers | ✅ merged, deployed and checked live | #191 |
| 13 | KMP convention plugin | ⬜ the owner's (KMP practice) | — |

**Found along the way**
- **Share sheet and photo-recognition dialogs skipped the "Discard?" confirmation:** ✅ merged in #194.
- **Health Connect showed every meal as a snack:** the meal type is now set from the reminder meal windows, in #193.
- **A whitespace-only edit loses a meal's macros:** 🔄 in progress, in its own PR stacked on #195.

**Still the owner's to do**
- Remote Config: delete `meal_reminders_visible`.
- Play Data safety: add user-initiated diagnostics (Shake reports).
- Cloud Logging: alerts matching `Tester signup failed` need the new text, `testers signup failed`.
- Release 1.14.0 (#186).

---

## 1. ✅ Voice input: state through the ViewModel (#188)

**Done:** the views pass the raw recognised text to `ModalViewModel`. A pure helper joins it
to what's already typed, with unit tests. A recogniser launch failure is logged and shown
as a toast instead of being swallowed.

**Problem**
- `appendSpokenText` (`features/modal/.../views/VoiceInput.kt:80`) trims, capitalises and
  joins text inside the view layer. It is called from `RecordDetailsView`,
  `RecordDetailsCreateView` and `MealCorrectionInput`. That breaks the no-logic-in-UI rule
  and the unidirectional data flow.
- `VoiceInputButton` swallows `ActivityNotFoundException` with an empty `catch`.

**Scope (decided):** dictation stays on the hot path only: title, description and the AI
correction field. Those three already have it in both the create and the edit dialogs, so
no fields are added. This item is the refactor only.

**Approach**
1. `ModalViewModel` gets `onTitleSpoken`, `onDescriptionSpoken` and `onCorrectionSpoken`
   handlers. The views pass the raw recognised text up and nothing else.
2. Joining the spoken text to what's already typed (trim, capitalise the first letter when
   the field is empty, cursor at the end) moves out of the view into a pure helper the
   ViewModel calls, with unit tests.
3. `VoiceInputButton` reports a launch failure upward. The ViewModel logs it through
   `AnalyticsLogger` and shows a snackbar ("Voice input isn't available").

**Done when** no view calls `appendSpokenText`, the join logic has unit tests, and a
launch failure is logged and shown, not swallowed.

## 2. ✅ ModalActivity quick-log moves into the ViewModel (#189)

**Done:** quick-pick logging has one path. The widget tap without confirmation still
finishes before the first draw, and errors are handled in one place.

**Problem**
- `ModalActivity.kt:222-240` reads `settingsRepository` and runs
  `LogMealFromTemplateUseCase` in its own `CoroutineScope`.
- That duplicates `ModalViewModel.kt:212`, which logs the same meal for the
  confirmation-dialog path.

**Approach**
- Keep the "finish before first draw" trick. Decide whether to show anything in the
  Activity using a pure check: a small use case that answers "should log without showing?".
- Hand the actual logging to a process-scoped runner that the ViewModel's path also uses.
  It could be an `@ApplicationScope` `CoroutineScope` provided in `app/di`, which is the
  standard Hilt pattern.
- Then there is one logging path, with one place that handles errors.

## 3. ✅ ModalViewModel and ModalUiMapper: move orchestration and business rules out (#195)

**Done:** no behaviour change.
- Characterisation tests went in first, as their own commit, and pass unchanged after the move. They're in
  `ModalViewModelTest` and cover which template a save writes (forked, updated in place or left
  alone) and when analysis is queued, for:
  - title, description, photo and timestamp edits
  - AI corrections
  - logging again from details or a quick pick
- `SaveRecordEditsUseCase` holds what `persistRecordEdits` did, including the logged-variants check
  and the one-run-per-shared-template scheduling.
- `ScheduleAnalysisIfTemplateIncompleteUseCase` is shared by the ViewModel's three log-again paths and
  `LogMealFromTemplateUseCase`.
- The four rules live in a pure `MealEditRules` helper in `features/modal`, with `MealEditRulesTest`.
  `ModalUiMapper` keeps only mapping, plus `hasUnsavedEdits` and `hasDisplayableContent`.

**Found, not changed:** saving a whitespace-only title or description change (e.g. a trailing space)
on a single-variant meal still gives the meal a new template without nutrients and doesn't queue an
analysis. A test pins this down. Worth fixing separately.

**Problem**
- `persistRecordEdits` (`ModalViewModel.kt:995-1048`) coordinates repositories itself.
  That belongs in a use case.
- `scheduleMacroAnalysisForRecordIfTemplateIncomplete` (`ModalViewModel.kt:886`) repeats
  `LogMealFromTemplateUseCase.kt:29-37`.
- `ModalUiMapper.kt:111-133` also holds business rules:
  - `imagesRequireMacroReanalysis`
  - `editsRequireMacroAnalysis`
  - `appendCorrection`
  - `correctionCapacity`

**Approach**
- Extract `SaveRecordEditsUseCase`. It decides whether to re-analyse, persists the edits
  and schedules the analysis.
- Extract `ScheduleAnalysisIfTemplateIncompleteUseCase`. Both the ViewModel and
  `LogMealFromTemplateUseCase` call it.
- Move the edit and correction rules into those use cases, or into a small pure
  `MealEditRules` helper in the feature that the use cases call.
- The mapper goes back to only mapping models to UI models.

**Risk** This is the most bug-fixed area in the app. Before moving any code, add tests
that capture the current behaviour.

## 4. ✅ AiInferenceSection: values computed in the view (#195)

**Done:** `ModalUiMapper.mapNutrientBreakdowns` fills three plain fields on
`NutrientBreakdownUiModel`: `hasAiInference`, `aiInferencePreview` and `notesHiddenByPreview`.
`ModalUiMapperTest` covers the preview and hiding rules. Whether the preview overflows its two lines
still comes from measuring the text in the view, because only the layout knows that.

**Problem**
- `views/AiInferenceSection.kt:64` has a computed `hasAiInference` extension.
- `:94` computes `previewText` and `:112` computes `notesHiddenByPreview`.

**Approach**
- `ModalUiMapper.map…` computes these and stores them on the breakdown UI model as plain
  fields.
- Add mapper tests for the preview and hiding rules.

## 5. ✅ Meal reminders: layering and runner cleanup (#192)

**Done:** the settings sit behind `MealReminderRepository` (`repositories/settings`), on the
same `meal_reminders` prefs file and keys. `MealReminderRunner` is now `RunMealReminderPassUseCase`
(it keeps the `Mutex`) plus `GetMealReminderStatusUseCase`. The record query is shared through
`GetMealReminderInputsUseCase`, and `SettingsViewModel` uses `MealReminderRefresher` and the
use cases only. `runSafely` rethrows cancellation and reports failures to Crashlytics. `trigger`
is still a `String`, to keep this change small.

**Problem**
- `MealReminderPrefs` (SharedPreferences) lives in a feature module.
- `SettingsViewModel` writes those prefs directly (`:638`) and injects the concrete
  `MealReminderCoordinator`, although a `MealReminderRefresher` interface exists.
- `MealReminderRunner` acts as a use case but exposes both `run()` and `status()`.
- `MealReminderRunner.kt:59-62` repeats the record query in
  `GetMealReminderPlanUseCase.kt:23-25`.
- `decision as? Remind/Silent` casts appear at `:77`, `:96` and `:101`, where a `when` on
  the sealed type would be clearer.
- `trigger: String` is a free-form label, not a type.
- `MealReminderCoordinator.runSafely` uses `runCatching`. That also catches
  cancellation, and failures only reach Logcat, not Crashlytics.

**Approach**
1. Move the reminder settings (enabled, history, last pass) behind a repository:
   - an interface in `repositories/settings/domain` (or a new `repositories/reminders`)
   - the implementation in the Android module
2. Split the runner:
   - `RunMealReminderPassUseCase.execute(trigger)`
   - `GetMealReminderStatusUseCase.execute()`
   - Both share a "load recent records" step with `GetMealReminderPlanUseCase`.
3. `SettingsViewModel` depends on `MealReminderRefresher` and the new use cases only.
4. Replace the casts with `when` over `MealReminderDecision`.
5. Model `trigger` as a small sealed type or enum. The alarm path maps intent actions onto
   it. Low value, so do it only while touching the file.
6. In `runSafely`, rethrow `CancellationException` and report other failures through
   `AnalyticsLogger.logError`.

## 6. ✅ CheckCloudBackupConflictUseCase: rename or split (#190)

**Outcome:** option A agreed and done. It's now `ReconcileCloudBackupConflictUseCase`, and
its test is renamed to match. No behaviour changed. The notes below are kept for the
record.

**Facts**
- `features/settings/.../export/useCases/CheckCloudBackupConflictUseCase.kt:22-24` does
  write `lastSynced`. It happens only when Drive is newer *and* this install wrote the
  file.
- PR #174 calls this "adopting" the timestamp. It repairs bookkeeping lost when an upload
  was cancelled, overlapped another sync, or lost its acknowledgement.
- The early `return false` is what stops false conflicts, not the write. When the call
  succeeds, both callers (`AutoSyncUseCase.kt:63` and `SettingsViewModel.kt:415`) set
  `lastSynced` again right afterwards.
- The write keeps the displayed "last backed up" time correct when the follow-up upload
  fails.

**History** (most relevant)

| Commit | Date | PR | Change |
|---|---|---|---|
| 78ae3dde | 2026-07-10 | #71 | First conflict check |
| 2c25c17d | 2026-07-12 | #71 | Removed a ViewModel write of the cloud timestamp into `lastSynced`, which "would defeat conflict detection" |
| 3c0dde1a | 2026-07-24 | #85 | Matches a lost-ack upload against Drive within a 30s window |
| 65dee82d | 2026-08-17 | #116 | Defers the `lastSynced` write until after the post-restore restart |
| 56a79189 | 2026-10-11 | #174 | Extracted this use case and added the "written by this install" adopt-write |

**Options**
- **A. Rename only:** e.g. `ReconcileCloudBackupConflictUseCase`, with the KDoc already
  explaining the adopt-write. No behaviour change.
- **B. Split:** a pure `IsCloudBackupConflictUseCase`, plus an `AdoptOwnCloudBackupUseCase`
  that both callers run.
  - Riskier. It brings back the #174 self-conflicts if the written-by-this-install guard is
    dropped.
  - It brings back the 2c25c17d bug if the adopt-write loses either of its two conditions.
  - Every caller has to call both.

**Recommendation** Go with A. Splitting buys little and re-opens a bug-dense area.

## 7. ⬜ Error mapping duplicated

The `errorUiMapper.map` → `analyticsLogger.logError` → user message sequence is repeated
in four places. Extract one helper or use case, e.g. `ReportUserFacingErrorUseCase`, that
returns the message to show.

## 8. ✅ Shake key: replace `verifyShakeApiKey` (#190)

**Outcome:** option 1 done.
- Both workflows now open with a "Require Shake API key" step that fails when the secret is
  empty.
- `verifyShakeApiKey` and its package-task wiring are gone from `app/build.gradle.kts`.
- `ShakeFeedback.start` logs a warning and returns when the key is blank.
- `CLAUDE.md` is updated to match.

Verified: unit tests and `assembleDebug` pass with a local key. **Not verified yet:**
- the new CI step, which runs on the first push or PR
- a local build with no key

Fork and Dependabot PRs get no secrets, so they fail at the first step, as they did
before, just earlier.

The original notes follow.

**Now**
- Gradle reads the key while configuring the build and writes it into `BuildConfig`.
- A separate task finds the package tasks by name and fails the build when the key is
  empty.
- Fine-grained build-step wiring like that is brittle and mixes a CI concern into the
  app's build.

**Options**
1. **Fail fast in CI (recommended).**
   - Each workflow gets a first step that fails when its secret is empty:
     `if: ${{ secrets.SHAKE_API_KEY == '' }}` → `run: echo "::error::SHAKE_API_KEY secret
     missing" && exit 1`. That fails in seconds instead of after a full build, with a
     clear message.
   - Gradle then just passes the key through.
   - `ShakeFeedback.start` goes back to skipping when the key is blank, with a log line,
     so local builds and forks without a key still build and run.
   - Only CI-built artifacts ever reach testers, and CI guarantees the key.
2. **Fail in Gradle only on CI.**
   - Keep the check, but run it while Gradle configures the build and only when
     `System.getenv("CI") == "true"`.
   - Drop the package-task wiring. Simpler than now, but Gradle still enforces a CI rule.
3. **Keep the current task, tidied up.**
   - Hook the check onto the variant's packaging step through AGP's variant API, instead
     of matching task names.
   - It still blocks local packaging without a key.

**Decision needed:** 1 is my recommendation. It also means `CLAUDE.md` and the
`ShakeFeedback` KDoc go back to "blank means not started".

## 9. ✅ Health Connect: better state when unavailable (#193)

**Done:**
- `SettingsUiState.healthConnectStatus` holds `HealthConnectStatus` (available /
  install-or-update required / not supported), read on start and whenever Settings resumes.
- The Health Connect row shows the toggle, an "Install/Update" action that opens the Play
  listing through the `OpenPlayStoreListing` UI update, or a disabled "Not available on this
  device" row.
- `HealthConnectSyncUseCase` is split into `GetHealthConnectStatusUseCase`,
  `HasHealthConnectPermissionUseCase` (throws unless available),
  `SyncRecordsToHealthConnectUseCase` and `DeleteRecordFromHealthConnectUseCase`, behind a
  `HealthConnectClientProvider` seam so they're unit tested.
- **Added after device testing:**
  - Synced meals get a Health Connect meal type from the reminder meal windows (breakfast,
    lunch, dinner, otherwise snack), where before every meal showed as a snack.
  - Each sync also removes entries for meals that no longer exist.

The original notes follow. The crash was fixed in #190: the toggle checks availability
first and shows a snackbar. The follow-up work was:
- Expose the SDK status in `SettingsUiState`.
  - When an install or update is needed, the row offers "Install/Update Health Connect",
    which opens the Play listing for `com.google.android.apps.healthdata`.
  - When the status is `SDK_UNAVAILABLE`, the row is disabled with "Not available on this
    device".
- `HealthConnectSyncUseCase` now has four public entry points: `isAvailable`,
  `hasPermission`, `execute` and `deleteRecord`. Split it into single-`execute()` use
  cases.

## 10. ✅ Privacy: Shake disclosure for testers (#190)

**Done:** `PRIVACY.md` has a new "In-app feedback for testers" section. It covers:
- who gets the button
- what Shake receives
- that reports are sent only on submit
- who sees them

Update the Play Data safety form to match if it doesn't already list user-initiated
diagnostics.

- `PRIVACY.md:86-87` says "No meal photos or diary content are sent to analytics or crash
  reporting." That is literally true, since Shake is a feedback tool. But Shake does
  receive screenshots, including meal photos, plus logs and activity history.
- Even with testers only, Play's Data safety form covers what the build can send, and
  testers are still data subjects.
- **Suggestion:** a short "Beta testing feedback" paragraph in `PRIVACY.md`:
  - what Shake receives
  - that only you see it
  - Shake's privacy link
- Shake is now gated (item 11), so the paragraph can say it's only switched on for testers
  and for users who ask for help.

## 11. ✅ Gate Shake behind a Remote Config flag (allowlist) (#190)

**Decided:** allowlist only. Android can't tell which Play track installed the app, so
"tester" means "on the list". Separate tester builds were considered and rejected, because
they would stop the same AAB being promoted to production.

**Done**
- **Flag:** `FeatureFlagStore.Key.SHAKE_ENABLED` (`shake_enabled`).
  - The code default is `false`, so Shake stays off when Remote Config is unreachable.
  - The console default is `true` while every user is a tester.
- **Live updates:** `FeatureFlagStore.observe(key)` emits the current value and then every
  activation. That includes real-time Remote Config (`addOnConfigUpdateListener`), so a
  console change applies within seconds, without the one-hour fetch throttle or a restart.
- **App start:** `ShakeFeedback.bind` in `App.onCreate` starts Shake when the flag turns on.
  - When the flag was already on last session, Shake still starts inside `onCreate`.
  - Turning the flag off mid-session hides the floating button, since Shake can't be
    stopped. From the next launch Shake doesn't start at all.
- **Per-user:** the app already sends its client ID as the `client_id` custom signal. The
  user reads their ID from Settings, and you add it to a console condition (custom signal
  `client_id` exactly matches …) that sets `shake_enabled` to `true`.

**At production launch:** switch the console default to `false`. From then on only
allowlisted users get Shake, so collecting testers' client IDs becomes part of tester
onboarding.

**Not verified yet:**
- Shake appearing mid-session after a console change, on a device. My expectation is that
  it shows on the next screen; the button might not appear on the screen that's already
  open.
- Whether real-time updates connect for this project. If they don't, it shows as a
  `FeatureFlags` warning in Logcat.

## 12. ✅ Backend: `functions/testers.js` duplicates `lib/` (#191)

**Done:** `joinTest` runs on the shared `lib/` signup code. `lib/` gained options for the
platform split and per-form emails, and `waitlist.js` keeps its own email text.
- **Checks:** an old-vs-new comparison matched exactly, and after deployment a live signup
  plus repeats wrote one doc and sent one email each.
- **One deliberate change:** the failure log line now reads `testers signup failed`.

`testers.js:29-30,42-89,91-156` re-implements:
- the secrets
- `normalizeEmail`
- `dedupeKey`
- the notification email
- the sign-up handler

`lib/` already provides all of these, and `lib/secrets.js` even says `testers.js` uses it.
Switch `testers.js` to the `lib/` versions and deploy with
`firebase deploy --only functions`.

## 13. ⬜ KMP build logic: convention plugin (owner, KMP practice)

The same KMP target block is repeated across 5 `build.gradle.kts` files. Move it into a
`buildLogic` convention plugin, e.g. `dailymacros.kmp.library`. Fits the KMP migration
plan.

## Deferred (tolerated for now)

- **Hardcoded user-facing strings, and string resource names that don't follow
  `[feature].[section].[description]`:** both get fixed in one pass when the app is
  internationalised.
- **`testingFreeForAll` not respected by the client's subscribe banner:** accepted for
  the alpha, since the banner can be dismissed.

---

## Easy fixes (merged in #190)

- Stopped tracking the `.claude/.idea/` IDE files and added `.claude/.idea/` to
  `.gitignore`.
- Fixed the escaped apostrophes (`you\'ve` → `you’ve`) in the settings and trends strings.
- Moved Hilt modules into `di/` packages:
  - `MealReminderModule` to `features/shared/.../di`
  - `AnalyticsSinkModule` to `core/analytics/.../di`
  - `ShakeFeedbackModule` to `feedback/di`, which keeps Shake in its own package
- Overview: the ViewModel trims the search term; the view no longer does.
- Health Connect: the toggle checks availability before `hasPermission()`, which throws
  when Health Connect is missing or outdated. That fixes a crash on Android 12–13 devices
  without an up-to-date Health Connect.
- Meal reminders released: removed `MEAL_REMINDERS_VISIBLE` and the debug-only gate, so
  the Reminders section always shows. Reminders stay opt-in (`prefs.enabled` defaults to
  false). The `meal_reminders_visible` key can be deleted in the Firebase console.
- `PRIVACY.md`: added `RECEIVE_BOOT_COMPLETED` and how voice input works (no microphone
  permission; the device's recogniser handles the audio).
- Renames:
  - `scheduleMacroAnalysisForAllRecordsUsingTemplate` → `scheduleMacroAnalysisForTemplate`
  - `onMealReminderDebugRowTapped` → `refreshMealReminderDebugText`
  - the variables `n`, `c` and `p` in `ModalUiMapper` → full names
  - `CheckCloudBackupConflictUseCase` → `ReconcileCloudBackupConflictUseCase` (item 6)
- Ported the two abandoned edits from the old `relaxed-bohr` worktree, then deleted it:
  - the "Carb:" label → "Carbs:" (`TemplateUiMapper`)
  - the "last backed up" date in Settings is formatted with Compose's `LocalLocale`
    instead of `Locale.getDefault()`
