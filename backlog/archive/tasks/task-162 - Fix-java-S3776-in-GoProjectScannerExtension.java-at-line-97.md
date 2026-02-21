---
id: TASK-162
title: 'Fix java:S3776 in GoProjectScannerExtension.java at line 97'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 11:28'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java`
- **Line:** 97
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 97 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `GoProjectScannerExtension.java:97` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The main project file `src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java` already had the S3776 fix applied (likely via a prior refactor commit). The worktree file referenced in the task description still had the old high-complexity version.

The fix was a method extraction: the inline logic inside `detectGoFrameworks` (reading go.sum and detecting frameworks) was extracted into a separate `detectGoFrameworksFromGoSum(VirtualFile, Map)` method. This brings the cognitive complexity of `detectGoFrameworks` from ~19 down to ~6 (well below the 15 threshold).

Created a new dedicated test file: `src/test/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtensionTest.java` with 12 tests covering:
- Non-Go project skipping (2 tests)
- Framework detection from go.sum: Echo, Gin, Gorilla, Fiber, Chi (5 tests)
- ORM detection: GORM (1 test)
- GraphQL detection: graphql-go, gqlgen (2 tests)
- No go.sum scenario (1 test)
- Multiple libraries combined (1 test)

All 12 tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\n**Issue:** `java:S3776` — `detectGoFrameworks` in `GoProjectScannerExtension.java:97` had cognitive complexity of ~19 (threshold: 15).\n\n**Root cause:** The method contained deeply nested logic: an outer `if (goSum != null)` block with an inner try-catch containing multiple chained `if/else-if` blocks for framework detection, followed by another set of `if/else-if` blocks for file-content scanning. Together this exceeded the allowed complexity.\n\n**Resolution:** The fix was already present in the main project source as a method extraction — `detectGoFrameworksFromGoSum(VirtualFile goSum, Map<String, Object> goInfo)` was extracted as a dedicated private method. This reduces `detectGoFrameworks` to cognitive complexity ~6 and `detectGoFrameworksFromGoSum` to ~8, both well under 15.\n\n**Tests added:** Created `GoProjectScannerExtensionTest.java` with 12 tests covering all branches of the refactored code — framework detection (Echo, Gin, Gorilla, Fiber, Chi), ORM detection (GORM), GraphQL detection (graphql-go, gqlgen), combined scenarios, and edge cases (no go.sum, non-Go project, null languages). All 12 tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
