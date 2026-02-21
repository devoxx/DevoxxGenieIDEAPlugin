---
id: TASK-116
title: Fix java:S3776 in GlobTool.java at line 14
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
ordinal: 116000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`
- **Line:** 14
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 14 in `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `GlobTool.java:14` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `convertGlobToRegex` in `GlobTool.java` by extracting five private helper methods to reduce Cognitive Complexity from 36 to under 15:

- `handleBackslash(String glob, int i, StringBuilder regex)` — handles escape sequences; returns updated index
- `handleAsterisk(String glob, int i, int inClass, StringBuilder regex)` — handles `*` and `**`; returns updated index
- `handleQuestion(int inClass, StringBuilder regex)` — handles `?` inside/outside character class
- `handleOpenBracket(String glob, int i, StringBuilder regex)` — handles `[`, `[!` (negation) and `[^` (escaped caret); returns updated index
- `handleComma(int inGroup, StringBuilder regex)` — handles `,` inside/outside brace groups

The main method's logic is unchanged; only structure was extracted. All 16 existing tests pass.

## Final Summary

Fixed SonarQube rule `java:S3776` (Cognitive Complexity) in `GlobTool.java`. The single `convertGlobToRegex` method had complexity 36 due to nested control flow inside a `switch` statement within a `for` loop. Extracted five focused private helper methods — each with complexity well below 15 — leaving the main method with complexity of ~3. No logic was altered; all existing tests continue to pass.
