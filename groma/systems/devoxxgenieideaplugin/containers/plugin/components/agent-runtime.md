---
type: C4 Component
title: Agent runtime
status: stable
groma:
  id: agent-runtime
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/agent/AgentMessage.java
      symbol: AgentMessage
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/agent/AgentType.java
      symbol: AgentType
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/agent/SubAgentConfig.java
      symbol: SubAgentConfig
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/AgentApprovalService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/AgentChangedFilesPublisher.java
      symbol: AgentChangedFilesPublisher
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/AgentFileChangeTracker.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
      symbol: AgentToolProviderFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/CommandBlacklist.java
      symbol: CommandBlacklist
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/ToolErrorRecovery.java
      symbol: ToolErrorRecovery
  group: Agent mode
  technology: langchain4j ToolProvider
description: Builds the agent tool chain and enforces loop limits and approvals
---

`AgentToolProviderFactory` composes built-in, MCP and skill tools behind `AgentLoopTracker` (global tool-call limit, cancellation) and `AgentApprovalProvider` (auto-approve read-only tools, prompt for writes, command blacklist). Also tracks changed files for diff review and runs read-only sub-agents for parallel exploration.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java) | [src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java) | Registers built-in tools | langchain4j ToolProvider |
| [src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java) | [src/main/java/com/devoxx/genie/service/mcp/MCPExecutionService.java](../../../../../../src/main/java/com/devoxx/genie/service/mcp/MCPExecutionService.java) | Adds MCP tools | langchain4j ToolProvider |
| [src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java) | [src/main/java/com/devoxx/genie/service/skill/SkillRegistry.java](../../../../../../src/main/java/com/devoxx/genie/service/skill/SkillRegistry.java) | Adds skills | Java call |
