---
id: TASK-143
title: 'Fix java:S3776 in SpecContextBuilder.java at line 19'
status: Done
assignee: []
created_date: '2026-02-21 09:49'
updated_date: '2026-02-21 10:39'
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
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java`
- **Line:** 19
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 44 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 19 in `src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 44 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `SpecContextBuilder.java:19` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `buildContext` in `SpecContextBuilder.java` to reduce cognitive complexity from 44 to well below 15.

**Changes made:**
- File: `src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java`
- Added `java.util.List` and `org.jetbrains.annotations.Nullable` imports
- Extracted 7 private helper methods from `buildContext`:
  - `appendField` – appends a label+value when value is non-null
  - `appendNonEmptyField` – appends label+value when non-null and non-empty
  - `appendJoinedList` – appends a comma-joined list field (Assignees, Labels)
  - `appendSection` – appends a `## Header\n content` block for text fields
  - `appendBulletList` – appends a `## Header\n- item` block for string lists
  - `appendAcceptanceCriteria` – renders AC with checkbox markers
  - `appendDefinitionOfDone` – renders DoD with checkbox markers
- `buildContext` itself now has zero branching logic — it's a flat sequence of helper calls
- `buildCliInstruction` and `buildAgentInstruction` were NOT changed (they were not flagged)

**All 16 tests in `SpecContextBuilderTest` pass** — no behaviour changes.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resolved `java:S3776` in `SpecContextBuilder.java:19` by refactoring `buildContext` to extract 7 private helper methods, reducing its cognitive complexity from 44 to 0 (all control flow moved to helpers, each of which is well under 15).\n\n**Helper methods extracted:**\n- `appendField(sb, label, value)` – null-guarded single-field append\n- `appendNonEmptyField(sb, label, value)` – null+empty-guarded single-field append (Milestone)\n- `appendJoinedList(sb, label, list)` – comma-joined list line (Assignees, Labels)\n- `appendSection(sb, header, content)` – `## Header\\ncontent` block (Description, Plan, Notes, Summary)\n- `appendBulletList(sb, header, items)` – `## Header\\n- item` block (Dependencies, References, Documentation)\n- `appendAcceptanceCriteria(sb, criteria)` – checkbox-format AC list\n- `appendDefinitionOfDone(sb, items)` – checkbox-format DoD list\n\n`buildContext` is now a flat sequence of 16 helper calls with no branching, loops, or ternary operators.\n\nAll 16 pre-existing tests in `SpecContextBuilderTest` pass without modification — full coverage of all code paths including null/empty edge cases for every field."}
<!-- SECTION:FINAL_SUMMARY:END -->
