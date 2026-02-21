---
id: TASK-160
title: 'Fix java:S3776 in CliTaskExecutorService.java at line 304'
status: Done
assignee: []
created_date: '2026-02-21 11:13'
updated_date: '2026-02-21 11:22'
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
Fix was already applied in the working tree as part of earlier refactoring work (matching archived tasks task-150/task-151).

Changes in `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`:
- `createStreamReader()` was simplified to just create a Thread with a method reference.
- Complex lambda body extracted into new private method `runStreamReader()` (handles BufferedReader loop + catch).
- Inner loop body extracted into new private method `processStreamLine()` (handles collection, logging, JSON parsing, and console dispatch).

Each new method's cognitive complexity is well under 15:
- `createStreamReader`: ~2 (just a ternary + method ref)
- `runStreamReader`: ~3 (while loop + catch + ternary)
- `processStreamLine`: ~10 (several if-checks + lambda + ternary + if/else)

Tests in `src/test/java/com/devoxx/genie/service/cli/CliTaskExecutorServiceTest.java` cover the refactored `processStreamLine` method (3 new tests for collector behaviour, stdout dispatch, stderr dispatch). All 18 tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed SonarQube rule `java:S3776` (Cognitive Complexity) in `CliTaskExecutorService.java`.\n\n### Problem\nThe `createStreamReader()` method contained a large anonymous lambda body with deeply nested control flow (while loop, multiple if-checks, a nested invokeLater lambda, and a catch block), giving it a cognitive complexity of ~30 against the allowed maximum of 15.\n\n### Solution\nExtracted the fat lambda into two focused private methods:\n\n1. **`runStreamReader()`** — handles the `BufferedReader` loop and `IOException` catch. Cognitive complexity: ~3.\n2. **`processStreamLine()`** — processes a single line: adds it to the optional collector, logs sample lines, optionally parses Claude stream-JSON events, and dispatches the line to the console via `invokeLater`. Cognitive complexity: ~10.\n\n`createStreamReader()` now simply constructs a `Thread` with a method reference, reducing its complexity to ~2.\n\n### Files Changed\n- `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java` — refactored `createStreamReader` + two new private methods.\n- `src/test/java/com/devoxx/genie/service/cli/CliTaskExecutorServiceTest.java` — added 3 tests covering `processStreamLine` (line collector, stdout dispatch, stderr dispatch).\n\n### Test Results\nAll 18 tests in `CliTaskExecutorServiceTest` pass (BUILD SUCCESSFUL).
<!-- SECTION:FINAL_SUMMARY:END -->
