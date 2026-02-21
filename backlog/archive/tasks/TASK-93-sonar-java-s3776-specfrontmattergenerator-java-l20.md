---
id: TASK-93
title: Fix java:S3776 in SpecFrontmatterGenerator.java at line 20
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
ordinal: 93000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 22 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterGenerator.java`
- **Line:** 20
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 22 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 20 in `src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterGenerator.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SpecFrontmatterGenerator.java:20` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `generate()` method in `SpecFrontmatterGenerator.java` by extracting 5 private helper methods:
- `appendDescription(sb, description)` — appends raw description text
- `appendAcceptanceCriteria(sb, criteria)` — renders AC checklist section
- `appendDefinitionOfDone(sb, items)` — renders DoD checklist section
- `appendSection(sb, header, content)` — generic header+content section (used for Implementation Plan and Notes)
- `appendFinalSummary(sb, content)` — final summary section (single trailing newline)

The `generate()` method cognitive complexity was reduced from 22 to effectively 0 (no control flow in the method body). Each helper method has complexity ≤ 3. All 25 existing tests pass.

## Final Summary

Fixed SonarQube java:S3776 in `SpecFrontmatterGenerator.java` at line 20. The `generate()` method had cognitive complexity of 22 (above the allowed 15) due to multiple compound null-and-empty checks (`!= null && !isEmpty()`) and nested for-loops with ternary expressions. Extracted 5 focused private helper methods (`appendDescription`, `appendAcceptanceCriteria`, `appendDefinitionOfDone`, `appendSection`, `appendFinalSummary`) which each handle one section of the output. The public API is unchanged and all 25 unit tests in `SpecFrontmatterGeneratorTest` continue to pass.
