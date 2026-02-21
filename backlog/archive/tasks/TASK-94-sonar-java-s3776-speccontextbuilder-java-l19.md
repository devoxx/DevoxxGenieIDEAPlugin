---
id: TASK-94
title: Fix java:S3776 in SpecContextBuilder.java at line 19
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
ordinal: 94000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 44 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java`
- **Line:** 19
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 44 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 19 in `src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SpecContextBuilder.java:19` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `SpecContextBuilder.buildContext()` which had a cognitive complexity of 44 (exceeding the allowed 15).

The original method contained all logic inline with 15+ nested if-statements and for-loops directly in the method body.

**Fix:** Extracted repeated patterns into private static helper methods:
- `appendField()` - appends a nullable string field
- `appendNonEmptyField()` - appends a non-empty nullable string field
- `appendJoinedList()` - appends a non-empty list joined with commas
- `appendTextSection()` - appends a markdown section for text content
- `appendStringListSection()` - appends a markdown section for a string list with bullet points
- `appendChecklistSection<T>()` - generic helper for checklist sections (Acceptance Criteria, Definition of Done)

Also added missing imports: `java.util.List`, `java.util.function.Function`, `java.util.function.Predicate`.

**Files modified:** `src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java`

## Final Summary

Fixed SonarQube rule `java:S3776` in `SpecContextBuilder.java` by decomposing the monolithic `buildContext()` method (cognitive complexity 44) into six focused private helper methods. Each helper handles one specific pattern of output (plain field, non-empty field, joined list, text section, string list section, generic checklist section), reducing the main method's complexity to well below the allowed threshold of 15. No new issues were introduced, and all existing spec-related tests continue to pass.
