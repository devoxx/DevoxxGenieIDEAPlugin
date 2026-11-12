---
id: TASK-276
title: Make DevoxxGenie settings load on IntelliJ IDEA 2026.3
status: Done
assignee:
  - '@Claude'
created_date: '2026-11-12T13:32:00Z'
updated_date: '2026-11-12T13:57:00Z'
labels:
  - ui
  - compatibility
dependencies: []
modified_files:
  - build.gradle.kts
  - src/test/java/com/devoxx/genie/packaging/NoSwingXDependencyTest.java
  - src/main/java/com/devoxx/genie/ui/settings/AbstractSettingsComponent.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/llmconfig/LLMConfigSettingsComponent.java
  - src/test/java/com/devoxx/genie/packaging/VcsExtensionRegistrationTest.java
  - src/main/resources/META-INF/vcs-features.xml
  - src/main/resources/META-INF/plugin.xml
  - >-
    src/main/java/com/devoxx/genie/service/automation/listeners/VcsCheckinHandlerFactory.java
priority: high
type: bug
ordinal: 23000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
On IntelliJ IDEA 2026.3 (build 263.7307.200) the DevoxxGenie settings pages never appear: the Settings dialog shows a "Loading" spinner, and idea.log reports "Cannot create configurable" for LLMProvidersConfigurable, caused by ClassNotFoundException: org.jdesktop.swingx.JXTitledSeparator. The plugin declares support up to build 263.* (#1388), but 2026.3 no longer makes SwingX available to plugins.

SwingX's JXTitledSeparator has been used since 2024 for settings section titles: AbstractSettingsComponent's section helper, which most DevoxxGenie settings pages inherit, and LLMConfigSettingsComponent's "LLM Chat Settings" header. It went unnoticed because the Plugin Verifier only checks 2025.3.3 and 2026.1, and the tests run on 2025.3.3, where SwingX is still present.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The DevoxxGenie settings pages open on IntelliJ IDEA 2026.3, including LLM Providers and LLM Settings
- [ ] #2 Settings section titles still render as titled separators on all supported IDE versions
- [ ] #3 The Plugin Verifier checks IntelliJ IDEA 2026.3 and reports no missing classes for the plugin
- [x] #4 A test fails if plugin sources use SwingX (org.jdesktop.swingx) again
- [ ] #5 Commit automation (VcsCheckinHandlerFactory) is registered through an optional dependency on com.intellij.modules.vcs, and opening the commit UI on IntelliJ IDEA 2026.3 logs no missing VCS classes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Reproduce: add "2026.3" to pluginVerifierUnifiedIdeVersions in build.gradle.kts and run ./gradlew verifyPlugin; expect org.jdesktop.swingx.JXTitledSeparator reported as missing on 263.
2. Guard test first: a test that fails while any main source imports org.jdesktop.swingx.
3. Replace JXTitledSeparator with com.intellij.ui.TitledSeparator in AbstractSettingsComponent and LLMConfigSettingsComponent.
4. Re-run verifyPlugin (2025.3.3, 2026.1, 2026.3) and the full test suite; IDE check on 2026.3 that the settings pages open.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
On fix/task-276-settings-on-idea-2026-3 (uncommitted).

- Reproduced: added "2026.3" to pluginVerifierUnifiedIdeVersions (and dropped the stale 2026.2-EAP comment). The verifier reported 2026.3 with 3 compatibility problems: org.jdesktop.swingx.JXTitledSeparator missing (AbstractSettingsComponent.addSection, LLMConfigSettingsComponent.createPanel), plus the VCS check-in API (CheckinProjectPanel, com.intellij.openapi.vcs.checkin.*) unresolved from VcsCheckinHandlerFactory and VcsCommitListener. 2025.3.3 and 2026.1 were Compatible. A first verifier run crashed because build/distributions/DevoxxGenie-1.17.0.zip wasn't on disk yet although buildPlugin was UP-TO-DATE; rerun after the zip existed.
- Checked the IDE jars: com.intellij.ui.TitledSeparator is in 2025.3.3 lib/app.jar and 2026.3 lib/intellij.platform.ide.jar. SwingX is in 2025.3.3 lib/lib.jar and absent from 2026.3 outside bundled plugins.
- SwingX fix: JXTitledSeparator replaced with TitledSeparator in both files. NoSwingXDependencyTest failed before and passes after. After the fix the verifier reports no org.jdesktop problems; 2025.3.3 and 2026.1 stay Compatible.
- VCS fix (scope added with the user's OK): 2026.3 moved the check-in API into the bundled plugin com.intellij.platform.vcs ("Version Control"), which declares the module com.intellij.modules.vcs; 2025.3.3 declares the same module in its core descriptor. The checkinHandlerFactory registration moved from plugin.xml to a new optional META-INF/vcs-features.xml (depends optional com.intellij.modules.vcs). VcsExtensionRegistrationTest failed before and passes after.
- The verifier still reports the 2 VCS problems on 2026.3. It resolves com.intellij.modules.vcs to the product module intellij.platform.vcs.impl and then looks for lib/modules/intellij.platform.vcs.jar, which 2026.3 doesn't ship (the jar is under plugins/platform-vcs-plugin/lib/modules); it logs that layout mismatch itself. So the verifier can't confirm the VCS fix, and AC #3 stays unchecked. Runtime check on 2026.3 needed (AC #5).
- Full suite passes.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixes DevoxxGenie on IntelliJ IDEA 2026.3 (build 263), which no longer provides two APIs the plugin used.

Settings stuck on "Loading": SwingX's JXTitledSeparator (org.jdesktop.swingx) is gone from the 2026.3 platform, so every settings page that drew a section title failed to load ("Cannot create configurable", ClassNotFoundException). It is replaced with the platform's com.intellij.ui.TitledSeparator in AbstractSettingsComponent and LLMConfigSettingsComponent. TitledSeparator is in 2025.3.3 (lib/app.jar) and 2026.3 (lib/intellij.platform.ide.jar). NoSwingXDependencyTest fails if SwingX is used again.

Commit automation: 2026.3 moved the VCS check-in API into the bundled "Version Control" plugin (com.intellij.platform.vcs), which declares the module com.intellij.modules.vcs; 2025.3.3 declares that module in its core. VcsCheckinHandlerFactory is now registered in a new optional vcs-features.xml that depends on com.intellij.modules.vcs. VcsExtensionRegistrationTest guards the registration.

The Plugin Verifier now also checks 2026.3. 2025.3.3 and 2026.1 are Compatible, and the SwingX problem is gone on 2026.3. The verifier still lists the two VCS problems on 2026.3, but it looks for those classes at lib/modules/intellij.platform.vcs.jar, a path 2026.3 doesn't ship, and logs that mismatch itself, so it can't confirm the VCS change either way.

Full test suite passes. Not verified: AC #1, #2 and #5 on IntelliJ IDEA 2026.3 (settings pages open with their section titles; opening the commit UI logs no missing VCS classes). AC #3 stays open because of the verifier limitation above.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1412
<!-- SECTION:FINAL_SUMMARY:END -->
