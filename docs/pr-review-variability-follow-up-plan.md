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
| **C** | **Multi-archetype edge case:** `slotPreviewsForTemplate` can return slots from **multiple** archetypes; UI/picker uses `slots.first()` for `archetypeKey` / label | **Done** | One underlined link per archetype (`VariabilityArchetypePickerEntry`); picker opens for the tapped archetype’s slot subset only. |
| **D** | **`TemplateVariantPickerDialog`:** ensure `combinationKey` / confirm always has every `slotKey` (normalize `selection` before `combinationKey` and `onConfirm`) | **Done** | `normalizedSlotKeyToVariant()` + `combinationKey(slots, normalizedSelection)`; confirm passes full map. |
| **E** | **`ModalViewModel.onVariantPickerConfirmed`:** when `templateIdForCombinationInArchetype` returns null, **do not** silent `return@runSafely` — surface `ModalUiUpdates.Error` or snackbar | **Done** | Sends `ModalUiUpdates.Error` for unmatched combo and missing record. |
| **F** | **`ValidateEditRecordUseCase`:** replace `get(recordId)!!` with null-safe path returning `EditValidationResult.Error` | **Done** | `Record not found`; `ValidateEditRecordUseCaseTest`. |

---

## 2. Architecture & maintainability

| ID | Item | Status | Notes |
|----|------|--------|--------|
| **G** | **`DialogHandle.TemplateVariantPickerDialog` carrying full `profileJson`:** consider snapshot id or reload from `VariabilityRepository` when opening picker | **Done** | Carries parsed **`variabilityArchetypes`** on `View` + picker; JSON stays DB-only / `GetVariabilityMatch` parse path. |
| **H** | **Orchestration in `ModalViewModel`:** extract use cases (e.g. open picker, apply combination) with single `execute` each | **Done** | `OpenTemplateVariantPickerFromRecordDetailsUseCase`, `ApplyTemplateVariantPickerSelectionUseCase`; VM wires results + UI side effects. |
| **I** | **KDoc on repository methods** for incremental mine semantics (exclusive watermark, first run after upgrade) | **Done** | `RecordsRepository`, `RecordsDAO.getRecentForVariabilityMining`, `TemplatesDAO.countTemplatesWithActivityAfter`. |

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
| **L1** | **`MineMealVariabilityPreviewUseCase`:** unit tests for watermark path, empty delta → skip, `ingestWatermark` when `fromTemplates` null / zero | **Done** | `MineMealVariabilityPreviewUseCaseTest` (fakes). |
| **L2** | **`GetVariabilityMatchForTemplateUseCase`:** throw vs match branches, empty slots vs picker entries | **Done** | `GetVariabilityMatchForTemplateUseCaseTest`; `VariabilityProfileMapperParseTest` for ingest watermark on parse. |
| **L3** | **Picker open / apply:** open skipped, ready dialog, no-op same template, applied, unknown combo, record not found | **Done** | `TemplateVariantPickerUseCasesTest` (open + apply use cases; avoids full `ModalViewModel`). |
| **L4** | **Room `MIGRATION_12_13`:** schema packaged for unit tests; migration SQL covered by export + manual review | **Partial** | `Migration12To13SchemaPackagedTest` opens `schemas/.../12.json` from test assets (single exported `12.json` copy). `MigrationTestHelper` + JVM unit tests does not load those assets via instrumentation context; use `connectedAndroidTest` or a dedicated harness if full migration execution is required in CI. |
| **L5** | **`TemplateVariabilityPreviewMapper`:** extra cases for production-shaped JSON (null `templateId`, duplicate keys) if needed | **Done** | Extended `TemplateVariabilityPreviewMapperTest`. |
| **L6** | **`ValidateEditRecordUseCase`** after **F** | **Done** | Blank title, confirm multi-edit, single-record shortcut; `ValidateEditRecordUseCaseTest`. |

**Done:** `RecordDetailsVariabilityDifferentMealLinkVisibilityTest` for link visibility rule.

---

## 5. Suggested implementation order

1. ~~**D + E**~~ — done.  
2. ~~**J + K**~~ — done.  
3. ~~**F + L6**~~ — F done; L6 partially covered (extend tests as needed).  
4. ~~**L1–L4**~~ — L1–L3, L5–L6 done; L4 partial (schema packaging test; see L4 notes).  
5. ~~**C**~~ — done (multi-archetype links).  
6. ~~**I**~~ — done (repository / DAO KDoc).

---

## 6. Human re-review after changes

Re-run the original review pass on:

- `MineMealVariabilityPreviewUseCase.kt`, `MineMealVariabilityWorker.kt`
- `RecordsDAO` / `TemplatesDAO` delta queries
- `TemplateVariantPickerDialog.kt`, `ModalViewModel.kt` (picker paths)
- `VariabilityProfileMapper` / snapshot persistence
- `Migration12To13SchemaPackagedTest.kt` (test assets mirror of Room export)

---

*Generated from PR review consolidation + subsequent branch work. Update checkboxes or Status column as items land.*
