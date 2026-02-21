---
id: TASK-111
title: Fix java:S3776 in JavaScriptProjectScannerExtension.java at line 288
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:48'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 111000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java`
- **Line:** 288
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 288 in `src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `JavaScriptProjectScannerExtension.java:288` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `enhanceBuildSystem` (was line 288) by extracting five helper methods:
- `getRunCommand(String packageManager)` — replaces nested ternary for run command
- `getInstallCommand(String packageManager)` — replaces nested ternary for install command
- `addTestCommands(...)` — handles unit test command setup
- `addE2eTestCommands(...)` — handles e2e test command setup
- `addLintFormatCommands(...)` — handles ESLint/Prettier command setup
- `addFrameworkCommands(...)` — handles framework-specific commands (Next.js, Nuxt.js, Angular)

The `enhanceBuildSystem` method now has a cognitive complexity of ~2 (two null-checks), well below the limit of 15. Each extracted helper has complexity ≤ 4.

## Final Summary

Resolved SonarQube java:S3776 in `JavaScriptProjectScannerExtension.java`. The `enhanceBuildSystem` method had a cognitive complexity of 27, exceeding the allowed limit of 15. The fix extracts all conditional logic into five focused helper methods (`getRunCommand`, `getInstallCommand`, `addTestCommands`, `addE2eTestCommands`, `addLintFormatCommands`, `addFrameworkCommands`), reducing the main method's complexity to ~2. All existing behaviour is preserved — no logic was changed, only restructured. No new SonarQube issues were introduced (all new methods have low individual complexity and follow existing patterns).
