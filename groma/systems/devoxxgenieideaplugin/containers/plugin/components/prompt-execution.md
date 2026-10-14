---
type: C4 Component
title: Prompt execution
status: stable
groma:
  id: prompt-execution
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/controller/listener/PromptExecutionListener.java
      symbol: PromptExecutionListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/controller/listener/TokenCalculationListener.java
      symbol: TokenCalculationListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/controller/ProjectContextController.java
      symbol: ProjectContextController
    - scanner: java
      file: src/main/java/com/devoxx/genie/controller/PromptExecutionController.java
      symbol: PromptExecutionController
    - scanner: java
      file: src/main/java/com/devoxx/genie/controller/TokenCalculationController.java
      symbol: TokenCalculationController
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ChatContextParameters.java
      symbol: ChatContextParameters
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/request/ChatMessageContext.java
      symbol: ChatMessageContext
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/request/EditorInfo.java
      symbol: EditorInfo
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/request/SemanticFile.java
      symbol: SemanticFile
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/MessageCreationService.java
      symbol: MessageCreationService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/cancellation/PromptCancellationService.java
      symbol: PromptCancellationService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/ExecutionException.java
      symbol: ExecutionException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/MemoryException.java
      symbol: MemoryException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/ModelException.java
      symbol: ModelException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/PromptErrorHandler.java
      symbol: PromptErrorHandler
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/PromptException.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/ProviderErrorTranslator.java
      symbol: ProviderErrorTranslator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/StreamingException.java
      symbol: StreamingException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/error/WebSearchException.java
      symbol: WebSearchException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/PromptExecutionService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/response/streaming/ThinkingResponseFormatter.java
      symbol: ThinkingResponseFormatter
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/result/PromptResult.java
      symbol: PromptResult
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/AbstractPromptExecutionStrategy.java
      symbol: AbstractPromptExecutionStrategy
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
      symbol: NonStreamingPromptStrategy
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategy.java
      symbol: PromptExecutionStrategy
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategyFactory.java
      symbol: PromptExecutionStrategyFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/threading/PromptTask.java
      symbol: PromptTask
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/threading/PromptTaskTracker.java
      symbol: PromptTaskTracker
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/threading/ThreadPoolManager.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/threading/ThreadPoolShutdownManager.java
      symbol: ThreadPoolShutdownManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/ChatMessageContextUtil.java
      symbol: ChatMessageContextUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/ChatService.java
      symbol: ChatService
  group: Prompt pipeline
  technology: langchain4j AiServices
description: Turns a submitted prompt into an LLM call and streams the result back
---

The controllers behind the action buttons build a `ChatMessageContext` (prompt, model, context files, editor selection, token budget). `PromptExecutionService` picks a strategy: streaming, non-streaming, web search, CLI runner or ACP runner. LLM strategies assemble the system/user message (`MessageCreationService`, including DEVOXXGENIE.md and passive RAG context), attach the agent or MCP tool provider, run on the prompt thread pool and forward tokens and results to the Conversation view. Also handles cancellation, provider error translation and token usage.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java](../../../../../../src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java) | [src/main/java/com/devoxx/genie/controller/ProjectContextController.java](../../../../../../src/main/java/com/devoxx/genie/controller/ProjectContextController.java) | Adds project context | Java call |
| [src/main/java/com/devoxx/genie/controller/ProjectContextController.java](../../../../../../src/main/java/com/devoxx/genie/controller/ProjectContextController.java) | [src/main/java/com/devoxx/genie/service/ProjectContentService.java](../../../../../../src/main/java/com/devoxx/genie/service/ProjectContentService.java) | Collects project content | Java call |
| [src/main/java/com/devoxx/genie/service/prompt/PromptExecutionService.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/PromptExecutionService.java) | [src/main/java/com/devoxx/genie/service/prompt/command/PromptCommandProcessor.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/command/PromptCommandProcessor.java) | Expands slash commands | Java call |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategyFactory.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategyFactory.java) | [src/main/java/com/devoxx/genie/service/prompt/strategy/WebSearchPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/WebSearchPromptStrategy.java) | Selects web search | Java call |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategyFactory.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategyFactory.java) | [src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java) | Selects CLI runner | Java call |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategyFactory.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/PromptExecutionStrategyFactory.java) | [src/main/java/com/devoxx/genie/service/prompt/strategy/AcpPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/AcpPromptStrategy.java) | Selects ACP runner | Java call |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java) | [src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java) | Attaches agent tools | Java call |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java) | [src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java) | Reads and updates memory | Java call |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java) | [src/main/java/com/devoxx/genie/service/prompt/steering/SteeringMessageInjector.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/steering/SteeringMessageInjector.java) | Injects steering messages | langchain4j chatRequestTransformer |
| [src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java) | [src/main/java/com/devoxx/genie/ui/panel/PromptOutputPanel.java](../../../../../../src/main/java/com/devoxx/genie/ui/panel/PromptOutputPanel.java) | Streams response into view | Java call on EDT |
| [src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java](../../../../../../src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java) | [src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java) | Resolves chat model | Java call |
| [src/main/java/com/devoxx/genie/service/MessageCreationService.java](../../../../../../src/main/java/com/devoxx/genie/service/MessageCreationService.java) | [src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java](../../../../../../src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java) | Adds semantic context | Java call |
| [src/main/java/com/devoxx/genie/service/ChatService.java](../../../../../../src/main/java/com/devoxx/genie/service/ChatService.java) | [src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java](../../../../../../src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java) | Persists conversations | Java call |
