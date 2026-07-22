Finish the in-progress coding on the current branch. The developer has sketched the gist of a feature or bug fix but left the nitty-gritty: tests, translations, DI wiring, error handling, and incomplete layers. Identify every gap and implement them all.

## Branch/range detection (required, user-selected base)

Determine the work range **before** analysing code:

1. List recent branch candidates using the Bash tool:
   - Run: `git branch -a --sort=-committerdate --format="%(refname:short)|%(objectname:short)|%(committerdate:iso)" 2>/dev/null | head -60`
   - Include both local and remote-tracking branches, excluding symbolic refs like `remotes/*/HEAD`.
   - Exclude the current branch in all forms (local name, upstream ref, remote-tracking ref with the same short name).
   - If a local and remote branch share the same short name, show one row but use the fully-qualified ref internally.
   - Sort by most recently updated tip commit, descending.
   - Use AskUserQuestion with a single-select picker to let the user choose the base branch.
   - Show the first 5 candidates. Each option label must be plain text: `<name-padded> | <short-sha>` (no markdown, no backticks). Right-pad names so SHAs align.
   - Add a `More...` option (value `__MORE__`) if there are more candidates.
   - If the user picks `More...`, show the next 10 and ask again. Repeat until all are shown.
   - Do not include a free-text fallback. Do not auto-select a branch.
   - If the user dismisses without selecting, stop and report the blocker.

2. Resolve the selected branch:
   - Use the exact ref returned by the picker as `<resolved-parent-branch>`.
   - Validate it exists: `git rev-parse --verify <ref>`.
   - If invalid, stop and report the blocker.

3. Compute the fork point:
   - `FORK_POINT=$(git merge-base HEAD <resolved-parent-branch>)`
   - If it fails, stop and report the blocker.

4. Work only within the range `FORK_POINT..HEAD`:
   - Get the diff: `git diff <FORK_POINT>..HEAD`
   - Get the commit list: `git log --oneline <FORK_POINT>..HEAD`

5. Print a **Base resolution checkpoint** in chat:
   - Selected parent branch, fork point SHA, reviewed range, commit count.
   - Then proceed immediately — do not ask for confirmation.

6. Never use `HEAD^..HEAD` unless explicitly requested.

## Phase 1: Intent analysis (read-only)

Read the diff and any files it touches to build a clear picture of:
- What feature or bug fix the developer was working on.
- Which architectural layers were touched (UI, ViewModel, use case, repository, service).
- What the developer already completed vs. what they left unfinished.

Read referenced source files in full where the diff alone is not enough to understand intent.

## Phase 2: Gap analysis

Inspect every dimension below. For each gap found, record it in a numbered checklist.

### Completeness
- `TODO` / `FIXME` comments left in changed files.
- Stub or placeholder implementations (empty bodies, `throw NotImplementedError`, hardcoded dummy values).
- Missing architectural layers — e.g., UI sketched but no use case; use case written but no repository method; repository method added but never called.
- Dead code paths that are unreachable or clearly unfinished.

### Tests
- New or changed behavior in use cases, repositories, or ViewModels without unit tests.
- ViewModel state transitions or UiUpdates not covered.
- Repository methods without unit tests.
- Check the existing test files to understand the naming and structure conventions before writing any test.

### Translations
- New user-facing strings in `values/strings.xml` that are absent from `values-es/strings.xml` or `values-ja/strings.xml`.

### Dependency injection
- `@Inject constructor` implementations or `@HiltViewModel` classes that are not yet bound in a Hilt module (`@Binds` in a `di/` package).

### Error handling
- Repository or use-case calls in a ViewModel that do not handle `DomainError` subclasses.
- Empty or swallowed catch blocks introduced in the diff.

### Analytics
- New user-triggered actions in changed UI code that lack analytics events, if comparable existing actions in the same feature fire events.

## Phase 3: Plan

Print the numbered gap checklist clearly. Group items by category. Mark each item with its target file(s) where already known.

Ask the user a single yes/no question — "Should I proceed with implementing all of the above?" — using AskUserQuestion. If the user says no, stop.

## Phase 4: Implement

Work through the checklist in this order:

1. **Missing layers** — complete the architectural skeleton before writing tests.
2. **Tests** — write unit tests for all changed behavior; inspect an existing analog test file first and mirror its structure exactly.
3. **Translations** — add the missing strings to `values-es/strings.xml` and `values-ja/strings.xml`.
4. **DI wiring** — add `@Binds` entries or Hilt modules as needed.
5. **Error handling** — add `DomainError` handling where missing.
6. **Analytics** — add analytics calls mirroring the existing pattern.
7. **TODOs / stubs** — replace placeholders with real implementations.

Rules for every file you create or edit:
- Before writing, inspect the most similar existing file in the project and mirror its structure, naming, and style exactly.
- After creating a new file, immediately run `git add <path>`.
- Do not refactor or clean up code the developer did not touch.
- Do not add features or behavior beyond what the partial implementation implies.
- If a gap is genuinely ambiguous (e.g., a TODO with no clear meaning), skip it, record it as a QUESTION, and explain why.
- Never delete, skip, or weaken existing tests.
- Adhere to all conventions in CLAUDE.md.

## Phase 5: Verify

After all changes are made, run:

```
./gradlew testDebugUnitTest --rerun-tasks
```

Fix any test failures before reporting done. Do not skip or weaken a test to make it pass — fix the production code or the test expectation based on the intended behavior.

## Output

Summarize what was done in chat:
- Items implemented (with file links).
- Items skipped (with reason).
- QUESTIONs for the developer.
- Test run result.
