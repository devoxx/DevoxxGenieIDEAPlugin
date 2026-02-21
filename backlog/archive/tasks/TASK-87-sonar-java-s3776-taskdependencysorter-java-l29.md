---
id: TASK-87
title: Fix java:S3776 in TaskDependencySorter.java at line 29
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
ordinal: 87000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 48 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`
- **Line:** 29
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 48 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 29 in `src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`.

## Implementation Notes

Refactored `TaskDependencySorter.java` to reduce cognitive complexity of `sort()` from 48 to ~3 by extracting private helper methods:

- Added `DependencyGraph` record to hold `dependents` and `inDegree` maps
- Extracted `buildSelectedById()` — builds `Map<String, TaskSpec>` from tasks list
- Extracted `buildDependencyGraph()` — builds adjacency list and in-degree map
- Extracted `initQueue()` — initialises BFS queue from zero-indegree nodes
- Extracted `sortLayerIds()` — sorts IDs within a layer by ordinal then numeric ID
- Extracted `processLayer()` — processes one BFS layer and returns specs
- Extracted `decrementDependents()` — decrements in-degrees and enqueues ready nodes
- Extracted `findCycleIds()` — collects IDs still in cycle after BFS
- Also refactored `sortByLayers()` to reuse all helpers (complexity reduced from ~30 to ~5)
- Removed dead code: unused `allById` map that was built but never referenced in `sort()`
- Compiled `NUMBER_PATTERN` regex as a static final constant (was compiled per call)
- Added proper `import java.util.regex.Matcher/Pattern` replacing inline fully-qualified names
- Removed unused `import java.util.stream.Collectors` and `java.util.Objects`

All 28 existing tests pass without modification.

## Final Summary

**File changed:** `src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java`

The `sort()` method at line 29 had a cognitive complexity of 48 due to deeply nested control flow: double for-loops with nested ifs, a while loop containing a comparator lambda and another nested for-loop, all driving complexity well above the allowed 15.

The fix extracts 7 private helper methods and one private `DependencyGraph` record. Each helper is small and focused, bringing `sort()` down to complexity ~3 (one if + one while + one if). The `sortByLayers()` method was similarly refactored to reuse the same helpers, dropping its complexity from ~30 to ~5. Dead code (unused `allById` map), a per-call regex compilation, and stale imports were also cleaned up as part of the same commit.

All 28 unit tests in `TaskDependencySorterTest` continue to pass.

## Acceptance Criteria

- [x] Issue `java:S3776` at `TaskDependencySorter.java:29` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass
