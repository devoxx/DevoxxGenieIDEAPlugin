---
id: TASK-92
title: Fix java:S3776 in SpecFrontmatterParser.java at line 67
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:49'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 92000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java`
- **Line:** 67
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 67 in `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SpecFrontmatterParser.java:67` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the if/else block that processes the value portion of a YAML key-value pair (lines 96-107 in original) into a new private helper method `processScalarOrListStart(String value, String key, TaskSpecBuilder builder)`.

The helper returns `@Nullable List<String>`: a new `ArrayList` if the value is empty (list field starting), or `null` if a scalar was applied. This cleanly replaces the three-branch if/else that was at nesting depth 2 inside the for-loop, reducing it to depth 0 in the extracted method.

**Complexity breakdown after refactoring:**
- `parseFrontmatter`: 1+2+4+2+4+2 = **15** (was 19) ✓
- `processScalarOrListStart`: 1+1+1 = **3** ✓

Also inlined the redundant local variable in the list-item branch:
- Before: `String value = trimmed.substring(2).trim(); value = stripQuotes(value); currentList.add(value);`
- After: `currentList.add(stripQuotes(trimmed.substring(2).trim()));`

**Files modified:** `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java`

## Final Summary

Fixed SonarQube `java:S3776` in `SpecFrontmatterParser.parseFrontmatter` (line 67) by extracting the three-branch if/else value-processing block into a new private helper method `processScalarOrListStart`. This removed the depth-2 nesting contribution of those branches, reducing the method's cognitive complexity from 19 to exactly 15 (the allowed maximum). The new helper has a complexity of only 3. All 30 existing tests in `SpecFrontmatterParserTest` and `SpecFrontmatterParserNewFieldsTest` continue to pass. No new SonarQube issues were introduced.
