---
id: TASK-142
title: 'Fix java:S3776 in SpecFrontmatterParser.java at line 67'
status: Done
assignee: []
created_date: '2026-02-21 09:49'
updated_date: '2026-02-21 10:37'
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
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java`
- **Line:** 67
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 67 in `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `SpecFrontmatterParser.java:67` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Extracted the value-processing logic from `parseFrontmatter` into a new private method `parseFieldValue`. The nested if/else-if/else at nesting level 2 was the main contributor to the high cognitive complexity. Moving it to a helper method reduced `parseFrontmatter` from complexity 19 to 14 (threshold: 15), resolving the SonarQube S3776 violation.

Files modified:
- `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java`: Extracted `parseFieldValue(key, value, builder)` method; replaced the 3-branch if/else-if/else block inside `if (colonIndex > 0)` with a single call `currentList = parseFieldValue(currentKey, value, builder)`.
- `src/test/java/com/devoxx/genie/service/spec/SpecFrontmatterParserTest.java`: Added `shouldHandleExplicitEmptyArrayInFrontmatter()` test to cover the previously untested `[]` (explicit empty array) branch in `parseFieldValue`.

All 20 tests in `SpecFrontmatterParserTest` pass. No new SonarQube issues introduced.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary

Fixed SonarQube `java:S3776` in `SpecFrontmatterParser.java:67` by reducing the cognitive complexity of `parseFrontmatter` from 19 to 14 (threshold: 15).

### Root Cause

The `parseFrontmatter` method contained a nested `if/else-if/else` block at nesting level 2 (inside the `for` loop and inside `if (colonIndex > 0)`), which contributed 5 complexity units (3 for the `if` at nesting 2, plus `else if` and `else`).

### Fix

Extracted the value-processing logic into a new private method `parseFieldValue(String key, String value, TaskSpec.TaskSpecBuilder builder)`:
- Returns a new `ArrayList` when the value is empty (start of a YAML list field)
- Calls `applyListField` and returns `null` for explicit empty arrays (`[]`)
- Calls `applyScalarField` and returns `null` for normal scalar values

The call site in `parseFrontmatter` becomes a single-line assignment: `currentList = parseFieldValue(currentKey, value, builder)`.

### Test Coverage

Added `shouldHandleExplicitEmptyArrayInFrontmatter()` test to cover the previously untested `[]` branch. All 20 tests in `SpecFrontmatterParserTest` pass.
<!-- SECTION:FINAL_SUMMARY:END -->
