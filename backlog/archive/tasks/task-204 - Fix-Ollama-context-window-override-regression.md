---
id: TASK-204
title: Fix Ollama context window override regression
status: Done
assignee:
  - codex
created_date: '2026-03-09 12:20'
updated_date: '2026-03-09 12:37'
labels:
  - bug
  - ollama
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/979'
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/804'
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/936'
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaApiService.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Ollama issue #979 reports that DevoxxGenie always sends `options.num_ctx` on chat requests by copying the discovered model context window into runtime request parameters. This regressed after the fixes for #804 and #936: model discovery now prefers `parameters.num_ctx` when present, but the same discovered value is still treated as an explicit runtime override and forwarded through LangChain4j. As a result, models can ignore Ollama runtime defaults or environment-controlled context limits and allocate a larger KV cache than intended.

Implementation direction agreed on: add an Ollama-specific control in the LLM settings dialog that lets the user enable or disable request-time context-window overriding. When disabled, DevoxxGenie must not send `options.num_ctx` and should let Ollama apply its own runtime default/Modelfile behavior. When enabled, DevoxxGenie should send the intended `num_ctx` value explicitly. The implementation must keep UI/token-accounting behavior coherent and avoid silently conflating discovered model context with an explicit request override.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The LLM settings dialog exposes an Ollama-specific user control to enable or disable request-time context-window overriding.
- [x] #2 When the Ollama override setting is disabled, DevoxxGenie does not send `options.num_ctx` in chat requests for Ollama models.
- [x] #3 When the Ollama override setting is enabled, DevoxxGenie sends the intended `num_ctx` value for Ollama chat requests in both streaming and non-streaming paths.
- [x] #4 The implementation clearly separates discovered Ollama context length used for UI/token calculations from the explicit request override used to populate `num_ctx`.
- [x] #5 Regression tests cover the enabled and disabled setting states for both streaming and non-streaming Ollama chat models.
- [x] #6 Any user-facing Ollama setting labels, help text, or related documentation introduced by this change are updated in the same task.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add a persisted Ollama-specific boolean setting in `DevoxxGenieStateService` for request-time context-window overriding, defaulting to disabled.
2. Expose the toggle in the LLM settings dialog and wire `isModified`, `apply`, and `reset` handling in the LLM providers configurable.
3. Separate discovered context length from explicit request override in `CustomChatModel` and `ChatModelProvider`, keeping discovered values available for UI/token calculations while only populating the request override when the setting is enabled.
4. Update `OllamaChatModelFactory` to send `num_ctx` only when an explicit override is present, for both streaming and non-streaming builders.
5. Add regression tests covering enabled and disabled setting states in the provider and Ollama factory paths, then run the relevant test suite.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started implementation review on branch `fix/issue-979-ollama-context-window-override`. Confirmed no existing Ollama-specific request-time context override setting is present in the LLM settings UI or persisted state.

Implemented an Ollama-specific `Request Context Override` toggle in the LLM settings dialog, persisted via `DevoxxGenieStateService` and wired through configurable apply/reset/modified handling.

Separated discovered context length from explicit request override by keeping `CustomChatModel.contextWindow` for UI/token calculations and adding `contextWindowOverride` for request-time `num_ctx` injection.

Updated `ChatModelProvider` to populate the Ollama request override only when the new setting is enabled, and updated `OllamaChatModelFactory` to send `num_ctx` only from that explicit override in both streaming and non-streaming paths.

Added regression coverage in `ChatModelProviderTest`, `OllamaChatModelFactoryTest`, and `LLMProvidersConfigurableTest`. Focused verification passed with `./gradlew -q test --tests com.devoxx.genie.chatmodel.ChatModelProviderTest --tests com.devoxx.genie.chatmodel.local.ollama.OllamaChatModelFactoryTest --tests com.devoxx.genie.ui.settings.llm.LLMProvidersConfigurableTest`.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added an Ollama-specific `Request Context Override` setting to the Large Language Models settings panel and persisted it in `DevoxxGenieStateService`. The setting defaults to disabled so DevoxxGenie no longer sends `options.num_ctx` unless the user explicitly opts in.

To avoid conflating display metadata with runtime overrides, `CustomChatModel` now keeps discovered context length separately from a request-time override value. `ChatModelProvider` preserves the discovered context for token calculations/UI and only sets the override for Ollama when the new setting is enabled. `OllamaChatModelFactory` now injects `num_ctx` only from that explicit override for both chat and streaming builders.

Tests were expanded to cover the new setting path in the configurable, the provider mapping behavior, and Ollama builder serialization for enabled and disabled override states. Verified with `./gradlew -q test --tests com.devoxx.genie.chatmodel.ChatModelProviderTest --tests com.devoxx.genie.chatmodel.local.ollama.OllamaChatModelFactoryTest --tests com.devoxx.genie.ui.settings.llm.LLMProvidersConfigurableTest`.
<!-- SECTION:FINAL_SUMMARY:END -->
