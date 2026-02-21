---
id: TASK-137
title: 'Fix java:S3776 in TaskDependencySorter.java at line 29'
status: Done
assignee: []
created_date: '2026-02-21 09:48'
updated_date: '2026-02-21 09:59'
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
- **File:** `src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`
- **Line:** 29
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 48 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 29 in `src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 48 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `TaskDependencySorter.java:29` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactoring was already applied to the working tree. The sort() method (line 29) originally had cognitive complexity of 48 due to deeply nested loops, inline lambda comparators, and duplicate code blocks.

Changes made to TaskDependencySorter.java:
- Extracted buildSelectedIds() - stream collecting task IDs
- Extracted buildSelectedById() - builds LinkedHashMap by lowercased ID
- Extracted buildAllById() - builds HashMap for allSpecs lookup
- Extracted initializeGraphNodes() - initializes inDegree and dependents maps
- Extracted addInternalEdges() - adds edges only for internal (selected) deps
- Extracted buildInitialQueue() - seeds BFS queue with in-degree=0 nodes
- Extracted drainQueue() - snapshots and clears queue for layer processing
- Extracted layerComparator() - comparator by ordinal then numeric ID
- Extracted processLayerDependents() - decrements in-degree and enqueues ready nodes
- Extracted checkForCycles() - throws CircularDependencyException if cycle detected
- Extracted resolveTaskId() - resolves display ID from selected map

Also removed unused allById map that was built but never used in sort() and sortByLayers().

All 30+ existing tests in TaskDependencySorterTest pass. No new tests needed as coverage is comprehensive.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nResolved java:S3776 (Cognitive Complexity) in `TaskDependencySorter.java:29`.\n\nThe `sort()` method originally had a cognitive complexity of 48 (limit: 15) due to a monolithic implementation with deeply nested loops, inline lambda comparators, and duplicated logic between `sort()` and `sortByLayers()`.\n\n### Refactoring Approach\n\nExtracted 11 private helper methods to decompose the complexity:\n\n| Helper | Purpose |\n|--------|--------|\n| `buildSelectedIds()` | Collects IDs of selected tasks into a Set |\n| `buildSelectedById()` | Builds a LinkedHashMap of tasks keyed by lowercased ID |\n| `buildAllById()` | Builds a HashMap for allSpecs lookup |\n| `initializeGraphNodes()` | Seeds inDegree=0 and empty dependents sets for all nodes |\n| `addInternalEdges()` | Adds directed edges only for internal (within-selection) dependencies |\n| `buildInitialQueue()` | Seeds the BFS queue with all nodes having inDegree=0 |\n| `drainQueue()` | Snapshots the current queue and clears it for layer-by-layer processing |\n| `layerComparator()` | Returns a Comparator sorting by ordinal then numeric task ID |\n| `processLayerDependents()` | Decrements in-degree for dependents, enqueues newly-unblocked nodes |\n| `checkForCycles()` | Throws CircularDependencyException if not all nodes were processed |\n| `resolveTaskId()` | Resolves a display task ID from the selected map |\n\nAs a bonus, the unused `allById` map that was built but never referenced inside `sort()` and `sortByLayers()` was removed.\n\n### Files Modified\n- `src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`\n\n### Test Results\n- All 30+ tests in `TaskDependencySorterTest` pass (run with `./gradlew test --tests TaskDependencySorterTest`)\n- No new tests required — existing suite provides comprehensive coverage of all code paths
<!-- SECTION:FINAL_SUMMARY:END -->
