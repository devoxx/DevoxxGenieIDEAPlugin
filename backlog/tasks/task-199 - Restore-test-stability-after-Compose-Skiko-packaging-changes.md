---
id: TASK-199
title: Restore test stability after Compose/Skiko packaging changes
status: In Progress
assignee: []
created_date: '2026-03-07 17:11'
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
- [ ] #1 `./gradlew test` completes successfully in the current repository state.
- [ ] #2 The fix preserves the working runtime packaging for supported IDE builds and does not regress the manual 251 plugin startup validation.
- [ ] #3 Any build logic changes clearly separate test-classpath handling from plugin distribution packaging where needed.
<!-- AC:END -->
