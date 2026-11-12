---
id: TASK-274.3
title: Serve Jan from the generic local provider
status: Done
assignee:
  - '@Claude'
created_date: '2026-11-12T11:22:00Z'
updated_date: '2026-11-12T12:51:00Z'
labels:
  - local-llm
  - refactor
dependencies:
  - TASK-274.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1051
modified_files:
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/jan/CompactJsonHttpClient.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/CompactJsonHttpClient.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/jan/CompactJsonHttpClientBuilder.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/CompactJsonHttpClientBuilder.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/jan/CompactJsonHttpClientTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/compatible/CompactJsonHttpClientTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/compatible/OpenAiCompatibleChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistryTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceLocalProviderTest.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderDefinition.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistry.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/OpenAiCompatibleChatModelFactory.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java
  - src/main/java/com/devoxx/genie/ui/settings/ProviderEnabledResolver.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/ui/panel/LlmProviderPanel.java
  - src/main/java/com/devoxx/genie/controller/ProjectContextController.java
  - src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java
  - src/main/resources/META-INF/plugin.xml
  - src/main/java/com/devoxx/genie/chatmodel/local/jan/JanChatModelFactory.java
  - src/main/java/com/devoxx/genie/chatmodel/local/jan/JanModelService.java
  - src/main/java/com/devoxx/genie/model/jan/Data.java
  - src/main/java/com/devoxx/genie/model/jan/Metadata.java
  - src/main/java/com/devoxx/genie/model/jan/Parameters.java
  - src/main/java/com/devoxx/genie/model/jan/ResponseDTO.java
  - src/main/java/com/devoxx/genie/model/jan/Settings.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/jan/JanChatModelFactoryTest.java
  - src/test/java/com/devoxx/genie/service/jan/BaseIntellijTest.java
  - src/test/java/com/devoxx/genie/service/jan/JanServiceTest.java
  - src/test/java/com/devoxx/genie/chatmodel/ChatModelFactoryProviderTest.java
  - src/test/java/com/devoxx/genie/ui/panel/LlmProviderPanelTest.java
  - src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersRouterSettingsTest.java
  - docusaurus/docs/llm-providers/local-models.md
  - CLAUDE.md
  - AGENTS.md
parent_task_id: TASK-274
priority: low
type: enhancement
ordinal: 21000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Jan's dedicated factory lists an OpenAI-compatible /models catalog, so it can move to the generic local provider, but it has one server quirk the generic provider cannot express yet: Jan v0.8.0's bundled llama.cpp server hangs on multi-line JSON request bodies, which is what langchain4j's OpenAI client sends. The dedicated factory works around this by compacting request bodies (issue #1051). Generic local providers need a way to carry such a per-provider HTTP quirk before Jan can move without regressing that fix.

Jan reports a model's context length either at the top level or nested under its settings, and may omit a display name; the dedicated factory handles both and the move must keep that behaviour.

Depends on the GPT4All subtask, which establishes settings migration and enabled-by-default support for released providers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Jan models are listed and chat works through the generic local provider in streaming and non-streaming modes
- [x] #2 Requests to Jan are still sent as single-line JSON, so the issue #1051 hang does not return
- [x] #3 The context window is read from the nested settings value, then the top-level value, then the provider default
- [x] #4 A model without a display name is shown by its id
- [x] #5 Upgrading users keep a customised Jan URL and their enabled/disabled choice; Jan stays enabled by default
- [x] #6 The Jan factory, model service and Jan-only DTOs are removed, and any lookup by the old Jan settings keys still resolves
- [x] #7 Tests cover single-line request bodies, context resolution, display-name fallback and the settings migration cases above
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add compactJsonRequestBodies to LocalProviderDefinition; OpenAiCompatibleChatModelFactory wraps its HTTP client builder in CompactJsonHttpClientBuilder when it's set. Move CompactJsonHttpClient, CompactJsonHttpClientBuilder and their test from local/jan to local/compatible unchanged.
2. Confirm Jan's /v1/models format against a running Jan (the user starts its local API server), then register Jan: URL http://localhost:1337/v1/, enabled by default, compact JSON on, context from settings.ctx_len then ctx_len then 8000, display name from name with the id as fallback (shortened after a slash, like the other generic providers).
3. Migrate legacy janModelUrl and isJanEnabled once through migrateLegacyLocalProvider; getConfigValue("janModelUrl") keeps resolving.
4. Remove JanChatModelFactory, JanModelService, the model/jan DTOs, JanChatModelFactoryTest, JanServiceTest and BaseIntellijTest, Jan's settings fields and UI row, its ProviderEnabledResolver case and plugin.xml service.
5. Tests first: compact JSON applied only when the flag is set (streaming and non-streaming), context order, display-name fallback, migration cases.
6. Docs: local-models Jan section, CLAUDE.md, AGENTS.md. Full suite.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on feature/task-274.3-jan-generic-local-provider (uncommitted).

- Live check against Jan 0.8.8 (Local API Server on 127.0.0.1:1337): /v1/models entries carry only id, object, created and owned_by, with no ctx_len, settings or name. Ids mix file names, Hugging Face repo ids and a hash. The dummy key "na" is accepted, and /models also answers. Chat with Llama-3.2-1B answered in under a second for both single-line and multi-line JSON bodies, so 0.8.8 no longer has the #1051 hang (reported against 0.8.0); the workaround stays for older versions.
- LocalProviderDefinition gained compactJsonRequestBodies. OpenAiCompatibleChatModelFactory overrides resolveHttpClientBuilder to wrap the builder in CompactJsonHttpClientBuilder when it's set; chat and streaming models both use that builder. CompactJsonHttpClient, its builder and its test moved unchanged from local/jan to local/compatible.
- Jan is registered first in LocalProviderRegistry, which keeps master's Local tab order (Jan, LLaMA.c++, GPT4All, Nativ, Rapid-MLX). Settings: enabled by default, compact JSON on, context from settings.ctx_len then ctx_len then the fallback or 8000, display name from name with the id as fallback. Added a fallback-context setting because 0.8.x reports no context.
- Legacy janModelUrl and isJanEnabled migrate once; getConfigValue("janModelUrl") resolves to the generic URL.
- Removed JanChatModelFactory, JanModelService, the five model/jan DTOs, JanChatModelFactoryTest, JanServiceTest and BaseIntellijTest, Jan's settings fields and UI row, and its cases in ProviderEnabledResolver, SubAgentRunner, LlmProviderPanel, ProjectContextController and plugin.xml.
- Display-name change, approved: an id with a slash and no name is shortened after the slash, like the other generic providers (bartowski/Llama-3.2-1B-Instruct-GGUF shows as Llama-3.2-1B-Instruct-GGUF).
- Full suite passes.
- AC #1 still needs a check in the IDE: Jan models listed, and streaming and non-streaming chat through DevoxxGenie.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Jan is now served by the generic OpenAI-compatible local provider, the last of TASK-274's three moves.

The #1051 workaround moved with it. LocalProviderDefinition gained compactJsonRequestBodies, and when it's set OpenAiCompatibleChatModelFactory wraps its HTTP client builder in CompactJsonHttpClientBuilder. Chat and streaming models share that builder. CompactJsonHttpClient, its builder and its test moved unchanged from local/jan to local/compatible. Only Jan sets the flag.

Checked against Jan 0.8.8: /v1/models lists only id, object, created and owned_by. Jan accepts the dummy key "na", and in this version multi-line JSON bodies no longer hang (#1051 was reported against 0.8.0); the workaround stays for older versions. Jan's definition reads settings.ctx_len then ctx_len (sent by older Jan versions), then a new optional fallback-context setting, then 8000. The display name comes from name, falling back to the id, shortened after a slash like the other generic providers. Jan is first in the registry, which keeps master's Local tab order.

Legacy janModelUrl and isJanEnabled migrate once, and getConfigValue("janModelUrl") resolves to the generic URL. Removed JanChatModelFactory, JanModelService, the five model/jan DTOs and their tests, including BaseIntellijTest, which only the Jan test used, and Jan's special cases.

Full test suite passes. Not verified: AC #1, Jan models listed and streaming and non-streaming chat through DevoxxGenie in the IDE.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1410
<!-- SECTION:FINAL_SUMMARY:END -->
