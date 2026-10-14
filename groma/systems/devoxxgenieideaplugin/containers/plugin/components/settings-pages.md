---
type: C4 Component
title: General settings pages
status: stable
groma:
  id: settings-pages
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/AbstractSettingsComponent.java
      symbol: AbstractSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsConfigurable.java
      symbol: AgentSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/agent/ToolDescriptionEditorDialog.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/general/GeneralSettingsComponent.java
      symbol: GeneralSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/general/GeneralSettingsConfigurable.java
      symbol: GeneralSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/prompt/KeyboardShortcutPanel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/prompt/PromptSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/prompt/PromptSettingsConfigurable.java
      symbol: PromptSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/runner/CliTestErrorResolver.java
      symbol: CliTestErrorResolver
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/runner/RunnerSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/runner/RunnerSettingsConfigurable.java
      symbol: RunnerSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/SettingsComponent.java
      symbol: SettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/spec/SpecSettingsComponent.java
      symbol: SpecSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/spec/SpecSettingsConfigurable.java
      symbol: SpecSettingsConfigurable
  group: Settings and platform
  technology: Swing, IntelliJ Configurable
description: Settings pages that are not owned by a feature component
---

The shared settings component base plus general, prompt/shortcut, agent (including editable tool descriptions), runner and spec settings pages.
