---
id: task-131
title: 'Fix java:S1141 in BacklogConfigService.java at line 444'
status: Done
assignee: []
created_date: '2026-02-21 08:22'
updated_date: '2026-02-21 09:10'
labels:
  - sonarqube
  - java
dependencies: []
priority: medium
ordinal: 1000
---

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1141`
- **File:** `BacklogConfigService.java`
- **Line:** 444
- **Severity:** Medium impact on Maintainability
- **Issue:** Extract this nested try block into a separate method.

Fix the SonarQube issue `java:S1141` at line 444 in `BacklogConfigService.java`.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S1141` at `BacklogConfigService.java:444` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 All related tests continue to pass
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Extracted the inner try-catch (NumberFormatException) from `scanMaxId()` into a new private helper method `parseNumericSuffix(String id)`. The nested try structure in `scanMaxId` at line 438-445 was the S1141 violation. The helper returns the parsed integer or -1 if not a numeric suffix. No logic was changed — only the structure was refactored to eliminate nesting. File modified: `src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java`
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resolved java:S1141 (Nested try blocks should be avoided) in `BacklogConfigService.java`.\n\n**Root cause**: `scanMaxId()` had a `try-catch NumberFormatException` nested inside a `try-catch IOException`, violating S1141.\n\n**Fix**: Extracted the inner try-catch into a new private method `parseNumericSuffix(String id)` that:\n- Finds the last dash in the id\n- Attempts to parse the substring after it as an integer\n- Returns the parsed integer on success, or -1 on NumberFormatException or when no dash is present\n\nThe `scanMaxId` method was updated to call `parseNumericSuffix` and compare the result directly, preserving identical semantics (a return value of -1 is always less than the initial `max = 0`, so non-numeric IDs are correctly ignored).\n\n**No new SonarQube issues**: The helper method has a single, non-nested try-catch which is correct.\n\n**Tests**: All existing tests in `BacklogConfigServiceTest` (including `scanMaxId_handlesNonNumericSuffix`, `scanMaxId_handlesIdWithoutDash`, `scanMaxId_handlesQuotedId`, etc.) continue to pass with this purely structural refactor.\n\n**File modified**: `src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java`"]
<!-- SECTION:FINAL_SUMMARY:END -->
