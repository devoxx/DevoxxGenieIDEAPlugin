---
id: TASK-101
title: Fix java:S3776 in BacklogTaskToolExecutor.java at line 247
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:46'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 101000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java`
- **Line:** 247
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 247 in `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `BacklogTaskToolExecutor.java:247` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `editTask()` method (line 247) by extracting five focused helper methods:
- `applyScalarUpdates(TaskSpec, String)` – title, description, status, priority, milestone, finalSummary
- `applyListUpdates(TaskSpec, String)` – assignees, labels, dependencies
- `applyAcceptanceCriteriaChanges(TaskSpec, String)` – AC add/check/uncheck
- `setAcceptanceCriterionChecked(TaskSpec, List<Integer>, boolean)` – shared helper for check/uncheck
- `applyPlanChanges(TaskSpec, String)` – plan clear/set/append
- `applyNotesChanges(TaskSpec, String)` – notes clear/set/append

The refactored `editTask()` now has a cognitive complexity of ~3 (two guard-clause ifs + 5 method calls). All 16 existing `BacklogTaskToolExecutorTest` tests continue to pass.

## Final Summary

Resolved java:S3776 in `BacklogTaskToolExecutor.java` by decomposing the 103-line `editTask()` method (cognitive complexity 49) into six private helper methods. Each helper handles one cohesive concern, keeping all sub-methods well within the 15-complexity limit. No logic was changed — only structure. All tests pass.
