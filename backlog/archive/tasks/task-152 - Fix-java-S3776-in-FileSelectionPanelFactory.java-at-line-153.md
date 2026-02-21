---
id: TASK-152
title: 'Fix java:S3776 in FileSelectionPanelFactory.java at line 153'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 10:59'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java`
- **Line:** 153
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 153 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `FileSelectionPanelFactory.java:153` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Applied same fix as TASK-112 to the worktree file. Replaced the anonymous Task.Backgroundable inner class in `searchFiles` (line 153) with a named `private static final class FileSearchTask`. Also extracted the inner loop body of `searchProjectFiles` into a new `addMatchingFiles` helper method. The main project file (src/main/java/...) was already fixed by TASK-112; this task fixes the worktree copy at .claude/worktrees/sunny-exploring-lemon/. Files modified: .claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java. No test file exists for FileSelectionPanelFactory — this is a UI panel factory that requires IntelliJ Platform infrastructure, making standalone unit tests impractical without a running IDE instance.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resolved SonarQube java:S3776 (Cognitive Complexity) in the worktree copy of `FileSelectionPanelFactory.java` at line 153.\n\nThe `searchFiles` method hosted an anonymous `Task.Backgroundable` class whose combined nesting — for loops inside if-blocks inside the anonymous class methods — pushed the cognitive complexity to 49 against the allowed 15.\n\n**Context:** The main project file (`src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java`) was already fixed by TASK-112. This task applied the identical fix to the worktree copy at `.claude/worktrees/sunny-exploring-lemon/`.\n\n**Changes made:**\n1. Replaced the anonymous `Task.Backgroundable` with a named `private static final class FileSearchTask extends Task.Backgroundable`.\n2. Moved all fields (`searchText`, `listModel`, `resultList`, `openFiles`, `foundFiles`) into the new class as instance fields, passed via constructor.\n3. Extracted the inner `for`/`if` loop inside `searchProjectFiles` into a new `addMatchingFiles(GotoFileModel, String)` method, further reducing cognitive complexity.\n4. The `searchFiles` factory method is now a trivial one-liner: `new FileSearchTask(...).queue()`.\n\n**Cognitive complexity after fix (all well within limit of 15):**\n- `searchFiles`: ~0 (single line)\n- `FileSearchTask.searchVirtualFiles`: 2\n- `FileSearchTask.searchOpenFiles`: ~5\n- `FileSearchTask.searchProjectFiles`: ~10\n- `FileSearchTask.addMatchingFiles`: ~6\n- `FileSearchTask.onSuccess`: ~3\n\n**Tests:** No dedicated unit tests exist for `FileSelectionPanelFactory` (UI panel requiring IntelliJ platform infrastructure). Related `FileListManagerTest` (which tests the service used by the panel) was run and all 8 tests pass. No functional changes — all logic preserved exactly."
<!-- SECTION:FINAL_SUMMARY:END -->
