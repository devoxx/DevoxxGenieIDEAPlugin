---
id: TASK-164
title: 'Fix java:S3776 in KotlinProjectScannerExtension.java at line 364'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 12:00'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/languages/kotlin/KotlinProjectScannerExtension.java`
- **Line:** 364
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 364 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/languages/kotlin/KotlinProjectScannerExtension.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `KotlinProjectScannerExtension.java:364` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The `enhanceBuildSystem` method in `KotlinProjectScannerExtension.java` was already refactored in commit `2c8fb447` (as part of the `sonarlint-fixes` branch). The complex single method (cognitive complexity 27) was split into five helper methods:
- `getBuildCommands(isGradle, isMaven)` — returns the base build/test/run/clean/singleTest commands
- `addFrameworkCommands(commands, kotlinInfo, isGradle, isMaven)` — adds Spring Boot bootRun command
- `addAndroidCommands(commands, kotlinInfo)` — adds Android-specific Gradle commands
- `addMultiplatformCommands(commands, kotlinInfo)` — adds KMP Gradle commands
- `addCodeQualityCommands(commands, kotlinInfo, isGradle)` — adds ktlint and Detekt commands

No existing tests covered this class, so a new test file was created at:
`src/test/java/com/devoxx/genie/service/analyzer/languages/kotlin/KotlinProjectScannerExtensionTest.java`

11 tests written covering all refactored helper methods. All 11 tests pass.

Key technique: mock `VfsUtilCore.loadText` (not `VfsUtil.loadText`) with `any(VirtualFile.class)` to avoid Mockito recorder-state issues in the IntelliJ test environment, consistent with the JavaScriptProjectScannerExtensionTest pattern.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed SonarQube `java:S3776` (cognitive complexity > 15) in `KotlinProjectScannerExtension.java` at line 364.\n\n### What Was Done\n\nThe `enhanceBuildSystem` method (cognitive complexity 27 in the worktree reference) had already been refactored in commit `2c8fb447` on the `sonarlint-fixes` branch. The single complex method was decomposed into five focused helper methods:\n\n- `getBuildCommands(isGradle, isMaven)` — selects Gradle/Maven/default build commands\n- `addFrameworkCommands(commands, kotlinInfo, isGradle, isMaven)` — adds Spring Boot `bootRun`\n- `addAndroidCommands(commands, kotlinInfo)` — adds Android Gradle commands\n- `addMultiplatformCommands(commands, kotlinInfo)` — adds Kotlin Multiplatform commands\n- `addCodeQualityCommands(commands, kotlinInfo, isGradle)` — adds ktlint/Detekt commands\n\n### Tests Added\n\nCreated `src/test/java/com/devoxx/genie/service/analyzer/languages/kotlin/KotlinProjectScannerExtensionTest.java` with 11 tests covering all refactored paths:\n\n1. `enhanceProjectInfo_skipsNonKotlinProject` — guard: non-Kotlin project skipped\n2. `enhanceProjectInfo_skipsNullLanguages` — guard: null languages map skipped\n3. `enhanceBuildSystem_gradleKts_addsGradleCommands` — Gradle KTS build commands\n4. `enhanceBuildSystem_mavenProject_addsMavenCommands` — Maven build commands\n5. `enhanceBuildSystem_noBuildFile_defaultsToGradleCommands` — default Gradle commands\n6. `addFrameworkCommands_springBootWithGradle_addsBootRunCommand` — Spring Boot + Gradle\n7. `addFrameworkCommands_springBootWithMaven_addsBootRunCommand` — Spring Boot + Maven\n8. `addAndroidCommands_androidGradleProject_addsAndroidCommands` — Android commands\n9. `addMultiplatformCommands_kmpGradleProject_addsMultiplatformCommands` — KMP commands\n10. `addCodeQualityCommands_ktlintWithGradle_addsKtlintCommands` — ktlint commands\n11. `addCodeQualityCommands_detektWithGradle_addsDetektCommand` — Detekt command\n\nAll 11 tests pass. Used `VfsUtilCore.loadText` mock (not `VfsUtil.loadText`) with `any(VirtualFile.class)` to avoid Mockito recorder-state issues in the IntelliJ test environment.
<!-- SECTION:FINAL_SUMMARY:END -->
