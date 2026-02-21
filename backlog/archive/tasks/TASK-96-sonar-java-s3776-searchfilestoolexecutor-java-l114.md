---
id: TASK-96
title: Fix java:S3776 in SearchFilesToolExecutor.java at line 114
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:50'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 96000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/SearchFilesToolExecutor.java`
- **Line:** 114
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 114 in `src/main/java/com/devoxx/genie/service/agent/tool/SearchFilesToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SearchFilesToolExecutor.java:114` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

The `searchInDirectory` method had a cognitive complexity of 17 (exceeds the 15 limit) due to multiple nested
conditionals inside the `else` branch of a `for` loop:

- `if (isBinaryFile(child))` at nesting depth 2 → +3
- `if (fileMatcher != null && !fileMatcher.matches(...))` at nesting depth 2 → +3 + 1 (for `&&`) = +4
- Total contribution from the else block: 7 points

**Fix:** Extracted the file-eligibility check and delegation into a new private helper method `searchFileIfEligible`.
This reduces `searchInDirectory`'s complexity from 17 to ~10, while the new helper has complexity of only 3.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/agent/tool/SearchFilesToolExecutor.java`

## Final Summary

Reduced cognitive complexity of `searchInDirectory` from 17 to ~10 by extracting the `else` block body into a new
private method `searchFileIfEligible(VirtualFile, VirtualFile, Pattern, PathMatcher, StringBuilder, int[])`.

The helper method encapsulates two sequential guard clauses (skip binary files, skip files not matching the glob
pattern) and the delegation to `searchInFile`. This is a pure refactoring — no behaviour change, no new public API.

All 35 tests in `SearchFilesToolExecutorTest` pass after the change.
