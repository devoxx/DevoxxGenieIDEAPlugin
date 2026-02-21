---
id: TASK-95
title: Fix java:S3776 in SpecBrowserPanel.java at line 720
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:50'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 95000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java`
- **Line:** 720
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 720 in `src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SpecBrowserPanel.java:720` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the body of the `for (Map.Entry<String, List<TaskSpec>> entry : grouped.entrySet())` loop
in `refreshTree()` into a new private helper method `addStatusGroupToTree(Map.Entry<String, List<TaskSpec>> entry)`.

This removed 4 complexity points from `refreshTree()`:
- The nested `if (statusSpecs.isEmpty())` that was at nesting level 1 (+2)
- The doubly-nested `for (TaskSpec spec : statusSpecs)` loop (+2)

New complexity of `refreshTree()`: 15 (was 19).
The new helper `addStatusGroupToTree()` has complexity 2.
No new SonarQube issues introduced.

## Final Summary

**File modified:** `src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java`

**Change:** Extracted the inner loop body from `refreshTree()` into a new private method `addStatusGroupToTree(Map.Entry<String, List<TaskSpec>> entry)`.

The `for (Map.Entry...)` loop body previously contained:
1. `if (statusSpecs.isEmpty()) continue;` — contributed +2 (nested if) to refreshTree's complexity
2. `for (TaskSpec spec : statusSpecs)` — contributed +2 (doubly-nested for) to refreshTree's complexity

By moving this logic into a dedicated helper method, `refreshTree()` drops from complexity 19 to exactly 15 (the allowed maximum). The new helper has its own complexity of just 2.

All existing tests pass (SpecStatisticsPanelTest and others in the panel package).
