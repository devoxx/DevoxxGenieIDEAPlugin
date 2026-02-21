---
id: TASK-158
title: 'Fix java:S3776 in BuildSystemDetector.java at line 43'
status: Done
assignee: []
created_date: '2026-02-21 11:13'
updated_date: '2026-02-21 11:18'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/agent/tool/BuildSystemDetector.java`
- **Line:** 43
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 43 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/agent/tool/BuildSystemDetector.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `BuildSystemDetector.java:43` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `getTestCommand` in `BuildSystemDetector.java` by extracting the inline switch-case bodies into five private helper methods: `addGradleCommands`, `addMavenCommands`, `addNpmCommands`, `addCargoCommands`, `addGoCommands`. This reduced the cognitive complexity of `getTestCommand` from 19 to ~5 (one switch with flat delegates). Comprehensive tests already existed in `BuildSystemDetectorTest.java` covering all build systems, with-target/no-target cases, and Windows vs Unix — all 19 tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed `java:S3776` (Cognitive Complexity > 15) in `BuildSystemDetector.java:43` (`getTestCommand` method).\n\n**Root Cause:** The original `getTestCommand` method had all per-build-system logic inline in a single switch statement, accumulating a cognitive complexity of 19.\n\n**Fix:** Extracted the body of each switch case into a dedicated private helper method:\n- `addGradleCommands(command, testTarget, hasTarget, isWindows)`\n- `addMavenCommands(command, testTarget, hasTarget, isWindows)`\n- `addNpmCommands(command, testTarget, hasTarget, isWindows)`\n- `addCargoCommands(command, testTarget, hasTarget)`\n- `addGoCommands(command, testTarget, hasTarget)`\n\nThe `getTestCommand` method now contains a flat switch with simple delegate calls, reducing cognitive complexity from 19 to approximately 5.\n\n**Files Changed:** `src/main/java/com/devoxx/genie/service/agent/tool/BuildSystemDetector.java`\n\n**Tests:** `BuildSystemDetectorTest.java` already had 19 tests covering all build systems, with/without target, and Windows/Unix variants — all pass."
<!-- SECTION:FINAL_SUMMARY:END -->
