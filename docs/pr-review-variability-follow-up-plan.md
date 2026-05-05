# Follow-up plan: variability / template timestamps PR review

This document turns the consolidated PR review into an ordered implementation checklist.  
**Branch context:** `cursor/variability-template-timestamps-ui-8ebf` (and related variability work).

Legend: **Done** = already implemented on the branch at time of writing; **Todo** = still open.

---

## 1. Correctness & data safety

| ID | Item | Status | Notes / where |
|----|------|--------|----------------|
| **A** | Pass `templatesIngestWatermarkEpochMs` from snapshot into `VariabilityProfileMapper.parseProfileJson` in `GetVariabilityMatchForTemplateUseCase` | **Done** | Aligns parsed in-memory profile with DB snapshot metadata. |
| **B** | Incremental mine: do **not** call the model with empty `meal_observations` when delta query returns no rows; do **not** replace stored profile | **Done** | `MineMealVariabilityPreviewUseCase` + worker notification for skip path. |
| **C** | **Multi-archetype edge case:** `slotPreviewsForTemplate` can return slots from **multiple** archetypes; UI/picker uses `slots.first()` for `archetypeKey` / label | **Todo** | **Options:** (1) document “one archetype per template in practice”; (2) group preview rows by `archetypeKey` and expose one link/picker per archetype; (3) restrict mapper to a single archetype when building preview for a template. Decide with product. |
| **D** | **`TemplateVariantPickerDialog`:** ensure `combinationKey` / confirm always has every `slotKey` (normalize `selection` before `combinationKey` and `onConfirm`) | **Done** | `normalizedSlotKeyToVariant()` + `combinationKey(slots, normalizedSelection)`; confirm passes full map. |
| **E** | **`ModalViewModel.onVariantPickerConfirmed`:** when `templateIdForCombinationInArchetype` returns null, **do not** silent `return@runSafely` — surface `ModalUiUpdates.Error` or snackbar | **Done** | Sends `ModalUiUpdates.Error` for unmatched combo and missing record. |
| **F** | **`ValidateEditRecordUseCase`:** replace `get(recordId)!!` with null-safe path returning `EditValidationResult.Error` | **Done** | `Record not found`; `ValidateEditRecordUseCaseTest`. |

---

## 2. Architecture & maintainability

| ID | Item | Status | Notes |
|----|------|--------|--------|
| **G** | **`DialogHandle.TemplateVariantPickerDialog` carrying full `profileJson`:** consider snapshot id or reload from `VariabilityRepository` when opening picker | **Todo** | Reduces memory / staleness; larger refactor. |
| **H** | **Orchestration in `ModalViewModel`:** extract use cases (e.g. open picker, apply combination) with single `execute` each | **Todo** | Lowers coupling; do when picker flow grows. |
| **I** | **KDoc on repository methods** for incremental mine semantics (exclusive watermark, first run after upgrade) | **Todo** | `RecordsRepository` / DAO queries. |

**Already improved on branch:** variability link visibility moved off `ModalActivity` into dialog state + `computeShowVariabilityDifferentMealLink` + unit test (`RecordDetailsVariabilityDifferentMealLinkVisibilityTest`).

---

## 3. Debuggability & UX

| ID | Item | Status | Notes |
|----|------|--------|--------|
| **J** | **`SettingsViewModel`:** `runCatching` for `nextMineTemplateCount` — add `onFailure` (log + optional snackbar or leave count unchanged explicitly) | **Done** | `Log.w` + `ShowSnackbar` on post-mine refresh and `refreshTemplateCountForSettings`. |
| **K** | **`SettingsUiState`:** merge duplicate / conflicting KDoc lines for `nextMineTemplateCount` | **Done** | Single KDoc line. |

---

## 4. Tests

| ID | Item | Status | Notes |
|----|------|--------|--------|
| **L1** | **`MineMealVariabilityPreviewUseCase`:** unit tests for watermark path, empty delta → skip, `ingestWatermark` when `fromTemplates` null / zero | **Todo** | Fakes or test doubles for repos + ChatGPT. |
| **L2** | **`GetVariabilityMatchForTemplateUseCase`:** throw vs match branches, `profileJson` null when no slots | **Todo** | |
| **L3** | **`ModalViewModel`:** picker open / confirm / cancel / no-op same template | **Todo** | `kotlinx-coroutines-test` + fakes, or extract pure helpers first. |
| **L4** | **Room `MIGRATION_12_13`:** in-memory migration test (schema + defaults) | **Todo** | |
| **L5** | **`TemplateVariabilityPreviewMapper`:** extra cases for production-shaped JSON (null `templateId`, duplicate keys) if needed | **Partial** | `TemplateVariabilityPreviewMapperTest` exists; extend as bugs appear. |
| **L6** | **`ValidateEditRecordUseCase`** after **F** | **Partial** | `ValidateEditRecordUseCaseTest` covers missing record + unchanged shared template. |

**Done:** `RecordDetailsVariabilityDifferentMealLinkVisibilityTest` for link visibility rule.

---

## 5. Suggested implementation order

1. ~~**D + E**~~ — done.  
2. ~~**J + K**~~ — done.  
3. ~~**F + L6**~~ — F done; L6 partially covered (extend tests as needed).  
4. **L1–L4** — test coverage for mining, match use case, migration.  
5. **C** — product/architecture decision, then implement.  
6. **G + H + I** — larger refactors / documentation when capacity allows.

---

## 6. Human re-review after changes

Re-run the original review pass on:

- `MineMealVariabilityPreviewUseCase.kt`, `MineMealVariabilityWorker.kt`
- `RecordsDAO` / `TemplatesDAO` delta queries
- `TemplateVariantPickerDialog.kt`, `ModalViewModel.kt` (picker paths)
- `VariabilityProfileMapper` / snapshot persistence
- `AppDatabase.kt` migration 12→13

---

*Generated from PR review consolidation + subsequent branch work. Update checkboxes or Status column as items land.*
