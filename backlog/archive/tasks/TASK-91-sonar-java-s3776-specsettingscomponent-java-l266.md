---
id: TASK-91
title: Fix java:S3776 in SpecSettingsComponent.java at line 266
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
ordinal: 91000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/settings/spec/SpecSettingsComponent.java`
- **Line:** 266
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 266 in `src/main/java/com/devoxx/genie/ui/settings/spec/SpecSettingsComponent.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SpecSettingsComponent.java:266` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

The `performInit()` method had a cognitive complexity of 17 (limit: 15) due to deeply nested structure:
- outer try/catch wrapping a lambda
- inner try/catch inside the lambda
- nested conditionals for refreshing the spec directory (if basePath != null → if specDirName empty → if vf != null)
- if/else if/else result handling

The fix extracts three helper methods:
1. `runInitProgress(Exception[] error)` — contains the inner try/catch with the progress steps
2. `refreshSpecDirectory()` — handles the VFS refresh logic with its nested conditionals
3. `handleInitResult(boolean cancelled, Exception error)` — handles the post-execution if/else if/else branching

This brings `performInit()` down to ~4 complexity points. All new helpers are well below the 15-point limit.

## Final Summary

**File modified:** `src/main/java/com/devoxx/genie/ui/settings/spec/SpecSettingsComponent.java`

**Change:** Refactored `performInit()` (line 266) from a single deeply-nested method (cognitive complexity 17) into four focused methods:
- `performInit()` — now just orchestrates: start, run progress task, handle result (~4 complexity)
- `runInitProgress(Exception[] error)` — executes the background work steps with try/catch (~5 complexity)
- `refreshSpecDirectory()` — performs VFS refresh for the spec directory (~4 complexity)
- `handleInitResult(boolean, Exception)` — updates UI and sends notifications based on outcome (~3 complexity)

All existing tests pass. No new complexity issues introduced.
