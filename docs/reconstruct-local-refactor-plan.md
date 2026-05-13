# Reconstruct local refactor after reset

Plan for merging **this chat’s local refactor work** with **your latest cloud tree** after you wipe the working tree and check out the target branch (assumed: `origin/master` at `82d5fe1`, unless your cloud branch differs).

---

## 1. Current situation (snapshot)

### Git refs (as of plan write)

| Ref | Commit | Notes |
|-----|--------|--------|
| Local `master` (HEAD) | `03af99a` | 8 commits **not** on `origin/master` |
| `origin/master` | `82d5fe1` | Merge PR #36 (modal save race), CI prerelease tweak |
| Merge-base | `ab05f1b` | Common ancestor |

Local is **ahead** of `origin/master` by: `770460e` … `03af99a` (SAF/Hilt settings, DI decentralization, widget auto-reload, `GetMacrosWorker` → `NutrientAnalysisWorker`, ChatGPT repo error mapping, shared workers/use case).

`origin/master` is **ahead** of local by: `80db879`, `c7d5a3a`, `82d5fe1` (modal save race fix, CI).

### Dirty working tree (on top of `03af99a`)

Uncommitted delta (~21 paths, −911 / +69 lines vs HEAD). This is the **second wave** of work from this chat (fixes incomplete `03af99a`, prefs module, drop runner SPI).

| Area | What the unstaged diff does |
|------|-----------------------------|
| **Incomplete commit fix** | `03af99a` moved `MineMealVariabilityWorker` to `:features:shared` but `AppSingletonModule` still imported `features.main.MineMealVariabilityWorker` (deleted file). Unstaged fixes import + adds `@Provides` for notification SPIs. |
| **Nutrient analysis wiring** | Import updates in modal/overview/delete/cancel/apply use cases + `Notifications.kt` (`CHANNEL_ID_FOREGROUND` from shared). |
| **Variability mining cleanup** | Deletes settings-side use case, test, `MealVariabilityMiningWork.kt`, settings `strings.xml`, settings `SettingsPrefs.kt`; deletes committed `MealVariabilityMiningPreviewRunner.kt`; deletes `repositories/settings/SettingsPrefs.kt` (moved to prefs module). |
| **Variability mining evolution** | Untracked: `MineMealVariabilityPreviewUseCase` + test under `:features:shared` (`variabilityMining/` package); worker injects use case directly (no runner SPI). |
| **Module split** | Untracked `:repositories:settings:prefs`; `settings.gradle.kts` + `features/shared/build.gradle.kts` updated. |

### Important: variability mining on `origin/master`

Fetched `origin/master` **still contains** meal variability **mining** (worker, preview use case, settings UI, `mineMealVariability` on `ChatGPTRepository`, DB variability tables, template slot UI, etc.).

You said cloud work **removed variability mining**. Before reconstructing, on the branch you will actually use:

```bash
git checkout <your-target-branch>
# Then search:
rg -l "MineMealVariability|mineMealVariability|EnqueueMealVariability|variability_mining" --glob '*.kt'
```

- If **no matches** → treat “mining” sections below as **SKIP** (and optionally delete any leftover mining code still on that branch).
- If **matches remain** → apply mining-related steps only where the feature still exists.

**Template variability** (archetypes/slots in record details, `VariabilityRepository`, DB entities) is separate from **mining** (background worker + settings preview + ChatGPT `mineMealVariability`). Cloud may remove mining while keeping slot UI — verify on target branch.

---

## 2. Inventory: work from this chat

Grouped by theme. Each row: **Recommendation** after reset.

### A. Settings / SAF / Hilt (`770460e`)

| Item | Recommendation |
|------|----------------|
| SAF `CreateDocument` / `OpenDocument` as Compose `remember…()` | **KEEP** — re-apply on `origin` settings screen |
| `SettingsViewModel` → `@HiltViewModel` (drop `SettingsViewModelFactory`) | **KEEP** |
| `SharePublicUriLauncher`: `@ApplicationContext`, `@Singleton`, `FLAG_ACTIVITY_NEW_TASK` | **KEEP** |

### B. Network & DI decentralization (`b73f7f9`, `90d75ea`, `c03ffe9`)

| Item | Recommendation |
|------|----------------|
| `AppNetworkModule` → `:repositories:chatgpt` (`ChatGptNetworkModule`) | **KEEP** |
| Qualifiers (`@ForImageUploadChatGpt`, `@ForJsonBodyChatGpt`, `@ChatGptClientGson`, file-store qualifiers → `:data:file`) | **KEEP** |
| `AppDatabase` Hilt-owned; remove static singleton (`Option B`) | **KEEP** |
| `DataDbModule`, `DataFileModule`, `DataImageModule`, repository `@Binds` modules | **KEEP** |
| Backup restore → `RestartPending` / `appDatabase.close()` (no re-init rollback) | **KEEP** |
| Trim `AppSingletonModule` to app-only providers | **KEEP** (contents depend on mining — see D) |

### C. Widget (`19879ef`)

| Item | Recommendation |
|------|----------------|
| `WidgetAutoReloader` + `observeQuickPicks` + debounced reload | **KEEP** |
| `FoodDiaryWidgetReloader` interface → `:features:shared`; impl → `:features:widget` | **KEEP** |
| Remove explicit `scheduleReload` from ViewModels / `NutrientAnalysisUseCase` | **KEEP** |
| `DiaryWidgetScreen` constants `RECORD_DAYS_TO_DISPLAY`, `QUICK_PICK_COUNT` | **KEEP** |

### D. Nutrient analysis worker & use case (`a78e743`, `03af99a`, unstaged)

| Item | Recommendation |
|------|----------------|
| Rename `GetMacrosWorker` → `NutrientAnalysisWorker` | **KEEP** |
| Move `NutrientAnalysisUseCase` → `:features:shared` | **KEEP** |
| Move `NutrientAnalysisWorker` → `:features:shared` | **KEEP** |
| `MacroResultsNotificationSender` SPI + `@Provides` in `AppSingletonModule` | **KEEP** |
| `NotificationChannelIds.CHANNEL_ID_FOREGROUND` in shared; `Notifications.kt` imports it | **KEEP** |
| `ic_nutrition.xml` in `:features:shared` (worker small icon; not app `R`) | **KEEP** |
| Update all imports / test stubs (`NutrientAnalysisWorker` package) | **KEEP** |
| `features/shared/build.gradle.kts`: Hilt, WorkManager, `:data:image`, `:repositories:chatgpt`, `:core:analytics`, coroutines-guava | **KEEP** (no `:features:settings`) |

### E. ChatGPT errors (`c20d493` + unstaged use-case cleanup)

| Item | Recommendation |
|------|----------------|
| `DomainError` → `ChatGPTDomainError` | **KEEP** |
| `ChatGPTMapper` in `:repositories:chatgpt` | **KEEP** |
| `ChatGPTRepositoryImpl` catches `ChatGPTApiError`, throws `ChatGPTDomainError` | **KEEP** |
| Remove `ChatGPTApiError` handling from `FoodRecognitionUseCase` / `NutrientAnalysisUseCase` | **KEEP** |
| `NutrientAnalysisUseCase`: retry worker on `ChatGPTDomainError.DisplayMessageToUser.CheckInternetConnection` | **KEEP** |
| `ModalViewModel` / `MacrosNotificationTextMapper` use `ChatGPTDomainError` | **KEEP** |

### F. Meal variability **mining** (`03af99a` + unstaged)

| Item | Recommendation |
|------|----------------|
| `MineMealVariabilityWorker` in `:features:shared` | **SKIP** if mining removed on target branch |
| `MineMealVariabilityPreviewUseCase` (settings or shared) | **SKIP** if mining removed |
| `MealVariabilityMiningPreviewRunner` + `VariabilityMiningPreviewSnapshot` | **SKIP** (superseded by direct injection; and skip entirely if no mining) |
| `TitleTextNotificationSender` SPI | **SKIP** if mining removed (only used by mining worker) |
| `MEAL_VARIABILITY_MINING_*` work constants in shared | **SKIP** if mining removed |
| `features/shared/.../strings.xml` variability mining notification strings | **SKIP** if mining removed |
| `SettingsPrefs` variability-mining debug cache fields | **SKIP** if mining removed |
| `:repositories:settings:prefs` module | **SKIP** if prefs only existed for mining UI cache |
| `EnqueueMealVariabilityMining` + settings preview UI / `WorkManager` observer | **SKIP** if mining removed |
| `ChatGPTRepository.mineMealVariability` + prompt + `VariabilityMiningResult` | **SKIP** only if product removed API entirely (confirm on target branch) |

### G. Cloud-only on `origin/master` (not in local commits)

| Item | Recommendation |
|------|----------------|
| Modal save race: persist before `closeAll()` (`80db879`) | **Already on target** — do not revert |
| CI prerelease on same-repo PRs (`c7d5a3a`) | **Already on target** |

---

## 3. What to drop from local commit `03af99a` when mining is gone

If target branch has **no mining**, cherry-pick or manual replay should **omit** these paths from `03af99a`:

- `features/shared/.../MealVariabilityMiningPreviewRunner.kt`
- `features/shared/.../variabilityMining/MineMealVariabilityWorker.kt`
- `features/shared/src/main/res/values/strings.xml` (mining-only strings)
- `repositories/settings/.../SettingsPrefs.kt` (mining-only prefs; unless you still need prefs for something else)

Still **take** from `03af99a`:

- `NutrientAnalysisUseCase.kt`, `NutrientAnalysisWorker.kt`
- `notifications/*`, `NotificationChannelIds.kt`
- `ic_nutrition.xml`

---

## 4. Reconstruction procedure (after `git reset --hard` + checkout target)

Suggested order — each step ends with compile or `./gradlew :app:compileDebugKotlin` (or full `testDebugUnitTest` at end).

### Phase 0 — Baseline

1. `git fetch origin`
2. `git checkout <target-branch>` (e.g. `origin/master`)
3. `git clean -fdx` (only if you intend to wipe untracked; **destructive**)
4. Run `./gradlew testDebugUnitTest --no-daemon` — baseline green
5. Run variability/mining inventory (`rg` above) and tick **KEEP/SKIP** for section F

### Phase 1 — Foundation (commits `770460e` → `90d75ea`)

Re-apply in order (cherry-pick if clean, else manual):

1. SAF + `@HiltViewModel` settings
2. ChatGPT network module move
3. Hilt `AppDatabase`, file/image stores, repository modules, backup restart semantics

**Conflict hotspots with `origin/master`:** `AppSingletonModule.kt`, `App.kt`, `ModalActivity`, `settings` gradle, any file touched by modal save fix.

### Phase 2 — Widget (`19879ef`)

1. `observeQuickPicks` + DAO query
2. `WidgetAutoReloader`, `App` bootstrap
3. Relocate `FoodDiaryWidgetReloader` SPI; delete app `FoodDiaryWidgetReloaderImpl` module

### Phase 3 — Nutrient analysis (`a78e743` + shared move)

1. Rename worker + all references
2. Add shared notification SPIs + `ic_nutrition` + channel id
3. Move use case + worker to `:features:shared`; extend `features/shared/build.gradle.kts` (Hilt/WorkManager/deps)
4. `AppSingletonModule`: **only** `macroResultsNotificationSender` (+ existing gson/settingsAppInfo; **no** mining enqueue if mining skipped)
5. Fix call sites (overview, modal, delete, cancel, apply)

### Phase 4 — ChatGPT errors (`c20d493`)

1. Rename `ChatGPTDomainError`, add `ChatGPTMapper`
2. Map in `ChatGPTRepositoryImpl`; wire mapper in `ChatGptNetworkModule`
3. Strip API error handling from feature use cases

### Phase 5 — Mining (conditional)

**Only if section F is KEEP on target branch:**

1. Prefer **final** design from unstaged work (not raw `03af99a`):
   - Worker in `:features:shared` (`variabilityMining` package optional)
   - Use case in same module; **no** `MealVariabilityMiningPreviewRunner`
   - `:repositories:settings:prefs` for `SettingsPrefs` (not `:features:settings`, not `:repositories:settings` impl)
   - Work constants in shared (`MealVariabilityMiningWork.kt`)
   - Mining strings in `features/shared/res/values/strings.xml`
   - `TitleTextNotificationSender` + `@Provides` in app
   - `:features:settings` depends on `:features:shared` (not the reverse)
2. Delete app `features/main/MineMealVariabilityWorker.kt` if still present
3. Fix `AppSingletonModule` import to `features.shared.variabilityMining.MineMealVariabilityWorker`

**If mining removed on target:** delete/stop wiring any remaining mining types; remove `enqueueMealVariabilityMining` from `AppSingletonModule` if interface deleted; drop `:repositories:settings:prefs` from shared gradle if unused.

### Phase 6 — Validate

```bash
./gradlew testDebugUnitTest --no-daemon
```

Manual smoke (if you have a device):

- Settings export/import still works (SAF)
- Widget updates after record/quick-pick change
- Macro analysis from modal + background worker notification
- Modal save does not race (origin fix)

---

## 5. Cherry-pick vs manual replay

| Approach | When |
|----------|------|
| `git cherry-pick 770460e^..03af99a` | Target branch is close to merge-base and conflicts are manageable |
| Cherry-pick per commit | Easier to drop `03af99a` mining files between picks |
| Manual replay using this doc | Target branch diverged heavily (mining removed, large cloud edits) — **recommended** if cloud removed mining |

After cherry-pick of `03af99a`, you would still need unstaged fixes (AppSingletonModule, prefs module, delete runner SPI) — or skip `03af99a` and only apply Phase 3 files manually.

---

## 6. File checklist (nutrient path only — no mining)

Use as a copy/paste checklist when replaying without mining.

### New files

- `features/shared/.../NutrientAnalysisUseCase.kt`
- `features/shared/.../NutrientAnalysisWorker.kt`
- `features/shared/.../notifications/MacroResultsNotificationSender.kt`
- `features/shared/.../notifications/NotificationChannelIds.kt`
- `features/shared/src/main/res/drawable/ic_nutrition.xml`
- `data/db/di/DataDbModule.kt` (+ related module files from Phase 1)
- `repositories/chatgpt/ChatGPTMapper.kt`
- `repositories/chatgpt/domain/.../ChatGPTDomainError.kt` (rename from DomainError)
- `features/widget/.../WidgetAutoReloader.kt` (+ widget module changes)

### Delete / stop using

- `app/.../features/modal/usecase/NutrientAnalysisUseCase.kt`
- `app/.../features/common/workers/GetMacrosWorker.kt` (after rename move)
- `app/.../di/FoodDiaryWidgetReloaderImpl.kt` + module (if relocated)

### Touch imports

- `ModalViewModel`, `OverviewViewModel`, `DeleteRecordUseCase`, `ApplyConfirmedSharedTemplateEditUseCase`, `CancelMacrosAnalysisForRecordUseCase`, `Notifications.kt`
- All tests referencing `GetMacrosWorker` / old use case package

---

## 7. Known incomplete state in `03af99a` (do not replay blindly)

- `AppSingletonModule` pointed at deleted `features.main.MineMealVariabilityWorker` while worker lived under `features.shared.variabilityMining` — **must** update import and notification `@Provides` together.
- `MealVariabilityMiningPreviewRunner` was added then removed in unstaged work — **do not** reintroduce.
- `SettingsPrefs` in `repositories/settings` (committed) was wrong long-term home — use `:repositories:settings:prefs` only if mining (or other prefs) still need it.

---

## 8. Suggested commit strategy after reconstruction

Split for reviewability (optional):

1. `refactor: decentralize Hilt providers and Hilt-own Room`
2. `refactor: settings SAF in Compose and Hilt ViewModel`
3. `feat: widget auto-reload via repository flows`
4. `refactor: move nutrient analysis to features/shared and rename worker`
5. `refactor: map ChatGPT API errors in repository`
6. *(optional)* `refactor: meal variability mining in shared` — only if still product-relevant

---

## 9. Next session prompt (for the agent)

When you have checked out the target branch and wiped local changes, paste something like:

> Reconstruct per `docs/reconstruct-local-refactor-plan.md`. Target branch: `<name>`. Variability mining on branch: yes/no (result of rg). Skip all mining steps if no. Run `testDebugUnitTest` when done.

---

*Generated from working tree at local `03af99a` + unstaged diff vs `origin/master` @ `82d5fe1`.*
