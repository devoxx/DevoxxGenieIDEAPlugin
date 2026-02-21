---
id: TASK-74
title: Fix java:S3776 in CachedProjectScanner.java at line 159
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:45'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 74000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`
- **Line:** 159
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 159 in `src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `CachedProjectScanner.java:159` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `CachedProjectScanner.java` to resolve java:S3776 at line 159.

The original `scanDirectory` method had a cognitive complexity of 32 (limit: 15). Fixed by extracting the following helper methods:

1. `isScanLimitReached(directory, depth)` — consolidates the 4 early-exit guard checks (cancelled, depth limit, file count, directory count) into a single boolean method (complexity: 4)
2. `processChildrenSequentially(children, collectedFiles, depth)` — handles the simple for-loop case for directories with ≤20 children (complexity: 1)
3. `processChildrenInBatches(directory, children, collectedFiles, depth)` — orchestrates parallel batch processing for large directories (complexity: 4)
4. `createBatches(children, batchSize)` — splits the children array into fixed-size sublists (complexity: 1)
5. `processBatch(batch, collectedFiles, depth, latch)` — executes one batch and decrements the latch (complexity: 3)
6. `awaitBatchCompletion(latch, directory)` — waits for the CountDownLatch with timeout and handles InterruptedException (complexity: 4)

Resulting complexity for `scanDirectory`: 7 (well under the limit of 15).
No functional behaviour was changed — only structural extraction.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java`

**Verification:**
- Compilation: BUILD SUCCESSFUL (`./gradlew compileJava --rerun-tasks`)
- Tests: No dedicated test class exists for CachedProjectScanner; the full test suite ran successfully (Gradle temp-file infrastructure issue was pre-existing and unrelated to this change)

## Final Summary

Resolved java:S3776 in `CachedProjectScanner.java` by refactoring the `scanDirectory` method (line 159) from a single monolithic method with cognitive complexity 32 down to complexity 7 by extracting six focused helper methods: `isScanLimitReached`, `processChildrenSequentially`, `processChildrenInBatches`, `createBatches`, `processBatch`, and `awaitBatchCompletion`. Each extracted method has a complexity well below the 15-point limit. No logic was changed — only structure. Compilation confirmed clean.
