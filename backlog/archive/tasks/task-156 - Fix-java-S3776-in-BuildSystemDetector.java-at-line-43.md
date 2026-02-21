---
id: TASK-156
title: 'Fix java:S3776 in BuildSystemDetector.java at line 43'
status: Done
assignee: []
created_date: '2026-02-21 11:13'
updated_date: '2026-02-21 11:16'
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
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/BuildSystemDetector.java`
- **Line:** 43
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 43 in `src/main/java/com/devoxx/genie/service/agent/tool/BuildSystemDetector.java`.
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
Refactored `getTestCommand` in `BuildSystemDetector.java` to reduce cognitive complexity from 19 to ~1. Extracted the per-build-system logic from the `switch` cases into five private helper methods: `addGradleCommands`, `addMavenCommands`, `addNpmCommands`, `addCargoCommands`, `addGoCommands`. The main method's switch now contains only simple single-expression case arms. All 16 existing tests in `BuildSystemDetectorTest` pass. No new tests needed — coverage was already complete.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Reduced cognitive complexity of `getTestCommand` in `BuildSystemDetector.java` from 19 to 1 (well below the 15 threshold), resolving `java:S3776`.\n\n**Approach:** Extracted each `switch` case body into a dedicated private helper method:\n- `addGradleCommands` — handles gradlew/gradlew.bat + optional `--tests` arg\n- `addMavenCommands` — handles mvn/mvn.cmd + optional `-Dtest=` arg\n- `addNpmCommands` — handles npm/npm.cmd + optional `-- testTarget` args\n- `addCargoCommands` — handles `cargo test` + optional target\n- `addGoCommands` — handles `go test` with target or `./...` fallback\n\nThe main `getTestCommand` switch now has only single-expression arms with no nested conditionals, dropping its complexity to 1. Each helper's own complexity is at most 4 (two ternaries + one if), well within limits.\n\n**Files modified:** `src/main/java/com/devoxx/genie/service/agent/tool/BuildSystemDetector.java`\n\n**Tests:** All 16 tests in `BuildSystemDetectorTest` pass. Existing coverage was already comprehensive; no new tests required.">
<!-- SECTION:FINAL_SUMMARY:END -->
