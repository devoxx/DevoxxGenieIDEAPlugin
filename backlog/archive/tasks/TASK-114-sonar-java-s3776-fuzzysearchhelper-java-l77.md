---
id: TASK-114
title: Fix java:S3776 in FuzzySearchHelper.java at line 77
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:58'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 114000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/spec/FuzzySearchHelper.java`
- **Line:** 77
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 77 in `src/main/java/com/devoxx/genie/service/spec/FuzzySearchHelper.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `FuzzySearchHelper.java:77` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the inner loop body from `tokenMatchScore` into a new private helper method
`scoreTokenAgainstWords(String token, String[] textWords)`.

**Root cause:** `tokenMatchScore` had cognitive complexity 17 (limit is 15) due to a
deeply nested `for` loop (depth 2) containing `if/else if/else` at depth 3.

**Fix:** Moved the inner loop (lines 99–109) into `scoreTokenAgainstWords`. This reduces
`tokenMatchScore` complexity from 17 → 8, with the new helper at complexity 5.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/spec/FuzzySearchHelper.java`

**Tests:** All 40 tests in `FuzzySearchHelperTest` pass without modification.

## Final Summary

Resolved SonarQube `java:S3776` (Cognitive Complexity) in `FuzzySearchHelper.java:77`
by extracting the per-word scoring loop from `tokenMatchScore` into a new private helper
`scoreTokenAgainstWords`. No test changes were needed; all 40 existing tests continue to
pass. The helper has its own complexity of 5, well within the allowed limit.
