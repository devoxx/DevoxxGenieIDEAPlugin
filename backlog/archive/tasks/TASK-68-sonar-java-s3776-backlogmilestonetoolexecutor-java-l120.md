---
id: TASK-68
title: Fix java:S3776 in BacklogMilestoneToolExecutor.java at line 120
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:30'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 68000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/BacklogMilestoneToolExecutor.java`
- **Line:** 120
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 120 in `src/main/java/com/devoxx/genie/service/agent/tool/BacklogMilestoneToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `BacklogMilestoneToolExecutor.java:120` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the task-update loop inside `renameMilestone` into a new private helper method `renameTaskMilestones(String from, String to)`.

**Before (complexity 16):**
- `renameMilestone` contained a nested `if (updateTasks)` block with a `for` loop and inner `if`, adding +1 (if) + +2 (for at nesting level 1) + +3 (if at nesting level 2) = 7 points from that block alone.

**After (complexity ~11):**
- Replaced the `if (updateTasks) { ... }` block with a ternary: `int updatedCount = updateTasks ? renameTaskMilestones(from, to) : 0;` (+1)
- Extracted logic into `renameTaskMilestones` which is a flat method with complexity 3.

## Final Summary

Fixed `java:S3776` in `BacklogMilestoneToolExecutor.java` at line 120 (`renameMilestone` method).

The method had a cognitive complexity of 16 (limit is 15). The fix extracts the task-milestone-update loop into a private helper method `renameTaskMilestones(String from, String to)`, reducing the nesting depth in `renameMilestone`. The refactoring preserves identical behavior: when `updateTasks` is true, it iterates all task specs and renames matching milestones, returning the count of updated tasks. No new issues introduced; only `BacklogMilestoneToolExecutor.java` was modified.
