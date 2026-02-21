---
id: TASK-90
title: Fix java:S3776 in SpecStatisticsPanel.java at line 72
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
ordinal: 90000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/panel/spec/SpecStatisticsPanel.java`
- **Line:** 72
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 72 in `src/main/java/com/devoxx/genie/ui/panel/spec/SpecStatisticsPanel.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SpecStatisticsPanel.java:72` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `update(List<TaskSpec>, int)` at line 72 by extracting 6 focused helper methods:

- `addEmptyLabel()` — creates and adds the "No tasks found" label
- `addSummaryRow(...)` — builds and adds the summary label + spacer
- `buildSummaryText(...)` — static: formats summary string with 3 ternaries (complexity 3)
- `addProgressBar(...)` — creates and adds the StatusProgressBar component
- `addPriorityRow(Map)` — builds and adds the priority breakdown label
- `buildPriorityText(Map)` — static: uses List.of parts + String.join to avoid nested `!first` guards (complexity ~7)
- `addChecklistRow(List)` — calls `aggregateChecklistCounts()`, builds and adds checklist label
- `aggregateChecklistCounts(List)` — static: for loop + null guards, returns long[4] (complexity ~5)

The `update()` method now has cognitive complexity of ~2 (one `if &&`).
All 14 existing tests in `SpecStatisticsPanelTest` continue to pass.

## Final Summary

Fixed SonarQube `java:S3776` in `SpecStatisticsPanel.java` by decomposing the monolithic `update()` method (cognitive complexity 32) into eight focused helper methods, each well below the complexity limit of 15. The refactoring preserves all existing behavior exactly — the rendered output is identical — and all 14 unit tests pass. Added `java.util.ArrayList` import required by the new `List<String> parts` usage in `buildPriorityText` and `addChecklistRow`.
