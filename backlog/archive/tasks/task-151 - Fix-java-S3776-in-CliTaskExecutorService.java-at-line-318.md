---
id: TASK-151
title: 'Fix java:S3776 in CliTaskExecutorService.java at line 318'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 10:57'
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
- **File:** `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`
- **Line:** 318
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 34 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 318 in `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 34 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `CliTaskExecutorService.java:318` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactoring was already present in the working tree (staged changes). The original createStreamReader method at line 318 contained a large anonymous lambda with heavily nested control flow (cognitive complexity 34). The fix extracted the logic into two new private methods: runStreamReader (loop + IO handling) and processStreamLine (per-line logic). Tests in CliTaskExecutorServiceTest already cover the new methods including processStreamLine_withLineCollector_addsLine, processStreamLine_stdout_dispatchesToPrintOutput, processStreamLine_stderr_dispatchesToPrintError etc. All tests pass (BUILD SUCCESSFUL).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Fix java:S3776 in CliTaskExecutorService.java at line 318

### Problem
The `createStreamReader` method at line 318 had a cognitive complexity of 34 (limit is 15) due to a large anonymous lambda containing multiple nested conditions and control flow structures.

### Solution
The lambda body was extracted into two focused private methods:

1. **`runStreamReader`** — handles the IO loop: opens a `BufferedReader` on stdout or stderr, iterates lines, and delegates each line to `processStreamLine`. Catches `IOException` for normal stream close.

2. **`processStreamLine`** — handles a single line: optionally collects it, logs it at appropriate intervals, conditionally parses Claude stream-JSON events, and dispatches display to the console via `invokeLater`.

`createStreamReader` itself now just creates a named daemon `Thread` pointing at `runStreamReader`, keeping it very simple.

### Files Modified
- `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java` — extracted `runStreamReader` and `processStreamLine` from the lambda in `createStreamReader`

### Test Coverage
`CliTaskExecutorServiceTest` already contains tests for the new methods:
- `processStreamLine_withLineCollector_addsLine`
- `processStreamLine_withNullCollector_doesNotThrow`
- `processStreamLine_stdout_dispatchesToPrintOutput`
- `processStreamLine_stderr_dispatchesToPrintError`

All tests pass (BUILD SUCCESSFUL).
<!-- SECTION:FINAL_SUMMARY:END -->
