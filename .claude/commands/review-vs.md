Review the current branch against a base branch selected by the user.

Do not modify production code.

Act as four independent reviewers, then run a final PR-review judge pass to consolidate and arbitrate findings.

## Branch/range detection (required, user-selected base)

Determine the review range **before** reviewing code:

1. List recent branch candidates using the Bash tool:
   - Run: `git branch -a --sort=-committerdate --format="%(refname:short)|%(objectname:short)|%(committerdate:iso)" 2>/dev/null | head -60`
   - Include both local branches and remote-tracking branches (`remotes/*`), excluding symbolic refs like `remotes/*/HEAD`.
   - Exclude the current branch in all forms (local name, its upstream ref, and any remote-tracking ref with the same short name).
   - If a local and remote branch share the same short name, show one row but use the fully-qualified ref internally.
   - Sort by most recently updated tip commit timestamp, descending.
   - Use the AskUserQuestion tool with a single-select picker to let the user choose the base branch.
   - Show the first 5 candidates. Each option label must be plain text: `<name-padded> | <short-sha>` (no markdown, no backticks). Right-pad names so SHAs align in a column. Each option value must be the exact fully-qualified ref.
   - Add a `More...` option (value `__MORE__`) if there are more candidates not yet shown.
   - If the user picks `More...`, show the next 10 candidates and ask again. Repeat until all are shown.
   - Do not include a free-text fallback. Do not auto-select a branch.
   - If the user dismisses without selecting, stop and report the blocker.

2. Resolve the selected branch:
   - Use the exact ref returned by the picker as `<resolved-parent-branch>`.
   - Validate it exists: `git rev-parse --verify <ref>`.
   - If invalid, stop and report the blocker.

3. Compute the fork point:
   - `FORK_POINT=$(git merge-base HEAD <resolved-parent-branch>)`
   - If it fails, stop and report the blocker.

4. Review only the range `FORK_POINT..HEAD`:
   - Get the diff: `git diff <FORK_POINT>..HEAD`
   - Get the commit list: `git log --oneline <FORK_POINT>..HEAD`

5. Print a **Base resolution checkpoint** block in chat:
   - Selected parent branch
   - Fork point SHA
   - Reviewed range (`<FORK_POINT>..HEAD`)
   - Commit count
   - Then proceed immediately with the review — do not ask for confirmation.

6. Never use `HEAD^..HEAD` unless explicitly requested.

## Reviewer 1: Correctness
Check whether the task is actually solved.
Check for bugs, regressions, edge cases, nullability, lifecycle issues, coroutine/Flow issues, API/model mapping mistakes, date/time/locale bugs.

## Reviewer 2: Architecture and scalability
Check for layer violations, domain/data/UI model leakage, duplicated logic, unclear ownership, excessive coupling, abstractions that will not scale, and whether the design fits the existing architecture in CLAUDE.md.

## Reviewer 3: Debuggability and operations
Check error handling, logging, analytics events if relevant, silent failures, swallowed exceptions, vague failure states, hard-to-debug state transitions, and missing diagnostics.

## Reviewer 4: Tests
Check changed behavior without tests, weak assertions, missing regression tests, missing use-case tests, coroutine timing/flakiness, and tests that should be unit tests rather than instrumentation tests.
Check missing (emulator-based) ui tests.

## Final Judge (required)
You are the final PR review judge.

Inputs: all four reviewer outputs plus the branch diff.

Tasks:
1. Deduplicate findings across reviewers.
2. Separate must-fix (BLOCKER) from nice-to-have.
3. Note any disagreement between reviewers.
4. Produce a merge-blocking checklist.
5. Produce a small safe fix plan.

Judge behavior:
- Do not modify files.
- Mark BLOCKER only for likely bug, regression, data corruption, broken UX, broken build/migration, security issue, or serious maintainability trap.
- If uncertain, mark as FOLLOW-UP.

## Output

Write the review to:

`docs/ai-review/review-vs-findings-<current-branch-short-sha>-<parent-branch-short-sha>.md`

Create the `docs/ai-review/` directory if it does not exist.

Use this structure:

```
# Branch Review

## Base resolution
- Resolved parent branch: `<branch>`
- Fork point SHA: `<sha>`
- Reviewed range: `<sha>..HEAD`
- User-selected branch: `<branch>`

## Intended change
<summary of what this branch is meant to do>

## Correctness findings

## Architecture and scalability findings

## Debuggability findings

## Test findings

## Final judge consolidation

### Deduplicated must-fix (BLOCKERs)

### Deduplicated nice-to-have

### Reviewer disagreements

## Consolidated merge decision
<!-- Choose one: Safe to merge | Safe to merge after small fixes | Not safe to merge yet -->

## Merge-blocking checklist

## Small safe fix plan

## Should-fix checklist

## Follow-ups

## Human review checklist
<!-- List the 5-10 exact files/functions to personally inspect -->
```

## Rules

- Do not give vague style comments.
- Do not request broad refactors unless there is a concrete risk.
- Do not duplicate the same issue across sections.
- Use BLOCKER only for likely bug, regression, data loss, security issue, broken UX, broken build, broken migration, or serious maintainability trap.
- If uncertain, mark as FOLLOW-UP and explain why.
