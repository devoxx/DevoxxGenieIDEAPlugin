---
id: TASK-141
title: 'Fix java:S3776 in SpecStatisticsPanel.java at line 72'
status: Done
assignee: []
created_date: '2026-02-21 09:48'
updated_date: '2026-02-21 10:13'
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
- **File:** `src/main/java/com/devoxx/genie/ui/panel/spec/SpecStatisticsPanel.java`
- **Line:** 72
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 72 in `src/main/java/com/devoxx/genie/ui/panel/spec/SpecStatisticsPanel.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 32 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `SpecStatisticsPanel.java:72` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `update(List<TaskSpec>, int)` in `SpecStatisticsPanel.java` to reduce cognitive complexity from 32 to ~3 by extracting six private helper methods:
- `addEmptyState()` — handles empty panel state
- `addSummaryRow(...)` — builds and adds the summary label (Row 1)
- `addProgressBarRow(...)` — adds the status progress bar (Row 2)
- `addPriorityRow(...)` — delegates to `buildPriorityText()` and adds label (Row 3)
- `buildPriorityText(...)` (static) — pure text builder for priority breakdown (~13 CC)
- `addChecklistRow(...)` — aggregates AC/DoD counts and delegates to `buildChecklistText()` (~7 CC)
- `buildChecklistText(...)` (static) — pure text builder for checklist summary (~4 CC)

All existing 14 tests in `SpecStatisticsPanelTest` pass. No new tests needed as coverage was already comprehensive. No new SonarQube issues introduced — all extracted helpers stay well under the 15 CC limit. AC #4 satisfied by existing test coverage (no gaps identified).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resolved `java:S3776` in `SpecStatisticsPanel.java:72` by decomposing the monolithic `update(List<TaskSpec>, int)` method (cognitive complexity 32) into focused private helpers.\n\n**File modified:** `src/main/java/com/devoxx/genie/ui/panel/spec/SpecStatisticsPanel.java`\n\n**Approach:** Extracted six helper methods so the main `update` method becomes a thin dispatcher (CC ≈ 3). Each helper stays well under the 15 CC threshold:\n- `addEmptyState()` — CC 0\n- `addSummaryRow(...)` — CC 2 (two ternaries)\n- `addProgressBarRow(...)` — CC 0\n- `addPriorityRow(...)` — CC 0\n- `buildPriorityText(Map)` — CC ~13 (two loops with nested conditionals)\n- `addChecklistRow(List)` — CC ~7 (loop + two nullchecks + early return)\n- `buildChecklistText(...)` — CC ~4 (two ifs + one nested if)\n\n**Tests:** All 14 pre-existing tests in `SpecStatisticsPanelTest` pass. No new tests needed — existing coverage was already comprehensive. No new SonarQube issues introduced.
<!-- SECTION:FINAL_SUMMARY:END -->
