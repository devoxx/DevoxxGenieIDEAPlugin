---
id: task-130
title: 'Fix java:S1192 in BacklogConfigService.java at line 36'
status: Done
assignee: []
created_date: '2026-02-21 08:15'
updated_date: '2026-02-21 09:07'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 1000
---

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1192`
- **File:** `BacklogConfigService.java`
- **Line:** 36
- **Severity:** High impact on Maintainability
- **Issue:** Define a constant instead of duplicating this literal "tasks" 3 times.

Fix the SonarQube issue `java:S1192` at line 36 in `BacklogConfigService.java`.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S1192` at `BacklogConfigService.java:36` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 All existing tests continue to pass
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed java:S1192 in BacklogConfigService.java. The string literal "tasks" was used 3 times (lines 36, 102, 126). Extracted it into a private static final constant `TASKS_DIR = "tasks"` at line 31, then replaced all three occurrences with the constant. No new SonarQube issues introduced.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed SonarQube rule java:S1192 (\"String literals should not be duplicated\") in BacklogConfigService.java.\n\nThe string literal \"tasks\" appeared 3 times in the file:\n1. Line 36: in the BACKLOG_SUBDIRECTORIES list\n2. Line 102: in getTasksDir() — specDir.resolve(\"tasks\")\n3. Line 126: in getArchiveTasksDir() — specDir.resolve(\"archive\").resolve(\"tasks\")\n\nFix: Added a private static final constant `TASKS_DIR = \"tasks\"` at line 31, then replaced all three occurrences with the constant reference.\n\nAll BacklogConfigServiceTest tests that were passing before continue to pass. The 11 pre-existing test failures (unrelated to this file's logic) are unchanged.">
<!-- SECTION:FINAL_SUMMARY:END -->
