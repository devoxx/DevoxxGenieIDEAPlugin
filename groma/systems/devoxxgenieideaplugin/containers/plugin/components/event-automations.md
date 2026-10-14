---
type: C4 Component
title: Event automations
status: stable
groma:
  id: event-automations
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/automation/AgentType.java
      symbol: AgentType
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/automation/EventAgentMapping.java
      symbol: EventAgentMapping
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/automation/EventAutomationSettings.java
      symbol: EventAutomationSettings
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/automation/EventContext.java
      symbol: EventContext
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/automation/IdeEventType.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/listeners/BuildCompilationListener.java
      symbol: BuildCompilationListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/listeners/FileEventListener.java
      symbol: FileEventListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/listeners/FileSaveListener.java
      symbol: FileSaveListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/listeners/ProcessExitListener.java
      symbol: ProcessExitListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/listeners/TestExecutionListener.java
      symbol: TestExecutionListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/listeners/VcsCheckinHandlerFactory.java
      symbol: VcsCheckinHandlerFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/listeners/VcsCommitListener.java
      symbol: VcsCommitListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/automation/PromptTemplateRenderer.java
      symbol: PromptTemplateRenderer
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/automation/EventAgentDialog.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/automation/EventAutomationSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/automation/EventAutomationSettingsConfigurable.java
      symbol: EventAutomationSettingsConfigurable
  technology: IntelliJ listeners
description: Turns IDE events into prompts sent to an agent
---

Listens for commits, file saves/opens, build results, test results and process crashes, renders the mapped prompt template and either submits it to the tool window or asks for confirmation. Includes its settings page.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java](../../../../../../src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java) | [src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java](../../../../../../src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java) | Submits automation prompts | IntelliJ message bus (PROMPT_SUBMISSION_TOPIC) |
