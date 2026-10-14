---
type: C4 Component
title: Docker Agentic Platform integration
status: stable
groma:
  id: agentic-platform-integration
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ap/ApAgent.java
      symbol: ApAgent
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ap/ApAuthMode.java
      symbol: ApAuthMode
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ap/ApProject.java
      symbol: ApProject
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ap/ApRunEvent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ap/ApRunHandle.java
      symbol: ApRunHandle
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ap/ApSession.java
      symbol: ApSession
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/ap/ApCliException.java
      symbol: ApCliException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/ap/ApCliService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/ap/ApProjectMatcher.java
      symbol: ApProjectMatcher
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ap/ApAgentDetailsDialog.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ap/ApAgentsTab.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ap/ApBrowserPanel.java
      symbol: ApBrowserPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ap/ApNewRunPanel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ap/ApPreviewRibbon.java
      symbol: ApPreviewRibbon
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ap/ApSessionsTab.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/ap/ApSettingsComponent.java
      symbol: ApSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/ap/ApSettingsConfigurable.java
      symbol: ApSettingsConfigurable
  group: External agent runners
  technology: ap CLI
description: Browse agents and sessions and start runs on Docker's Agentic Platform
---

Wraps the `ap` binary, matches platform projects to the open project and provides the agents, sessions and new-run panels plus its settings page.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/ap/ApCliService.java](../../../../../../src/main/java/com/devoxx/genie/service/ap/ApCliService.java) | [docker-agentic-platform](../../../../../externals/docker-agentic-platform.md) | Starts agent runs | ap CLI process |
