---
id: TASK-145
title: 'Fix java:S3776 in CachedProjectScanner.java at line 159'
status: Done
assignee: []
created_date: '2026-02-21 10:40'
updated_date: '2026-02-21 10:45'
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
Refactored `scanDirectory()` in `CachedProjectScanner.java` to reduce cognitive complexity from 32 to ~8 by extracting three helper methods:
- `isScanTerminated(directory, depth)`: encapsulates all four early-exit guard clauses (cancelled, max depth, max files, max dirs) → complexity ~4
- `processChildrenInParallel(directory, collectedFiles, children, depth)`: handles the parallel batch submission and latch coordination → complexity ~9
- `createBatches(children, batchSize)` (private static): creates sublists from a VirtualFile array
- `awaitBatchCompletion(directory, latch)`: awaits the CountDownLatch with timeout/interrupt handling

New test file created: `src/test/java/com/devoxx/genie/service/analyzer/util/CachedProjectScannerTest.java` with 10 tests covering:
- empty/null children
- single and multiple files
- cache hit and cache miss (timestamp change)
- subdirectory recursion
- large directory (25 files → parallel batch path)
- gitignore exclusion

All 10 tests pass. No new SonarQube-relevant patterns introduced.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` (Cognitive Complexity) in `CachedProjectScanner.java` at line 159.\n\n### Problem\nThe `scanDirectory()` method had a cognitive complexity of 32 (limit: 15), caused by deeply nested conditionals, loops, lambdas, and try/catch blocks all within one method.\n\n### Solution\nExtracted four focused helper methods, reducing `scanDirectory()` complexity to ~8:\n\n1. **`isScanTerminated(directory, depth)`** — encapsulates all four early-exit guard checks (scan cancelled, max depth exceeded, max files exceeded, max directories exceeded). Complexity: ~4.\n\n2. **`processChildrenInParallel(directory, collectedFiles, children, depth)`** — handles splitting children into batches, submitting them to `SCAN_EXECUTOR`, and waiting for completion. Complexity: ~9.\n\n3. **`createBatches(children, batchSize)`** (private static) — pure utility that partitions a `VirtualFile[]` into fixed-size sublists.\n\n4. **`awaitBatchCompletion(directory, latch)`** — awaits a `CountDownLatch` with a 15-second timeout, cancelling the scan on timeout or thread interruption.\n\n### Files Changed\n- `src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java` — refactored\n- `src/test/java/com/devoxx/genie/service/analyzer/util/CachedProjectScannerTest.java` — new, 10 tests\n\n### Tests\nAll 10 new tests pass covering: empty/null children, single/multiple files, cache hit, cache miss on timestamp change, subdirectory recursion, large-directory parallel batch path, and gitignore exclusion.
<!-- SECTION:FINAL_SUMMARY:END -->
