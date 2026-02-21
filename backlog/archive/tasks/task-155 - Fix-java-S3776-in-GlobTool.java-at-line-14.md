---
id: TASK-155
title: 'Fix java:S3776 in GlobTool.java at line 14'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 11:10'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`
- **Line:** 14
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 14 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `GlobTool.java:14` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fix was already applied to main source file `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`. The monolithic `convertGlobToRegex` method (cognitive complexity 36) was refactored by extracting 5 private helper methods: `handleBackslash`, `handleAsterisk`, `handleQuestion`, `handleOpenBracket`, and `handleComma`. This reduces the complexity of the main method to well below the 15 threshold. A comprehensive test class `GlobToolTest` already exists with 17 test cases covering all branches. All tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The `java:S3776` issue (Cognitive Complexity 36 > 15) in `GlobTool.convertGlobToRegex()` was resolved by refactoring the method to delegate to 5 private helper methods:\n\n- `handleBackslash(String glob, int i, StringBuilder regex)` — handles `\\` escape sequences\n- `handleAsterisk(String glob, int i, int inClass, StringBuilder regex)` — handles `*` and `**` patterns\n- `handleQuestion(int inClass, StringBuilder regex)` — handles `?` wildcard\n- `handleOpenBracket(String glob, int i, StringBuilder regex)` — handles `[` character class opening with negation logic\n- `handleComma(int inGroup, StringBuilder regex)` — handles `,` inside/outside brace groups\n\nThe refactored main method is a clean dispatcher (switch statement delegating to helpers) with cognitive complexity well below 15. No new SonarQube issues were introduced. 17 existing tests in `GlobToolTest` cover all branches and all pass successfully."]
<!-- SECTION:FINAL_SUMMARY:END -->
