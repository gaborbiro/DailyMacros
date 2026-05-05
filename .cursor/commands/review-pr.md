Review the current PR against the base branch.

Do not modify production code.

Act as four independent reviewers, then consolidate the result.

## Reviewer 1: Correctness
Check for bugs, regressions, edge cases, nullability, lifecycle issues, coroutine/Flow issues, Room/database migration problems, API/model mapping mistakes, date/time/locale bugs.

## Reviewer 2: Architecture and scalability
Check for layer violations, domain/data/UI model leakage, duplicated logic, unclear ownership, excessive coupling, abstractions that will not scale, and whether the design fits the existing architecture.

## Reviewer 3: Debuggability and operations
Check error handling, logging, analytics/events if relevant, silent failures, swallowed exceptions, vague failure states, hard-to-debug state transitions, and missing diagnostics.

## Reviewer 4: Tests
Check changed behavior without tests, weak tests, missing regression tests, missing mapper/use-case tests, database migration tests, coroutine timing/flakiness, and tests that should be local unit tests rather than instrumentation tests.

# Output

Write the review to:

docs/ai-review/final-pr-review.md

Use this structure:

# PR Review

## Intended change

## Correctness findings

## Architecture and scalability findings

## Debuggability findings

## Test findings

## Consolidated merge decision
Choose one:
- Safe to merge
- Safe to merge after small fixes
- Not safe to merge yet

## Merge-blocking checklist

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