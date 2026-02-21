---
id: TASK-112
title: Fix java:S3776 in FileSelectionPanelFactory.java at line 153
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:58'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 112000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java`
- **Line:** 153
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 153 in `src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `FileSelectionPanelFactory.java:153` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

**Root cause:** The `searchFiles` method (line 153) contained an anonymous `Task.Backgroundable` inner class with
deeply nested loops and conditionals. SonarQube counted the combined cognitive complexity of the anonymous class's
methods as part of the enclosing `searchFiles` method, yielding a total of 49 (limit is 15).

**Fix applied:** Extracted the anonymous inner class into a named private static inner class `FileSearchTask` and
also extracted the inner loop body of `searchProjectFiles` into a new `addMatchingFiles` helper method.

**Files modified:**
- `src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java`

**Cognitive complexity breakdown after fix (all methods ≤ 15):**
- `searchFiles`: ~0 (single line instantiation + queue)
- `FileSearchTask.run`: 1 (lambda)
- `FileSearchTask.searchVirtualFiles`: 2 (two early-return ifs)
- `FileSearchTask.searchOpenFiles`: ~5 (for + 2 ifs)
- `FileSearchTask.searchProjectFiles`: ~10 (for + 3 ifs at various depths)
- `FileSearchTask.addMatchingFiles`: ~6 (for + 2 ifs)
- `FileSearchTask.onSuccess`: ~3 (lambda + for)

## Final Summary

Resolved SonarQube java:S3776 (Cognitive Complexity) in `FileSelectionPanelFactory.java` at line 153.

The `searchFiles` method hosted an anonymous `Task.Backgroundable` class whose combined nesting — for loops inside
if-blocks inside the anonymous class methods — pushed the cognitive complexity to 49 against the allowed 15.

**Changes made:**
1. Replaced the anonymous `Task.Backgroundable` with a named `private static final class FileSearchTask`.
2. Moved all fields (`searchText`, `listModel`, `resultList`, `openFiles`, `foundFiles`) into the new class as
   instance fields, passed via constructor.
3. Extracted the inner `for`/`if` loop inside `searchProjectFiles` into a new `addMatchingFiles(GotoFileModel, String)`
   method, further reducing `searchProjectFiles` complexity from ~26 to ~10.
4. The `searchFiles` factory method is now a trivial one-liner.

No functional changes — all logic is preserved exactly. The `MAX_RESULTS` constant is accessible from the static
nested class via the outer class's static scope. Compilation confirmed successful.
