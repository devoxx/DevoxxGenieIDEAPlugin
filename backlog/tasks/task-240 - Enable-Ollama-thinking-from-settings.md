---
id: TASK-240
title: Enable Ollama thinking from settings
status: In Progress
assignee: []
created_date: '2026-07-01 14:48'
updated_date: '2026-07-02 09:35'
labels:
  - settings
  - ollama
  - langchain4j
dependencies: []
modified_files:
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/LLMProvidersConfigurable.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/streaming/ThinkingResponseFormatter.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/MessagePair.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/theme/DevoxxGenieTheme.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurableTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandlerTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/response/streaming/TestStreamingResponseHandler.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategyTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategyTest.java
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModelThinkingTest.kt
priority: medium
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow users to opt into Ollama reasoning/thinking output from the LLM Providers settings, near the existing stream mode setting. When disabled, behavior must remain unchanged. When enabled, DevoxxGenie should request Ollama thinking through LangChain4j and make returned thinking visible in responses without losing normal answer text or token metrics.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A persisted settings checkbox lets users enable or disable Ollama thinking, defaulting to disabled for existing behavior.
- [x] #2 Settings modified/apply/reset logic correctly detects, saves, and restores the Ollama thinking setting.
- [x] #3 Ollama chat and streaming model creation enable LangChain4j thinking only when the setting is enabled.
- [x] #4 Streaming Ollama thinking tokens are captured and rendered in the response without replacing the final answer text.
- [x] #5 Automated tests cover settings persistence, Ollama model wiring, and streaming thinking handling.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented with TDD. Important follow-up from manual test: normal streaming chat goes through LangChain4j AiServices TokenStream, so StreamingPromptStrategy must subscribe to onPartialThinking(handler::onPartialThinking); direct handler support alone is insufficient. Verification: full ./gradlew test passed before the subscription patch; after patch, reran affected test classes successfully.

Updated the thinking renderer from a generic details block to a visible Markdown callout: `> 🧠 **Thinking**`, followed by the final answer. This makes the reasoning visually distinct in the response UI. Verification after the style change: affected tests passed.

Updated the Ollama thinking UI refinement: streaming thinking is now encoded with internal thinking markers, split into `thinkingMarkdown` in the Compose view model, and rendered as its own theme-aware `ThinkingBubble` before the final assistant response bubble. Added targeted regression coverage for marker formatting and view-model splitting.

Fixed the non-streaming Ollama thinking gap: the normal no-tool non-streaming path now calls `ChatModel.chat(...)` directly instead of `AiServices` returning only a `String`, preserving `AiMessage.thinking()`. `NonStreamingPromptStrategy` formats returned `AiMessage.thinking()` with the same internal markers used by streaming so the Compose thinking bubble appears when streaming is off. Also refactored `ThinkingBubble` to render its content as Markdown.

Scope expanded beyond Ollama: the setting was renamed from ollamaThinkingEnabled to showThinkingEnabled (checkbox label "Show Thinking") and wired into all OpenAI-compatible factories via new ThinkingSupport helper — LocalChatModelFactory base (LMStudio, Jan, GPT4All, Exo), llama.cpp, CustomOpenAI, OpenAI, DeepSeek, DeepInfra, OpenRouter, Groq, Grok, GLM, Kimi, Nvidia — plus Mistral (returnThinking on MistralAi builders). The flag is client-side only (langchain4j parses the reasoning_content response field), so it is a harmless no-op for providers/models that emit no reasoning. Not covered: Anthropic/Gemini/Bedrock (extended thinking there must be enabled with a token budget → cost implications; follow-up task) and Azure OpenAI (azure module lacks returnThinking). Known limitation: langchain4j 1.17.1 parses only reasoning_content, so OpenRouter/Groq (which use a 'reasoning' field) won't show thinking yet.

Also fixed: non-streaming prompts with agent/MCP tools dropped thinking because the AiServices Assistant interface returned String (langchain4j discards AiMessage.thinking() for String return types). Assistant.chat() now returns AiMessage.
<!-- SECTION:NOTES:END -->
