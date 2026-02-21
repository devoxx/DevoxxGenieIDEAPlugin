---
id: TASK-161
title: 'Fix java:S3776 in GlobTool.java at line 14'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 11:23'
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
The fix for java:S3776 was already present in the main source tree. The task was generated against a worktree version (.claude/worktrees/sunny-exploring-lemon/) which had the original inlined implementation with Cognitive Complexity 36.

Main source GlobTool.java already has the refactored version with helper methods extracted:
- handleBackslash() - handles escape sequences
- handleAsterisk() - handles * and ** patterns
- handleQuestion() - handles ? pattern
- handleOpenBracket() - handles [ and [! patterns
- handleComma() - handles , inside brace groups

This reduces convertGlobToRegex() cognitive complexity from 36 to ~3 (just a for loop + switch dispatching to helpers).

Test coverage: GlobToolTest.java already exists with 18 test cases covering all cases. All tests pass (BUILD SUCCESSFUL).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nThe java:S3776 issue in GlobTool.java was already resolved in the main source branch. The task was created against a worktree version (.claude/worktrees/sunny-exploring-lemon/) containing the original monolithic implementation.\n\n### What was done\n\nVerified that `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java` already contains the correct refactored implementation:\n\n**Original problem** (in worktree): `convertGlobToRegex()` at line 14 had all logic inlined inside a switch statement with deeply nested conditionals, resulting in Cognitive Complexity of 36 (limit: 15).\n\n**Fix already applied**: The method was refactored by extracting 5 private helper methods:\n- `handleBackslash(String, int, StringBuilder)` — handles `\\` escape sequences\n- `handleAsterisk(String, int, int, StringBuilder)` — handles `*` and `**` glob patterns\n- `handleQuestion(int, StringBuilder)` — handles `?` pattern\n- `handleOpenBracket(String, int, StringBuilder)` — handles `[` and `[!` patterns\n- `handleComma(int, StringBuilder)` — handles `,` inside brace groups\n\nThis reduces `convertGlobToRegex()` Cognitive Complexity from 36 to ~3 (for loop + switch dispatch only).\n\n### Tests\n\n`GlobToolTest.java` already exists with 18 comprehensive test cases covering all code paths. All tests pass (BUILD SUCCESSFUL).\n\n### Files involved\n- `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java` — already fixed\n- `src/test/java/com/devoxx/genie/service/analyzer/tools/GlobToolTest.java` — already has full coverage
<!-- SECTION:FINAL_SUMMARY:END -->
