Review the current branch against its fork point from a parent branch explicitly selected by the user.

Do not modify production code.

Act as four independent reviewers, then run a final PR-review judge pass to consolidate and arbitrate findings.

## Branch/range detection (required, user-selected base)

Determine the review range **before** reviewing code:

1. List recent branch candidates and ask user to choose:
   - Include both:
     - local branches except the currently checked-out branch
     - remote-tracking branches (`refs/remotes/*`) except symbolic refs like `*/HEAD`
   - Exclude the current branch from the picker in all forms:
     - local current branch name
     - its upstream/tracking ref (if any)
     - remote-tracking refs whose short branch name equals the current local branch name
   - Include remote-only branches as valid candidates.
   - If a local and remote branch represent the same name, keep one display row but preserve the fully-qualified ref that will be used.
   - Sort by most recently updated tip commit timestamp (descending), regardless of local/remote.
   - Use paginated candidate loading:
     - first page: 5 candidates
     - each additional page: +10 candidates
     - continue until all candidates are shown
   - Add a picker option at the end of each non-final page:
     - label: `More...`
     - value: `__MORE__`
   - If user selects `More...`, show the next page and repeat selection.
   - When all candidates are already shown, do not include `More...`.
   - Render each branch option label in aligned columns:
     - `<display-name-padded> | <short-tip-sha>`
     - Do not use markdown formatting in option labels (no `**`, `_`, backticks, or HTML), because picker options render plain text.
     - Emphasize branch name by position and separator only: branch first, then ` | `, then hash.
     - Compute max branch-name length in the currently shown page and right-pad names so hashes align vertically.
   - Use an interactive picker (single-select) to choose the base branch:
     - question: "Which branch should be used as review base?"
     - each option label must include: `<display-name-padded> | <short-tip-sha>`
     - each option value must map to the exact fully-qualified `<ref>`
     - reserve `__MORE__` value exclusively for pagination
     - do not include an "Other" option or any free-text fallback option in the picker
     - allowed actions are only: pick a listed branch, pick `More...`, or cancel/dismiss
   - Do not accept free-text when picker options are available.
   - Do not auto-select a branch.
   - If user dismisses or does not choose a branch, stop and report blocker.

2. Resolve selected parent branch:
   - Use exactly the branch/ref returned by the interactive picker as `<resolved-parent-branch>`.
   - Accept either short branch name or fully-qualified ref from the displayed list.
   - Validate that selected ref exists in local refs or remote-tracking refs.
   - If not found, stop and report exact blocker.

3. Compute fork point:
   - `FORK_POINT=$(git merge-base HEAD <resolved-parent-branch>)`
   - If fork point cannot be determined, stop and report blocker.

4. Review only this range:
   - Commits: `FORK_POINT..HEAD`
   - Diff: `git diff <FORK_POINT>..HEAD`

5. Report base-resolution evidence:
   - Selected parent branch
   - Fork point SHA
   - Reviewed range (`<FORK_POINT>..HEAD`)
   - Branch list shown to user
   - User-selected base branch value

6. Mandatory checkpoint before code inspection:
   - Print a "Base resolution checkpoint" block in chat with:
     - selected parent branch
     - fork point SHA
     - reviewed range (`<FORK_POINT>..HEAD`)
     - user-selected base confirmation
   - Do not inspect specific changed files or produce findings before this checkpoint is printed.
   - Ask for explicit confirmation: "Proceed with this base? (yes/no)".
   - If the answer is "no", stop and report blocker.

7. Never use `HEAD^..HEAD` for this command unless explicitly asked.

## Reviewer 1: Correctness
Check for bugs, regressions, edge cases, nullability, lifecycle issues, coroutine/Flow issues, Room/database migration problems, API/model mapping mistakes, date/time/locale bugs.

## Reviewer 2: Architecture and scalability
Check for layer violations, domain/data/UI model leakage, duplicated logic, unclear ownership, excessive coupling, abstractions that will not scale, and whether the design fits the existing architecture.

## Reviewer 3: Debuggability and operations
Check error handling, logging, analytics/events if relevant, silent failures, swallowed exceptions, vague failure states, hard-to-debug state transitions, and missing diagnostics.

## Reviewer 4: Tests
Check changed behavior without tests, weak tests, missing regression tests, missing mapper/use-case tests, database migration tests, coroutine timing/flakiness, and tests that should be local unit tests rather than instrumentation tests.

## Final Judge (required)
You are the final PR review judge.

Inputs:
- correctness review
- architecture review
- debuggability review
- tests review
- the branch diff (`<FORK_POINT>..HEAD`)

Tasks:
1. Deduplicate findings.
2. Separate must-fix from nice-to-have.
3. Identify any disagreement between reviewers.
4. Produce a merge-blocking checklist.
5. Produce a small safe fix plan.

Judge behavior:
- Do not modify files.
- Be conservative: mark merge-blocking only for likely bugs, regressions, data corruption, broken UX, broken build/migration, security issues, or serious maintenance debt.

# Output

Write the review to:

`docs/ai-review/final-release-fix-review.md`

Use this structure:

# Release Branch Review

## Base resolution
- Resolved parent branch: `<branch>`
- Fork point SHA: `<sha>`
- Reviewed range: `<sha>..HEAD`
- Branch list shown: `<yes|no>`
- User selected branch: `<branch>`

## Base resolution checkpoint
- Proceed confirmed: `<yes|no>`
- If `no`, include exact blocker and stop.

## Intended change

## Correctness findings

## Architecture and scalability findings

## Debuggability findings

## Test findings

## Final judge consolidation

### Deduplicated must-fix

### Deduplicated nice-to-have

### Reviewer disagreements

## Consolidated merge decision
Choose one:
- Safe to merge
- Safe to merge after small fixes
- Not safe to merge yet

## Merge-blocking checklist

## Small safe fix plan

## Should-fix checklist

## Follow-ups

## Human review checklist
List the 5-10 exact files/functions I should personally inspect.

# Rules

- Do not give vague style comments.
- Do not request broad refactors unless there is a concrete risk.
- Do not duplicate the same issue across sections.
- Use BLOCKER only for likely bug, regression, data loss, security issue, broken UX, broken build, broken migration, or serious maintainability trap.
- If uncertain, mark as FOLLOW-UP and explain the uncertainty.
