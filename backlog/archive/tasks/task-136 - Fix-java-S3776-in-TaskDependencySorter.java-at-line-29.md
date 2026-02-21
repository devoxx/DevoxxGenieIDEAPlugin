---
id: TASK-136
title: 'Fix java:S3776 in TaskDependencySorter.java at line 29'
status: Done
assignee: []
created_date: '2026-02-21 09:48'
updated_date: '2026-02-21 09:58'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`
- **Line:** 29
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 48 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 29 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`.
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
Refactored `src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`.

The `sort` method at line 29 had a cognitive complexity of 48 (limit 15). Fixed by extracting 9 private helper methods:
- `buildSelectedIds` – builds the normalized set of selected task IDs
- `buildSelectedById` – builds the LinkedHashMap lookup for selected tasks
- `buildAllById` – builds a HashMap for all specs (used by `getUnsatisfiedDependencies`)
- `initializeGraphNodes` – initialises adjacency list and in-degree map
- `addInternalEdges` – adds dependency edges within the selected set
- `buildInitialQueue` – seeds the BFS queue with zero-in-degree nodes
- `drainQueue` – drains the queue into a snapshot list
- `layerComparator` – comparator that orders tasks by ordinal then numeric ID
- `processLayerDependents` – decrements in-degrees and enqueues newly-eligible nodes
- `checkForCycles` – detects cycles and throws `CircularDependencyException`
- `resolveTaskId` – resolves a lowercase ID back to its original display ID

`sortByLayers` was also significantly simplified by reusing the same helpers.

The refactored `sort` method has a cognitive complexity of ~7 (well within 15). All 29 existing `TaskDependencySorterTest` tests pass. No new tests needed — existing test suite has complete branch coverage.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resolved `java:S3776` in `TaskDependencySorter.java` by refactoring the `sort` method (and opportunistically `sortByLayers`) to extract 11 focused private helper methods.\n\n**Root cause:** The `sort` method had a cognitive complexity of 48 — driven by nested loops, conditionals, and lambda comparators all inlined in a single 100-line method.\n\n**Fix:** Extracted the following private helpers:\n- `buildSelectedIds` / `buildSelectedById` / `buildAllById` — lookup map construction\n- `initializeGraphNodes` — graph initialisation\n- `addInternalEdges` — edge-building loop (kept its own loop/if structure, well within limit)\n- `buildInitialQueue` — seeds the BFS queue\n- `drainQueue` — snapshot-and-clear pattern\n- `layerComparator` — ordinal + numeric-ID comparator\n- `processLayerDependents` — decrement in-degrees and enqueue\n- `checkForCycles` / `resolveTaskId` — cycle detection and ID resolution\n\nThe refactored `sort` method's cognitive complexity is ~7 (limit 15). `sortByLayers` also benefits from the same helpers.\n\n**Tests:** All 29 existing `TaskDependencySorterTest` tests pass without modification. No new tests required — existing coverage is comprehensive. No new SonarQube issues introduced."
<!-- SECTION:FINAL_SUMMARY:END -->
