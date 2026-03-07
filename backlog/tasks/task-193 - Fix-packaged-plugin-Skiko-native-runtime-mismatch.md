---
id: TASK-193
title: Fix packaged plugin Skiko native runtime mismatch
status: In Progress
assignee:
  - Codex
created_date: '2026-03-07 15:22'
updated_date: '2026-03-07 15:29'
labels:
  - build
  - plugin-distribution
  - compose
dependencies: []
references:
  - /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build.gradle.kts
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build/idea-sandbox/IC-2024.3/plugins/DevoxxGenie/lib
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The built plugin distribution currently packages Skiko AWT classes at version 0.9.37.4 while the platform-specific skiko-awt-runtime native jars are pinned to 0.8.18. On macOS this causes UnsatisfiedLinkError in MetalApiKt when the Compose tool window initializes. The build should package matching Skiko runtime artifacts so the distributed plugin starts cleanly.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The plugin build configuration no longer mixes Skiko AWT classes and skiko-awt-runtime native jars from different versions.
- [x] #2 Rebuilding the plugin distribution produces a ZIP whose packaged Skiko runtime jars match the resolved Skiko AWT version.
- [x] #3 The packaged plugin no longer includes the previously mismatched 0.8.18 Skiko runtime jars alongside skiko-awt 0.9.37.4.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Align the explicit `skiko-awt-runtime-*` dependencies in `build.gradle.kts` with the Skiko version resolved by Compose runtime dependencies.
2. Rebuild the plugin distribution and inspect the packaged plugin lib directory to confirm the runtime jars now match the Skiko AWT version.
3. If the rebuilt plugin still ships conflicting Kotlin/coroutines runtime artifacts that appear to interfere with IDE startup, inspect and narrow packaging further before stopping.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
User reported UnsatisfiedLinkError in the packaged plugin at Compose/Skiko initialization on macOS.

User retested the rebuilt ZIP in IDEA and still hit the same MetalApi UnsatisfiedLinkError, so the issue is not fully resolved. Investigating whether the plugin should stop bundling Compose/Skiko runtime jars altogether and rely on IntelliJ platform Compose UI instead.

Rebuilt the plugin after stripping platform-provided Compose/Kotlin runtime jars from `prepareSandbox`. Verified that neither `build/idea-sandbox/IC-2024.3/plugins/DevoxxGenie/lib` nor `build/distributions/DevoxxGenie-1.0.0.zip` contains `skiko-*`, Compose desktop jars, `kotlin-stdlib*`, or `kotlinx-coroutines-core*` anymore. Awaiting runtime retest in IDEA.
<!-- SECTION:NOTES:END -->
