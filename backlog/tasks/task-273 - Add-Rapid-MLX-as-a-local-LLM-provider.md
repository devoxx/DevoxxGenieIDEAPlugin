---
id: TASK-273
title: Serve Nativ and Rapid-MLX from one generic OpenAI-compatible local provider
status: Done
assignee:
  - Claude
created_date: '2026-11-05T13:12:00Z'
updated_date: '2026-11-12T11:23:00Z'
labels: []
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1342
  - >-
    https://github.com/raullenchai/DevoxxGenieIDEAPlugin/compare/master...feat/rapid-mlx-provider
  - https://github.com/raullenchai/Rapid-MLX
documentation:
  - docs/superpowers/specs/2026-11-05-generic-local-providers-design.md
  - docs/superpowers/plans/2026-11-05-generic-local-providers.md
modified_files:
  - AGENTS.md
  - CLAUDE.md
  - README.md
  - docs/superpowers/plans/2026-11-05-generic-local-providers.md
  - docs/superpowers/specs/2026-11-05-generic-local-providers-design.md
  - docusaurus/docs/configuration/settings.md
  - docusaurus/docs/features/overview.md
  - docusaurus/docs/getting-started/faq.md
  - docusaurus/docs/getting-started/quick-start-local.md
  - docusaurus/docs/getting-started/why-devoxxgenie.md
  - docusaurus/docs/llm-providers/exo.md
  - docusaurus/docs/llm-providers/local-models.md
  - docusaurus/docs/llm-providers/overview.md
  - docusaurus/docusaurus.config.js
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java
  - src/main/java/com/devoxx/genie/chatmodel/catalog/CatalogDefaults.java
  - src/main/java/com/devoxx/genie/chatmodel/catalog/CatalogFields.java
  - src/main/java/com/devoxx/genie/chatmodel/catalog/ModelCatalogParser.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogParser.java
  - src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterDefinition.java
  - src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterRegistry.java
  - src/main/java/com/devoxx/genie/chatmodel/local/LocalLLMProviderUtil.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderDefinition.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistry.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/OpenAiCompatibleChatModelFactory.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/nativ/NativChatModelFactory.java
  - src/main/java/com/devoxx/genie/chatmodel/local/nativ/NativModelService.java
  - src/main/java/com/devoxx/genie/controller/ProjectContextController.java
  - src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java
  - src/main/java/com/devoxx/genie/model/nativ/NativModelEntryDTO.java
  - src/main/java/com/devoxx/genie/model/nativ/NativModelsResponseDTO.java
  - src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/ui/panel/LlmProviderPanel.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/ProviderEnabledResolver.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/ModelComboLoader.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java
  - src/main/resources/META-INF/plugin.xml
  - src/test/java/com/devoxx/genie/chatmodel/ChatModelFactoryProviderTest.java
  - src/test/java/com/devoxx/genie/chatmodel/catalog/ModelCatalogParserTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogParserTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterChatModelFactoryTest.java
  - src/test/java/com/devoxx/genie/chatmodel/local/LocalLLMProviderUtilTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/compatible/OpenAiCompatibleChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/nativ/NativChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/nativ/NativModelServiceTest.java
  - src/test/java/com/devoxx/genie/controller/ProjectContextControllerTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceLocalProviderTest.java
  - src/test/java/com/devoxx/genie/ui/settings/agent/ModelComboLoaderTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersRouterSettingsTest.java
priority: medium
type: feature
ordinal: 17000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Issue #1342 asks for a Rapid-MLX provider, and the author's fork was never opened as a PR, so we implement it and credit the author. Adding it the Nativ way would copy a factory, service and DTOs and touch about 15 files, so this follows the routers approach: one definition-driven local provider, with Nativ migrated and Rapid-MLX as the second definition. Jan, GPT4All, LLaMA.c++, LMStudio, Ollama and Exo are out of scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A typical OpenAI-compatible local server takes one provider enum line and one definition entry, enforced by a test
- [x] #2 Rapid-MLX is the last row of the Local settings tab, and is disabled by default with URL http://localhost:8000/v1/
- [x] #3 A disabled provider sends no requests and shows no notification
- [x] #4 Rapid-MLX's context window uses max_model_len, then context_window, then 8000, and embedding entries are left out while an empty capabilities list keeps the model
- [x] #5 A non-JSON reply on the provider's port is handled as not running, without an exception
- [ ] #6 Existing Nativ users keep their URL, enabled state, fallback context and selected model after upgrading
- [x] #7 After Apply, a changed URL is used without a restart and without a stale model list
- [ ] #8 Chat, streaming, agent and sub-agent paths use the configured URL, and agent settings load models off the EDT
- [x] #9 Router catalog behaviour is unchanged
- [x] #10 README and docs list Rapid-MLX at the bottom of the local providers and explain the 8000-8009 port behaviour
- [x] #11 The settings tabs read Local, Cloud Direct, Routers, Sharding and Custom OpenAI, with Exo's row on Sharding and Exo otherwise unchanged
- [x] #12 The Sharding tab lists vLLM, SGLang, llama.cpp RPC, LocalAI and GPUStack as grayed-out, display-only Coming soon rows, with no provider, saved state or requests behind them
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented via subagent-driven development on feat/rapid-mlx-provider, 300b618c..cd227cbe. Criteria 6 and 8 wait on the IDE and live-server check. The screenshots still need retaking, including a new Sharding image. Follow-ups: ParallelExploreToolExecutorTest leaks its own executor (flaky); sub-agents saved on a disabled provider can still send requests, for every provider.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Nativ and Rapid-MLX are now served by one definition-driven local provider (LocalProviderRegistry / LocalProviderDefinition / OpenAiCompatibleChatModelFactory), sharing the model catalog parser with the routers. Nativ's dedicated factory, model service and DTOs are removed and its saved settings migrate into the shared localProvider* state. Settings tabs are reorganised (Local, Cloud Direct, Routers, Sharding, Custom OpenAI) with Exo and grayed-out "coming soon" rows on Sharding. README, CLAUDE.md and docs are updated.

Rebase onto master (2026-11-12): master had meanwhile merged #1404, a dedicated RapidMLXChatModelFactory by the Rapid-MLX author. That design is superseded here: its factory, model service, DTOs, state fields and Constant were removed (unreleased, so no migration needed), its ChatModelProvider and ProviderEnabledResolver tests were ported to the generic API, and its docs note on API-key servers was kept. Full test suite passes.

Not verified: AC #6 (Nativ upgrade keeps URL, enabled state, fallback context and selected model) and AC #8 (chat, streaming, agent and sub-agent paths use the configured URL) still need a manual check in the IDE against live servers. Screenshots still need retaking, including a new Sharding image.

Follow-ups: TASK-274 (move GPT4All, LLaMA.c++ and Jan onto the generic provider); ParallelExploreToolExecutorTest leaks its own executor (flaky); sub-agents saved on a disabled provider can still send requests, for every provider.
<!-- SECTION:FINAL_SUMMARY:END -->
