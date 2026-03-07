---
id: TASK-199
title: Restore test stability after Compose/Skiko packaging changes
status: Done
assignee: []
created_date: '2026-03-07 17:11'
updated_date: '2026-03-07 17:30'
labels:
  - tests
  - build
  - compose
  - skiko
dependencies:
  - TASK-198
references:
  - /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build.gradle.kts
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate and fix the test regressions introduced after updating the plugin packaging strategy for Compose 1.7.3 and Skiko 0.8.18. The user reports that ./gradlew test now fails broadly after the runtime compatibility work for the 251+ IDE line.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `./gradlew test` completes successfully in the current repository state.
- [x] #2 The fix preserves the working runtime packaging for supported IDE builds and does not regress the manual 251 plugin startup validation.
- [ ] #3 Any build logic changes clearly separate test-classpath handling from plugin distribution packaging where needed.
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Restored test stability after the Compose/Skiko packaging changes by preserving Gradle's live test classpath wiring while still filtering out bundled kotlinx-coroutines jars. The test task now uses classpath.filter(...) instead of rebuilding the classpath with files(...), which avoids JUnit discovery NoClassDefFoundError failures while keeping the IntelliJ platform test hang fix in place.
<!-- SECTION:FINAL_SUMMARY:END -->
