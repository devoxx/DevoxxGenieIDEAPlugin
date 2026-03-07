---
id: TASK-196
title: Make Compose UI compatible across supported IntelliJ IDEA branches
status: Done
assignee: []
created_date: '2026-03-07 15:45'
updated_date: '2026-03-07 16:05'
labels:
  - build
  - compose
  - compatibility
  - plugin-verifier
dependencies:
  - TASK-194
references:
  - /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build.gradle.kts
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/kotlin/com/devoxx/genie/ui/compose
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Align the plugin's Compose UI build and source usage with the Compose runtime APIs available in the supported IntelliJ IDEA branches so plugin verification passes across the intended range.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The Compose UI build does not generate verifier ABI errors for the supported IntelliJ IDEA branches.
- [x] #2 `./gradlew verifyPlugin` passes for the chosen supported IDE matrix.
- [x] #3 Any remaining compatibility range changes are reflected explicitly in Gradle metadata if still required.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Dropped 243 support and moved the baseline to 251 by setting `sinceBuild = "251"`, defaulting the development IDE dependency to `2025.1.7`, and trimming the verifier matrix to 251/252/253.

Aligned the build to the 251 Kotlin/Compose compiler line (`2.1.10`) and added Compose Desktop 1.7.3 as compile-only stubs so the plugin compiles against the 251-era API without packaging external Compose jars.

Filtered the IntelliJ Platform Gradle plugin's hot-reload-only Compose compiler option (`generateFunctionKeyMetaAnnotations`) from Kotlin compile tasks because it is not supported by the 2.1.10 compiler line.

Adapted the Markdown renderer styling setup in `AiBubble.kt` and `UserBubble.kt` to the older 0.28.0 renderer API (`link`, `codeText`, `inlineCodeText`, `linkText`, `tableText`).

Verified with `./gradlew -q compileKotlin` and `./gradlew verifyPlugin`. The verifier now passes for IC-251.29188.11, IC-252.28539.33, and IU-253.31033.145; each report shows compatibility with deprecated API warnings only.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Raised the plugin's minimum supported IDE to 251 and realigned the Compose build to that platform line. The plugin now compiles cleanly against a 251-era Compose/Kotlin toolchain and `verifyPlugin` succeeds for the supported matrix (2025.1, 2025.2, 2025.3), leaving only deprecated API warnings rather than runtime compatibility failures.
<!-- SECTION:FINAL_SUMMARY:END -->
