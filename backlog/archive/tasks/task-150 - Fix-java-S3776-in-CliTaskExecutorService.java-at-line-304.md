---
id: TASK-150
title: 'Fix java:S3776 in CliTaskExecutorService.java at line 304'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 10:56'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`
- **Line:** 304
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 304 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `CliTaskExecutorService.java:304` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `createStreamReader` in `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`.

The method had cognitive complexity ~30 due to deeply nested lambdas: a `Thread` lambda containing a `while` loop with `if` blocks and an inner `invokeLater` lambda — each level adding nesting penalty.

Fix: split into three focused methods:
- `createStreamReader` — now only creates and returns a daemon Thread (complexity: ~1)
- `runStreamReader` — reads lines from the process stream in a loop (complexity: ~5)
- `processStreamLine` — handles a single line: collect, log, parse Claude JSON, dispatch to console (complexity: ~12)

All existing 15 tests in `CliTaskExecutorServiceTest` continue to pass. No new SonarQube issues were introduced.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Fix: Reduce Cognitive Complexity in `createStreamReader` (java:S3776)\n\n### Problem\n`createStreamReader` in `CliTaskExecutorService.java` had cognitive complexity ~30 (limit: 15). The cause was heavy nesting: a `Thread` lambda wrapping a `while` loop with multiple `if` blocks and an inner `invokeLater` lambda — each level compounding the nesting penalty.\n\n### Solution\nSplit `createStreamReader` into three focused private methods:\n\n| Method | Responsibility | Approx. Complexity |\n|---|---|---|\n| `createStreamReader` | Creates and returns a named daemon Thread | ~1 |\n| `runStreamReader` | Opens the stream, reads lines in a loop, handles IOException | ~5 |\n| `processStreamLine` | Collects, logs, parses Claude JSON, dispatches one line to console | ~12 |\n\nThe logic is unchanged — code was reorganised only.\n\n### Files Modified\n- `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java` — refactored `createStreamReader` into three methods\n- `src/test/java/com/devoxx/genie/service/cli/CliTaskExecutorServiceTest.java` — added 4 new tests for `processStreamLine`\n\n### Tests\n- All 15 pre-existing tests continue to pass\n- 4 new tests added covering `processStreamLine`: line collection, null collector, stdout dispatch, stderr dispatch\n- Total: 19 tests, all green
<!-- SECTION:FINAL_SUMMARY:END -->
