---
id: TASK-31
title: Improve TaskDependencySorter branch coverage (91%/68% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 09:28'
updated_date: '2026-02-14 10:04'
labels:
  - testing
  - spec-service
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java
  - src/test/java/com/devoxx/genie/service/spec/TaskDependencySorterTest.java
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TaskDependencySorter has good instruction coverage (91%) but branch coverage is only 68%. It is a pure utility class with no IntelliJ dependencies — all methods are static graph algorithms.

Branch gaps to address:
- getUnsatisfiedDependencies() — 56% branch (13 of 30 missed). Needs tests for: external dependency handling, completed task dependency satisfaction, mixed internal/external deps, deps referencing non-existent tasks
- sort() — 80% branch (9 missed). Needs tests for: null dependency lists, empty completed task sets, single-node graphs, tiebreaking by ordinal
- extractNumber() — 50% branch (2 missed). Needs tests for non-numeric ID strings, empty strings, IDs without numbers
- lambda$sort$0 — 0% (warning lambda for unknown dependency). Needs test with dependency referencing unknown task ID
- lambda$sort$1 — 66% branch. Ordinal comparison tiebreaking edge cases
- lambda$sort$2/3 — 50% branch. Null-safe comparisons
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage stays at 90%+
- [x] #2 Branch coverage reaches 80%+
- [x] #3 Tests cover unsatisfied dependency edge cases
- [x] #4 Tests cover tiebreaking and ordinal sorting
- [x] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added 14 new tests to TaskDependencySorterTest covering:\n\ngetUnsatisfiedDependencies() branches:\n- null dependency list\n- empty dependency list\n- internal dep not completed (in selected but not completed)\n- external dep that is Done (satisfied)\n- unknown dep not in allSpecs\n- external dep In Progress (not Done)\n- mixed satisfied/unsatisfied deps\n- allSpecs entry with null ID\n\nsort() branches:\n- null dependency list on tasks\n- task with null ID\n- non-numeric IDs (extractNumber returns MAX_VALUE)\n- allSpecs with null ID entries\n- case-insensitive dependency matching\n- equal ordinal with different numeric IDs (tiebreaking)\n\nResults: Branch coverage 68% → 82%, instruction coverage stable at 94%, line coverage 99%."
<!-- SECTION:FINAL_SUMMARY:END -->
