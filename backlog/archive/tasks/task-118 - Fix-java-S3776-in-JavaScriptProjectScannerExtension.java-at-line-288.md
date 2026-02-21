---
id: task-118
title: 'Fix java:S3776 in JavaScriptProjectScannerExtension.java at line 288'
status: Done
assignee: []
created_date: '2026-02-20 21:58'
updated_date: '2026-02-21 09:11'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 118000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java`
- **Line:** 288
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 288 in `src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `JavaScriptProjectScannerExtension.java:288` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 All related tests continue to pass
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
This task is a duplicate of the already-completed TASK-111. The fix was applied in commit ad60f170 ('fix(sonar): reduce cognitive complexity in analyzer and PSI tools'). The `enhanceBuildSystem` method at line 288 was already refactored by extracting helper methods (`getRunCommand`, `getInstallCommand`, `addTestCommands`, `addE2eTestCommands`, `addLintFormatCommands`, `addFrameworkCommands`). The method now has a cognitive complexity of ~2 (two null-checks), well below the limit of 15. No code changes were needed — the fix was already in place.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
TASK-118 is a duplicate of TASK-111, which was already completed in commit ad60f170. The `enhanceBuildSystem` method in JavaScriptProjectScannerExtension.java at line 288 had already been refactored to reduce cognitive complexity from 27 to ~2 by extracting six helper methods: `getRunCommand`, `getInstallCommand`, `addTestCommands`, `addE2eTestCommands`, `addLintFormatCommands`, and `addFrameworkCommands`. The current code at line 288 fully satisfies all three acceptance criteria — no code changes were required.
<!-- SECTION:FINAL_SUMMARY:END -->
