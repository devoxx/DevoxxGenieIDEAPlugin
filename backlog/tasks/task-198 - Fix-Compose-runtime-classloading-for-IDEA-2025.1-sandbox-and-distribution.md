---
id: TASK-198
title: Fix Compose runtime classloading for IDEA 2025.1 sandbox and distribution
status: Done
assignee: []
created_date: '2026-03-07 16:39'
updated_date: '2026-03-07 17:08'
labels:
  - build
  - compose
  - intellij-platform
  - runtime
dependencies:
  - TASK-196
references:
  - /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build.gradle.kts
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/kotlin/com/devoxx/genie/ui/compose/ComposeConversationViewController.kt
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Resolve the runtime NoClassDefFoundError for androidx.compose.runtime.SnapshotStateKt when running the plugin on IntelliJ IDEA Community 2025.1.7. The current build verifies against 251/252/253, but the plugin sandbox/distribution still fails at runtime because Compose runtime classes are not visible to the plugin classloader in the supported 251 baseline.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Running the plugin in IntelliJ IDEA Community 2025.1.7 opens the DevoxxGenie tool window without Compose runtime classloading errors.
- [x] #2 The plugin packaging strategy for Compose libraries is consistent across sandbox and distribution builds and does not reintroduce the previous skiko or coroutines runtime conflicts.
- [x] #3 Plugin verification remains green for the supported IDE matrix after the packaging fix.
- [x] #4 The resulting build configuration documents the intended Compose runtime source for supported IDE branches.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Adjusted the plugin packaging strategy in /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build.gradle.kts so the plugin now bundles Compose 1.7.3 runtime/foundation/ui jars plus a matched Skiko 0.8.18 runtime set, while still stripping Kotlin stdlib and kotlinx-coroutines jars to avoid the previous ABI conflicts. Also replaced the unstable AnnotatedString(text) call in /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/kotlin/com/devoxx/genie/ui/compose/components/CopyButton.kt with buildAnnotatedString { append(textToCopy) } to avoid constructor ABI issues across supported IDE lines.

User confirmed the rebuilt plugin opens and runs successfully on IntelliJ IDEA Community 2025.1.7 (build IC-251.29188.11) on macOS arm64 after packaging Compose 1.7.3 API jars together with matched Skiko 0.8.18 runtime jars. Verification still needs a clean rerun after the final packaging changes.

Ran ./gradlew verifyPlugin successfully after the final Skiko packaging change. Plugin Verifier reported Compatible for IC-251.29188.11, IC-252.28539.33, and IU-253.31033.145. Remaining verifier output is limited to unresolved optional dependencies not bundled with those IDE distributions, which does not block compatibility.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed Compose runtime packaging for the 251+ plugin line. The plugin now bundles the Compose 1.7.3 desktop jars together with the matched Skiko 0.8.18 runtime set, while continuing to exclude Kotlin stdlib and kotlinx-coroutines jars to avoid previous runtime ABI conflicts. Also replaced the unstable AnnotatedString(text) constructor call in CopyButton with buildAnnotatedString to keep bytecode compatible across supported IDE lines. Manual validation succeeded on IntelliJ IDEA Community 2025.1.7 (IC-251.29188.11), and ./gradlew verifyPlugin now reports Compatible for IC-251.29188.11, IC-252.28539.33, and IU-253.31033.145.
<!-- SECTION:FINAL_SUMMARY:END -->
