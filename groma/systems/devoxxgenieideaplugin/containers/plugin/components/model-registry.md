---
type: C4 Component
title: Model factory registry
status: stable
groma:
  id: model-registry
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/ChatModelFactory.java
      symbol: ChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java
      symbol: ChatModelFactoryProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java
      symbol: ChatModelProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/ThinkingSupport.java
      symbol: ThinkingSupport
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/CustomChatModel.java
      symbol: CustomChatModel
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/enumarations/AwsBedrockAuthMode.java
      symbol: AwsBedrockAuthMode
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/enumarations/ExecutionMode.java
      symbol: ExecutionMode
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/GenericOpenAIProvider.java
      symbol: GenericOpenAIProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/LanguageModel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/models/ModelConfig.java
      symbol: ModelConfig
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/models/ModelConfigEntry.java
      symbol: ModelConfigEntry
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/openrouter/Data.java
      symbol: Data
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/openrouter/Pricing.java
      symbol: Pricing
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/openrouter/ResponseDTO.java
      symbol: ResponseDTO
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/openrouter/TopProvider.java
      symbol: TopProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/LLMProviderService.java
      symbol: LLMProviderService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/models/LLMModelRegistryService.java
      symbol: LLMModelRegistryService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/models/ModelConfigService.java
      symbol: ModelConfigService
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/costsettings/LanguageModelCostSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/costsettings/LanguageModelCostSettingsConfigurable.java
      symbol: LanguageModelCostSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java
      symbol: LLMProvidersConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/llmconfig/LLMConfigSettingsComponent.java
      symbol: LLMConfigSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/llmconfig/LLMConfigSettingsConfigurable.java
      symbol: LLMConfigSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/DefaultLLMSettingsUtil.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/LanguageModelSelectionUtil.java
      symbol: LanguageModelSelectionUtil
  group: LLM providers
  technology: langchain4j
description: Maps providers to chat-model factories and knows each model's limits and price
---

Defines the `ChatModelFactory` contract and `ChatModelFactoryProvider` lookup by `ModelProvider`, the `LanguageModel` description, and the model catalogue: a built-in list overlaid with `models.json` fetched from the documentation site (cached 24 h). Includes the provider, LLM config and cost settings pages.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java) | [src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java) | Looks up factory | Java call |
| [src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java) | [src/main/java/com/devoxx/genie/chatmodel/cloud/openai/OpenAIChatModelFactory.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/cloud/openai/OpenAIChatModelFactory.java) | Creates cloud models | Java call |
| [src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java) | [src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java) | Creates local models | Java call |
| [src/main/java/com/devoxx/genie/service/models/LLMModelRegistryService.java](../../../../../../src/main/java/com/devoxx/genie/service/models/LLMModelRegistryService.java) | [src/main/java/com/devoxx/genie/service/models/ModelConfigService.java](../../../../../../src/main/java/com/devoxx/genie/service/models/ModelConfigService.java) | Overlays remote catalogue | Java call |
| [src/main/java/com/devoxx/genie/service/models/ModelConfigService.java](../../../../../../src/main/java/com/devoxx/genie/service/models/ModelConfigService.java) | [docusaurus/docusaurus.config.js](../../../../../../docusaurus/docusaurus.config.js) | Fetches model catalogue | HTTPS GET genie.devoxx.com/api/models.json |
