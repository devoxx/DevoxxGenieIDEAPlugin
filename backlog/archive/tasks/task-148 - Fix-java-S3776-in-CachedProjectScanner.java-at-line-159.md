---
id: TASK-148
title: 'Fix java:S3776 in CachedProjectScanner.java at line 159'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 10:50'
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
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`
- **Line:** 159
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 159 in `src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `CachedProjectScanner.java:159` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactoring of scanDirectory() was already applied on branch sonarlint-fixes. The original method had cognitive complexity of 32 (limit 15). It was split into four focused helpers:

- `isScanTerminated(VirtualFile, int)` — consolidates all early-exit guards (cancelled flag, depth limit, file count, directory count)
- `processChildrenInParallel(VirtualFile, List, VirtualFile[], int)` — moves batch creation, latch management and async dispatch out of the main path
- `createBatches(VirtualFile[], int)` — pure utility to partition an array into sublists
- `awaitBatchCompletion(VirtualFile, CountDownLatch)` — encapsulates the latch.await() + interrupt handling

Modified file: src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java

Tests file already existed: src/test/java/com/devoxx/genie/service/analyzer/util/CachedProjectScannerTest.java (9 tests covering empty dir, null children, single/multi files, caching, timestamp change, recursion, large-dir parallel batch, and .gitignore filtering). All 9 tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nResolved `java:S3776` (Cognitive Complexity) in `CachedProjectScanner.java` at line 159.\n\n### Problem\nThe `scanDirectory()` method had a cognitive complexity of 32, exceeding the allowed threshold of 15. It contained nested conditionals for cancellation/limit checks, inline batch creation, latch management, and interrupt handling all in one place.\n\n### Solution\nExtracted four focused helper methods, each with a single responsibility:\n\n| Method | Responsibility |\n|---|---|\n| `isScanTerminated(VirtualFile, int)` | Early-exit guard (cancelled flag, depth, file count, directory count) |\n| `processChildrenInParallel(VirtualFile, List, VirtualFile[], int)` | Async batch dispatch and latch coordination |\n| `createBatches(VirtualFile[], int)` | Pure utility: partitions an array into fixed-size sublists |\n| `awaitBatchCompletion(VirtualFile, CountDownLatch)` | `latch.await()` with timeout + interrupt handling |\n\nAfter extraction, `scanDirectory()` is a ~30-line orchestrator with a cognitive complexity well below 15.\n\n### Files Modified\n- `src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`\n\n### Tests\n- `src/test/java/com/devoxx/genie/service/analyzer/util/CachedProjectScannerTest.java` — 9 tests, all passing (empty dir, null children, single/multi files, caching, timestamp invalidation, recursive subdirectory, large-dir parallel batching, `.gitignore` filtering).
<!-- SECTION:FINAL_SUMMARY:END -->
