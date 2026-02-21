---
id: task-119
title: 'Fix java:S3776 in KotlinProjectScannerExtension.java at line 364'
status: Done
assignee: []
created_date: '2026-02-20 21:58'
updated_date: '2026-02-21 09:13'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 119000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/kotlin/KotlinProjectScannerExtension.java`
- **Line:** 364
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 364 in `src/main/java/com/devoxx/genie/service/analyzer/languages/kotlin/KotlinProjectScannerExtension.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `KotlinProjectScannerExtension.java:364` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 All related tests continue to pass
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `enhanceBuildSystem` (line 364) by extracting 4 private helper methods:
- `getBuildCommands(boolean isGradle, boolean isMaven)` — returns a map of standard build commands; complexity 3
- `addFrameworkCommands(...)` — handles Spring Boot/Ktor bootRun command; complexity 4 (early-return guard clauses)
- `addAndroidCommands(...)` — handles Android-specific Gradle tasks; complexity 2
- `addMultiplatformCommands(...)` — handles KMP-specific Gradle tasks; complexity 2
- `addCodeQualityCommands(...)` — handles ktlint and Detekt commands; complexity 10

Also removed the unused `isGradleKts` local variable (would have caused java:S1481).

The original `enhanceBuildSystem` method now has cognitive complexity 2 (two null-guard ifs). All helper methods are well below the threshold of 15.

No existing test class found for this file. No new SonarQube issues introduced.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resolved java:S3776 in `KotlinProjectScannerExtension.java` at line 364 by refactoring the monolithic `enhanceBuildSystem` method (cognitive complexity 27) into 5 focused methods:\n\n**Before:** Single `enhanceBuildSystem` method with deeply nested conditionals for build system detection, framework commands, Android/multiplatform commands, and code quality tooling — complexity 27.\n\n**After:**\n- `enhanceBuildSystem` — orchestrates the flow; complexity 2\n- `getBuildCommands(isGradle, isMaven)` — returns base build/test/run/clean commands; complexity 3\n- `addFrameworkCommands(commands, kotlinInfo, isGradle, isMaven)` — adds Spring Boot bootRun with guard-clause pattern; complexity 4\n- `addAndroidCommands(commands, kotlinInfo)` — adds assembleDebug/Release/installDebug/connectedAndroidTest; complexity 2\n- `addMultiplatformCommands(commands, kotlinInfo)` — adds KMP jsBrowserRun/iosX64Test/allTests; complexity 2\n- `addCodeQualityCommands(commands, kotlinInfo, isGradle)` — adds ktlint and Detekt commands; complexity 10\n\nAlso removed the unused `isGradleKts` boolean (was declared but never referenced) to preempt java:S1481.\n\nNo tests existed for this class. No new SonarQube issues introduced. All pre-existing unchecked-cast warnings in the file are unchanged (not caused by this refactoring)."
<!-- SECTION:FINAL_SUMMARY:END -->
