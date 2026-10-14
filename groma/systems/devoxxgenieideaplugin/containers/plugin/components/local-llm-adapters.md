---
type: C4 Component
title: Local provider adapters
status: stable
groma:
  id: local-llm-adapters
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/customopenai/CustomOpenAIChatModelFactory.java
      symbol: CustomOpenAIChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/customopenai/CustomOpenAIContextWindow.java
      symbol: CustomOpenAIContextWindow
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/customopenai/CustomOpenAICost.java
      symbol: CustomOpenAICost
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/exo/ExoChatModelFactory.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/exo/ExoModelService.java
      symbol: ExoModelService
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/gpt4all/GPT4AllChatModelFactory.java
      symbol: GPT4AllChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/gpt4all/GPT4AllModelService.java
      symbol: GPT4AllModelService
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/jan/CompactJsonHttpClient.java
      symbol: CompactJsonHttpClient
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/jan/CompactJsonHttpClientBuilder.java
      symbol: CompactJsonHttpClientBuilder
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/jan/JanChatModelFactory.java
      symbol: JanChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/jan/JanModelService.java
      symbol: JanModelService
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/llamacpp/LlamaChatModelFactory.java
      symbol: LlamaChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/lmstudio/LMStudioChatModelFactory.java
      symbol: LMStudioChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/lmstudio/LMStudioModelService.java
      symbol: LMStudioModelService
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/LocalChatModelFactory.java
      symbol: LocalChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/LocalLLMProvider.java
      symbol: LocalLLMProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/LocalLLMProviderUtil.java
      symbol: LocalLLMProviderUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/nativ/NativChatModelFactory.java
      symbol: NativChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/nativ/NativModelService.java
      symbol: NativModelService
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaApiService.java
      symbol: OllamaApiService
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java
      symbol: OllamaChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaModelService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/exo/ExoModelDTO.java
      symbol: ExoModelDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/exo/ExoModelEntryDTO.java
      symbol: ExoModelEntryDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/gpt4all/Model.java
      symbol: Model
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/gpt4all/ModelPermission.java
      symbol: ModelPermission
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/gpt4all/ResponseDTO.java
      symbol: ResponseDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/jan/Data.java
      symbol: Data
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/jan/Metadata.java
      symbol: Metadata
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/jan/Parameters.java
      symbol: Parameters
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/jan/ResponseDTO.java
      symbol: ResponseDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/jan/Settings.java
      symbol: Settings
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/lmstudio/LMStudioModelDTO.java
      symbol: LMStudioModelDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/lmstudio/LMStudioModelEntryDTO.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/nativ/NativModelEntryDTO.java
      symbol: NativModelEntryDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/nativ/NativModelsResponseDTO.java
      symbol: NativModelsResponseDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ollama/OllamaModelDTO.java
      symbol: OllamaModelDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ollama/OllamaModelEntryDTO.java
      symbol: OllamaModelEntryDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ExoClusterPanel.java
  group: LLM providers
  technology: langchain4j, OkHttp
description: Chat-model factories and model discovery for local LLM runtimes
---

Factories for Ollama, LM Studio, GPT4All, Jan, llama.cpp, Exo, Nativ and custom OpenAI-compatible servers, plus services that list their installed models.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java) | [local-llm-runtimes](../../../../../externals/local-llm-runtimes.md) | Calls local LLMs | HTTP |
| [src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaModelService.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaModelService.java) | [local-llm-runtimes](../../../../../externals/local-llm-runtimes.md) | Lists installed models | HTTP |
