---
id: TASK-102
title: Fix java:S3776 in CliTaskExecutorService.java at line 333
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:47'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 102000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`
- **Line:** 333
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 333 in `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `CliTaskExecutorService.java:333` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

The `createStreamReader` method (line 333) had a cognitive complexity of 30, far exceeding the limit of 15.

**Root cause:** The method created a `Thread` with a large lambda body containing:
- A nested `invokeLater` lambda with further conditionals
- A `try-with-resources` block with a `while` loop
- Multiple `if` statements and ternary operators, all deeply nested

**Fix:** Extracted the thread body into three focused helper methods:

1. **`streamLines(...)`** — Contains the stream-reading loop logic (reads lines, collects stderr, calls helpers). Cognitive complexity ~7.
2. **`logLineIfNeeded(...)`** — Handles the periodic logging condition (`lineCount <= 5 || lineCount % 50 == 0`). Complexity ~3.
3. **`printLineToConsole(...)`** — Dispatches a line to the EDT via `invokeLater`, prefixing with task ID when parallel. Complexity ~5.

The refactored `createStreamReader` now has complexity ~1 (single ternary for stream name selection).

**Files modified:**
- `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`

## Final Summary

Fixed `java:S3776` in `CliTaskExecutorService.createStreamReader()` by decomposing the 44-line monolithic thread lambda into three private helper methods: `streamLines()`, `logLineIfNeeded()`, and `printLineToConsole()`. The original method had a cognitive complexity of 30 due to deeply nested lambdas (thread body → invokeLater lambda), loops, and multiple conditionals. After extraction, `createStreamReader` has complexity ~1, while each helper stays well under 10. All 7 existing `CliTaskExecutorServiceTest` tests pass. The only build failures in the project are pre-existing unrelated errors in `FileSelectionPanelFactory.java`.
