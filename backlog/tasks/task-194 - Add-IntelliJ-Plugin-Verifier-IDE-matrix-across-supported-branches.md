---
id: TASK-194
title: Add IntelliJ Plugin Verifier IDE matrix across supported branches
status: Done
assignee: []
created_date: '2026-03-07 15:34'
updated_date: '2026-03-07 15:43'
labels:
  - build
  - compatibility
  - plugin-verifier
dependencies:
  - TASK-193
references:
  - /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/build.gradle.kts
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace the single-target plugin verifier configuration with a representative IDE matrix across the declared compatibility range, then run verifyPlugin and record the compatibility results.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Gradle plugin verification is configured to check multiple released IntelliJ IDEA versions across the supported build range.
- [x] #2 `./gradlew verifyPlugin` runs against the configured matrix.
- [x] #3 Results are summarized with any compatibility blockers called out.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Updated `build.gradle.kts` to verify a representative IntelliJ IDEA matrix across supported released branches: IC 2024.3.7, IC 2025.1.7, IC 2025.2.6.1, and unified IU/IntelliJ IDEA 2025.3.3.

Ran `./gradlew verifyPlugin`; verification now executes against four IDE targets instead of one.

Results: IC-243.28141.18 fails with 114 compatibility problems, IC-251.29188.11 fails with 40 compatibility problems and 3 deprecated API usages, IC-252.28539.33 fails with 40 compatibility problems and 3 deprecated API usages, IU-253.31033.145 is compatible with 3 deprecated API usages.

Main blockers are Compose ABI mismatches on older IDE branches (`getCurrentCompositeKeyHashCode`, `Updater.init-impl`, `ComposeUiNode.Companion.getApplyOnDeactivatedNodeAssertion`) and deprecated clipboard APIs on 252/253.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Configured plugin verification to cover four representative IntelliJ IDEA branches across the currently released compatibility range and ran the verifier. The plugin verifies cleanly only on the unified 253 line; 243, 251, and 252 fail due to Compose runtime/API incompatibilities, confirming the declared lower range is too broad for the current Compose-based UI implementation.
<!-- SECTION:FINAL_SUMMARY:END -->
