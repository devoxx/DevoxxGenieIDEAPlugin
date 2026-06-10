---
id: TASK-238
title: Fix PyCharm EDT exception when opening DevoxxGenie settings
status: Done
assignee: []
created_date: '2026-06-10 20:23'
updated_date: '2026-06-10 21:20'
labels:
  - bug
  - pycharm
  - settings
  - edt
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/util/SettingsDialogUtil.java
  - src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowFactory.java
modified_files:
  - src/main/java/com/devoxx/genie/ui/util/SettingsDialogUtil.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - src/test/java/com/devoxx/genie/ApiCompatibilityTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Opening DevoxxGenie settings from the tool window in PyCharm can throw `java.lang.IllegalStateException: This method is forbidden on EDT because it does not pump the event queue`. The observed stack starts from `DevoxxGenieToolWindowFactory$OpenSettingsAction.actionPerformed`, calls `SettingsDialogUtil.showSettingsDialog`, then IntelliJ builds settings configurables and PyCharm's Flask console options call `PythonPackageManager.Companion.forSdk(...)` via `FlaskUtilsKt.isFlaskInstalled(...)`, which is forbidden on the EDT. Investigate how the plugin opens settings and adjust the flow so opening DevoxxGenie settings does not synchronously trigger forbidden PyCharm package-manager work on the event dispatch thread.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Opening DevoxxGenie settings from the tool window in PyCharm no longer throws the EDT `IllegalStateException`.
- [x] #2 The settings action still opens the intended DevoxxGenie settings page in IntelliJ-based IDEs where Python/PyCharm configurables are present and where they are absent.
- [x] #3 The fix avoids blocking or long-running work on the EDT and uses an IntelliJ Platform-supported threading/progress approach where needed.
- [x] #4 A regression test or documented manual verification covers the PyCharm settings-opening path.
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Root cause (verified against IntelliJ 2025.3.3 bytecode): every synchronous `ShowSettingsUtil.showSettingsDialog(...)` overload (display-name, class, or predicate based) collects ALL configurable groups on the calling thread. In PyCharm, building the group tree runs `FlaskConsoleOptionsProvider.isApplicableTo()` → `PythonPackageManager.forSdk()` → `runBlockingCancellable`, which asserts a background thread and throws on the EDT. Intermittent because the Python package manager is cached per SDK after the first call.

Fix mirrors the platform's own ShowSettingsAction: `SettingsDialogUtil` now collects configurable groups via `ShowSettingsUtilImpl.getConfigurableGroups()` on a pooled thread, resolves the target configurable with `ConfigurableVisitor`, then shows the dialog on the EDT via the non-deprecated, non-internal `ShowSettingsUtilImpl.showSettings()`. Added a display-name variant and a disposed-project guard. Routed the "Open Web search settings" / "Open RAG settings" hyperlinks in `AgentSettingsComponent` (same EDT bug) through the utility. Regression coverage: `ApiCompatibilityTest.settingsDialogIsNeverOpenedSynchronouslyOnTheEdt` scans src/main and fails if any code calls `ShowSettingsUtil.getInstance().showSettingsDialog` directly (TDD: red before fix, green after). Full suite: 2955 tests, only pre-existing unrelated `ReadFileToolExecutorTest` failure (fails on clean tree too).

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1108
<!-- SECTION:FINAL_SUMMARY:END -->
