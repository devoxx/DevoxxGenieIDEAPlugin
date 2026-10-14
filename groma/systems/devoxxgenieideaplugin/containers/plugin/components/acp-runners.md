---
type: C4 Component
title: ACP runners
status: stable
groma:
  id: acp-runners
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/acprunners/AcpRunnersChatModelFactory.java
      symbol: AcpRunnersChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/spec/AcpToolConfig.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/ClientCapabilities.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/ClientInfo.java
      symbol: ClientInfo
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/ContentBlock.java
      symbol: ContentBlock
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/InitializeParams.java
      symbol: InitializeParams
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/InitializeResult.java
      symbol: InitializeResult
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/SessionNewParams.java
      symbol: SessionNewParams
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/SessionNewResult.java
      symbol: SessionNewResult
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/SessionPromptParams.java
      symbol: SessionPromptParams
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/model/SessionUpdateParams.java
      symbol: SessionUpdateParams
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/AcpClient.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/AcpTransport.java
      symbol: AcpTransport
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/AgentRequestHandler.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/AcpConnectionException.java
      symbol: AcpConnectionException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/AcpException.java
      symbol: AcpException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/AcpRequestException.java
      symbol: AcpRequestException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/AcpSessionException.java
      symbol: AcpSessionException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/AcpTimeoutException.java
      symbol: AcpTimeoutException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/FsReadException.java
      symbol: FsReadException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/FsWriteException.java
      symbol: FsWriteException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/exception/TerminalCreateException.java
      symbol: TerminalCreateException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/acp/protocol/JsonRpcMessage.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/AcpPromptStrategy.java
      symbol: AcpPromptStrategy
  group: External agent runners
  technology: ACP (JSON-RPC over stdio)
description: Talks to external coding agents over the Agent Client Protocol
---

A JSON-RPC client over the agent process's stdio that performs the ACP handshake, creates sessions, sends prompts, streams responses and answers the agent's file read/write, permission and terminal requests.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/AcpPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/AcpPromptStrategy.java) | [src/main/java/com/devoxx/genie/service/acp/protocol/AcpClient.java](../../../../../../src/main/java/com/devoxx/genie/service/acp/protocol/AcpClient.java) | Prompts ACP agent | Java call |
| [src/main/java/com/devoxx/genie/service/acp/protocol/AcpTransport.java](../../../../../../src/main/java/com/devoxx/genie/service/acp/protocol/AcpTransport.java) | [coding-agent-clis](../../../../../externals/coding-agent-clis.md) | Speaks ACP | JSON-RPC over stdio |
