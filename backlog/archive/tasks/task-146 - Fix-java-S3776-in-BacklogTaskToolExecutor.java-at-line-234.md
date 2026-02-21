---
id: TASK-146
title: 'Fix java:S3776 in BacklogTaskToolExecutor.java at line 234'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 10:48'
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
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java`
- **Line:** 234
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 234 in `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `BacklogTaskToolExecutor.java:234` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactoring was already staged (git index) as part of the sonarlint-fixes branch. The editTask method at line 234 was split into 5 private helper methods to reduce cognitive complexity from 49 to well below 15:

- `applyScalarUpdates` - title, description, status, priority, milestone, finalSummary
- `applyListUpdates` - assignees, labels, dependencies
- `applyAcceptanceCriteriaUpdates` - add/check/uncheck AC items
- `applyPlanUpdates` - planSet, planAppend, planClear (using early-return pattern)
- `applyNotesUpdates` - notesSet, notesAppend, notesClear (using early-return pattern)

Added 11 new tests to BacklogTaskToolExecutorTest.java covering previously untested paths:
- editTask_listFieldUpdates
- editTask_notesSetAndAppend
- editTask_notesClear
- editTask_acceptanceCriteriaUncheck
- archiveDoneTasks_noCompleted_returnsNoTasksMessage
- archiveDoneTasks_withCompleted_returnsCount
- unarchiveTask_missingId_returnsError
- unarchiveTask_success
- listArchivedTasks_empty_returnsNoArchivedMessage
- listArchivedTasks_withResults_returnsFormattedList

All 27 tests pass (BUILD SUCCESSFUL).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nResolved `java:S3776` (Cognitive Complexity) in `BacklogTaskToolExecutor.java` at line 234 (`editTask` method).\n\n### What was done\n\nThe `editTask` method had a cognitive complexity of 49 (limit: 15). It was refactored by extracting 5 private helper methods:\n\n- **`applyScalarUpdates`** — handles title, description, status, priority, milestone, finalSummary\n- **`applyListUpdates`** — handles assignees, labels, dependencies list replacements\n- **`applyAcceptanceCriteriaUpdates`** — handles add/check/uncheck of AC items\n- **`applyPlanUpdates`** — handles planSet/planAppend/planClear with early-return\n- **`applyNotesUpdates`** — handles notesSet/notesAppend/notesClear with early-return\n\nThe refactoring was already staged in the git index as part of this branch's prior work.\n\n### Tests added\n\n11 new tests added to `BacklogTaskToolExecutorTest.java` to cover previously untested code paths in the extracted helper methods and unarchive/listArchived operations. All 27 tests pass.\n\n### Files changed\n- `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java` (already staged)\n- `src/test/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutorTest.java` (new tests added)
<!-- SECTION:FINAL_SUMMARY:END -->
