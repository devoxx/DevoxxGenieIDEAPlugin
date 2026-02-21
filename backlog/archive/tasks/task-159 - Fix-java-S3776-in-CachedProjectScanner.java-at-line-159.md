---
id: TASK-159
title: 'Fix java:S3776 in CachedProjectScanner.java at line 159'
status: Done
assignee: []
created_date: '2026-02-21 11:13'
updated_date: '2026-02-21 11:19'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`
- **Line:** 159
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 159 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`.
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
Refactoring was already applied to the main branch file. The original `scanDirectory` method had cognitive complexity of 32 (deeply nested conditionals, parallel processing, gitignore checks, depth/count limit checks all inline).

The method was split into focused helpers:
- `isScanTerminated(directory, depth)` — extracts cancellation/depth/limit guard logic
- `processChild(child, collectedFiles, depth)` — handles individual file or subdir
- `processChildrenInParallel(directory, collectedFiles, children, depth)` — batched parallel processing
- `createBatches(children, batchSize)` — static batch partitioning utility
- `awaitBatchCompletion(directory, latch)` — latch await with timeout/interrupt handling

The refactored `scanDirectory` at line 159 now has cognitive complexity ~7, well under the 15 limit.

Test file `CachedProjectScannerTest.java` exists with 10 tests covering: empty/null children, single file, multiple files, caching behavior, timestamp-based cache invalidation, subdirectory recursion, parallel batching, and gitignore filtering. All 10 tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` (Cognitive Complexity) in `CachedProjectScanner.java` at line 159.\n\n### Problem\nThe `scanDirectory` method had a cognitive complexity of 32 (limit: 15). It combined: cancellation/depth/count guard checks, gitignore filtering, null-check on children, a branching threshold between sequential and parallel processing, and inline batch+latch logic.\n\n### Solution\nThe method was decomposed into five focused private helpers:\n\n| Helper | Responsibility |\n|---|---|\n| `isScanTerminated(directory, depth)` | Guard: cancelled flag, max depth, max files, max dirs |\n| `processChild(child, collectedFiles, depth)` | Per-child: gitignore check, recurse or collect |\n| `processChildrenInParallel(...)` | Batch children and dispatch to thread pool |\n| `createBatches(children, batchSize)` | Partition array into sub-lists |\n| `awaitBatchCompletion(directory, latch)` | Await latch with timeout/interrupt handling |\n\nThe refactored `scanDirectory` at line 159 now has cognitive complexity ~7.\n\n### Tests\n`CachedProjectScannerTest.java` covers 10 scenarios: empty/null children, single file, multiple files, cache hit, cache invalidation on timestamp change, subdirectory recursion, large-directory parallel batching, gitignore exclusion. All 10 tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
