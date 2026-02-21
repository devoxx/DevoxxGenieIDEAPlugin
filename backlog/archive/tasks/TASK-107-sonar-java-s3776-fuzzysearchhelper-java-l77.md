---
id: TASK-107
title: Fix java:S3776 in FuzzySearchHelper.java at line 77
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:48'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 107000
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

Extracted the inner word-matching loop from `tokenMatchScore` into a new private helper method
`calculateBestWordScore(String token, String[] textWords)`. This removed the deeply nested
for-loop with if/else-if/else chain (which contributed 3+4+1+1 = 9 points of nesting complexity)
from `tokenMatchScore`, reducing its cognitive complexity well below the limit of 15.

No logic changes were made — only structural extraction.

## Final Summary

**Problem:** `tokenMatchScore` in `FuzzySearchHelper.java` had cognitive complexity 17 (limit: 15),
caused by a triply-nested structure: outer for-loop → if/else → inner for-loop → if/else-if/else.

**Fix:** Extracted lines 97–109 (the inner for-loop over `textWords` and its if/else-if/else chain)
into a new private method `calculateBestWordScore(String token, String[] textWords)`. The outer
method now calls this helper instead of inlining the logic.

**Result:**
- `tokenMatchScore` complexity reduced from 17 → ~9 (well under the limit of 15)
- `calculateBestWordScore` has its own modest complexity (~5) — within limits
- No logic changed; all 30+ existing tests pass (BUILD SUCCESSFUL)
- No new SonarQube issues introduced (helper is private, straightforward, single-purpose)

**File modified:** `src/main/java/com/devoxx/genie/service/spec/FuzzySearchHelper.java`
