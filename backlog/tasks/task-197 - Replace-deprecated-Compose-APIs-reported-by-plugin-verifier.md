---
id: TASK-197
title: Replace deprecated Compose APIs reported by plugin verifier
status: To Do
assignee: []
created_date: '2026-03-07 16:06'
labels:
  - compose
  - compatibility
  - tech-debt
  - plugin-verifier
dependencies:
  - TASK-196
references:
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build/reports/pluginVerifier/IC-251.29188.11/report.html
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build/reports/pluginVerifier/IC-252.28539.33/report.html
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build/reports/pluginVerifier/IU-253.31033.145/report.html
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Address the remaining deprecated Compose API usages reported by `./gradlew verifyPlugin` on the supported IntelliJ IDEA 251/252/253 matrix. Current warnings include deprecated clipboard APIs, deprecated clickable overloads, and deprecated `getCurrentCompositeKeyHash`-based generated Compose calls.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The deprecated API usages currently reported by `./gradlew verifyPlugin` are removed or reduced to an explicitly accepted minimum.
- [ ] #2 `./gradlew verifyPlugin` still passes for the supported IDE matrix after the cleanup.
- [ ] #3 Any unavoidable remaining warnings are documented with rationale.
<!-- AC:END -->
