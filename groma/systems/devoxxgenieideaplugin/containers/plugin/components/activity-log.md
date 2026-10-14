---
type: C4 Component
title: Activity log
status: stable
groma:
  id: activity-log
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/activity/ActivityMessage.java
      symbol: ActivityMessage
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/activity/ActivitySource.java
      symbol: ActivitySource
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/debug/RawTrafficType.java
      symbol: RawTrafficType
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/activity/ActivityLoggingMessage.java
      symbol: ActivityLoggingMessage
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/debug/RawTrafficListenerService.java
      symbol: RawTrafficListenerService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/debug/SecretRedactor.java
      symbol: SecretRedactor
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/debug/DebugSettingsComponent.java
      symbol: DebugSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/debug/DebugSettingsConfigurable.java
      symbol: DebugSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/window/AgentMcpLogToolWindowFactory.java
      symbol: AgentMcpLogToolWindowFactory
  group: Chat experience
description: DevoxxGenie Logs tool window for agent, MCP, RAG and raw LLM traffic
---

Collects activity messages published on the message bus (agent tool calls, MCP traffic, RAG events, the system prompt, and opt-in redacted raw request/response traffic) and shows them in a dedicated tool window for debugging.
