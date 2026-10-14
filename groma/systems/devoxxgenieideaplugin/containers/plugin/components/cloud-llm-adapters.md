---
type: C4 Component
title: Cloud provider adapters
status: stable
groma:
  id: cloud-llm-adapters
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/anthropic/AnthropicChatModelFactory.java
      symbol: AnthropicChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/anthropic/ClaudeModels.java
      symbol: ClaudeModels
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/azureopenai/AzureOpenAIChatModelFactory.java
      symbol: AzureOpenAIChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockAuthResolver.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockModelFactory.java
      symbol: BedrockModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockService.java
      symbol: BedrockService
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareChatModelFactory.java
      symbol: CloudflareChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareCompatHttpClient.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareCompatRequestNormalizer.java
      symbol: CloudflareCompatRequestNormalizer
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareGatewayUrl.java
      symbol: CloudflareGatewayUrl
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareModelName.java
      symbol: CloudflareModelName
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/deepinfra/DeepInfraChatModelFactory.java
      symbol: DeepInfraChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/deepseek/DeepSeekChatModelFactory.java
      symbol: DeepSeekChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/glm/GLMChatModelFactory.java
      symbol: GLMChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/google/GoogleChatModelFactory.java
      symbol: GoogleChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/grok/GrokChatModelFactory.java
      symbol: GrokChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/groq/GroqChatModelFactory.java
      symbol: GroqChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/kimi/KimiChatModelFactory.java
      symbol: KimiChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/mistral/MistralChatModelFactory.java
      symbol: MistralChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/nvidia/NvidiaChatModelFactory.java
      symbol: NvidiaChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/openai/OpenAIChatModelFactory.java
      symbol: OpenAIChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/openrouter/OpenRouterChatModelFactory.java
      symbol: OpenRouterChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/cloud/openrouter/OpenRouterService.java
      symbol: OpenRouterService
  group: LLM providers
  technology: langchain4j, AWS SDK
description: One chat-model factory per hosted LLM provider
---

Factories that build langchain4j chat and streaming models for each cloud provider, with provider quirks such as Bedrock authentication, OpenRouter model listing and the Cloudflare request normaliser.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/chatmodel/cloud/openai/OpenAIChatModelFactory.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/cloud/openai/OpenAIChatModelFactory.java) | [cloud-llm-providers](../../../../../externals/cloud-llm-providers.md) | Calls hosted LLMs | HTTPS |
| [src/main/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockModelFactory.java](../../../../../../src/main/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockModelFactory.java) | [cloud-llm-providers](../../../../../externals/cloud-llm-providers.md) | Calls Bedrock models | AWS SDK |
