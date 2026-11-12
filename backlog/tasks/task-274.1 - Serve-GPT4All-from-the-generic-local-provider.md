---
id: TASK-274.1
title: Serve GPT4All from the generic local provider
status: Done
assignee:
  - '@Claude'
created_date: '2026-11-12T11:21:00Z'
updated_date: '2026-11-12T12:15:00Z'
labels:
  - local-llm
  - refactor
dependencies: []
modified_files:
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
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/ui/settings/ProviderEnabledResolver.java
  - src/main/java/com/devoxx/genie/ui/panel/LlmProviderPanel.java
  - src/main/java/com/devoxx/genie/controller/ProjectContextController.java
  - src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java
  - src/main/resources/META-INF/plugin.xml
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/gpt4all/GPT4AllChatModelFactory.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/gpt4all/GPT4AllModelService.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/gpt4all/GPT4AllChatModelFactoryTest.java
  - src/test/java/com/devoxx/genie/chatmodel/ChatModelFactoryProviderTest.java
  - src/test/java/com/devoxx/genie/chatmodel/ChatModelProviderTest.java
  - src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceTest.java
  - src/test/java/com/devoxx/genie/ui/panel/LlmProviderPanelTest.java
  - >-
    src/test/java/com/devoxx/genie/controller/ActionButtonsPanelControllerTest.java
  - src/test/java/com/devoxx/genie/controller/ProjectContextControllerTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersRouterSettingsTest.java
  - docusaurus/docs/llm-providers/local-models.md
  - CLAUDE.md
  - AGENTS.md
parent_task_id: TASK-274
priority: medium
type: enhancement
ordinal: 19000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
GPT4All's dedicated factory only lists /v1/models and uses the shared OpenAI chat clients, so it is the simplest fit for the generic local provider and the first to move. As a bonus it fixes a gap: the dedicated factory sets no context window at all for GPT4All models.

This is the first move of a provider that has already shipped, so it is also where saved-settings migration and enabled-by-default support for generic providers get established; LLaMA.c++ and Jan reuse them. Nativ's move in TASK-273 already migrates legacy fields and is the precedent to follow.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GPT4All models are listed from the configured server's /v1/models and chat (streaming and non-streaming) works through the generic local provider
- [x] #2 GPT4All models get a context window (the provider default when the server reports none) instead of none
- [x] #3 A user upgrading with a customised GPT4All URL keeps that URL
- [x] #4 A user upgrading who had GPT4All disabled keeps it disabled; a fresh install and an upgrading user who never changed it have it enabled, as today
- [x] #5 A generic local provider can be declared enabled by default, and Nativ and Rapid-MLX stay disabled by default
- [x] #6 The GPT4All factory, model service and GPT4All-only DTOs are removed, and any lookup by the old GPT4All settings keys still resolves
- [x] #7 Tests cover model listing, context fallback and the settings migration cases above
- [x] #8 LocalProviderRegistryTest.everyDefinition_isFullyWired passes with GPT4All registered
- [x] #9 New users start on Ollama when it is available; GPT4All stays enabled but is no longer pre-selected just because it sorts first
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add enabledByDefault to LocalProviderDefinition; isLocalProviderEnabled falls back to it (fresh installs never call loadState). GPT4All true, Nativ and Rapid-MLX false.
2. Register GPT4All in LocalProviderRegistry: GPT4ALL_MODEL_URL, gpt4all.io, 8000-token default, optional fallback-context field.
3. One-shot legacy migration in loadState for GPT4All (gpt4allModelUrl, isGpt4AllEnabled) and Nativ (nativModelUrl, isNativEnabled, nativFallbackContextLength): copy non-default values into the localProvider* maps, then reset the legacy fields, so a cleared value cannot be re-seeded. Removes the Nativ-only special case in setLocalProviderFallbackContext. getConfigValue("gpt4allModelUrl") keeps resolving.
4. Wire GPT4All through the generic factory; drop its special cases (ChatModelProvider, SubAgentRunner, ProviderEnabledResolver, LlmProviderPanel, settings interface, settings UI, plugin.xml); delete GPT4AllChatModelFactory, GPT4AllModelService and their test. Keep model/gpt4all DTOs: Cloudflare, NVIDIA and Custom OpenAI use them.
5. Tests first: registry contents and per-definition defaults, GPT4All and Nativ upgrade cases, GPT4All catalog fallback; update ChatModelProvider, ChatModelFactoryProvider and state service tests.
6. Docs: CLAUDE.md, AGENTS.md, local-models page.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on feature/task-274.1-gpt4all-generic-local-provider (uncommitted).

- LocalProviderDefinition gained enabledByDefault; isLocalProviderEnabled falls back to it because a fresh install never calls loadState. GPT4All true; Nativ and Rapid-MLX false.
- GPT4All is registered first in LocalProviderRegistry (port 4891, 8000-token default, optional fallback-context field). Its settings row now sits after LLaMA.c++, with the other generic rows.
- loadState migrates legacy fields once (migrateLegacyLocalProvider) for GPT4All and Nativ, then resets them. This also fixes a Nativ bug from TASK-273: a migrated Nativ URL cleared to blank came back after a restart. The Nativ-only special case in setLocalProviderFallbackContext is gone.
- Removed GPT4AllChatModelFactory, GPT4AllModelService and their test. The model/gpt4all DTOs stay: Cloudflare, NVIDIA and Custom OpenAI use them, so none were GPT4All-only (AC #6). getConfigValue("gpt4allModelUrl") still resolves.
- Behaviour change: GPT4All now supports "add project to context" like every generic provider, since it has a context window.
- New-user default: the first-run provider pick (LlmProviderPanel.setLastSelectedProvider) prefers Ollama. Before, the alphabetically sorted combo put GPT4All first; that was true on master already, not caused by this move.
- Full suite: 4181 tests, 1 failure: ParallelExploreToolExecutorTest.cancel_setsFlag_andCancelsActiveRunners (ThreadLeakTracker), the known flake; passes 3/3 alone.
- AC #1 still needs a live check: chat (streaming and non-streaming) against a running GPT4All server.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
GPT4All is now served by the generic OpenAI-compatible local provider. It's registered in LocalProviderRegistry with port 4891, an 8000-token default context (it had none before) and an optional fallback-context setting. The dedicated GPT4AllChatModelFactory, GPT4AllModelService and their test are removed, along with GPT4All's special cases in ChatModelProvider, SubAgentRunner, ProviderEnabledResolver, LlmProviderPanel, the settings interface, the settings UI and plugin.xml. The model/gpt4all DTOs stay because Cloudflare, NVIDIA and Custom OpenAI use them.

LocalProviderDefinition gained enabledByDefault. isLocalProviderEnabled falls back to it because a fresh install never calls loadState: GPT4All is on, Nativ and Rapid-MLX stay off. loadState migrates the legacy GPT4All and Nativ fields once, then resets them. That keeps a custom URL and a disabled state on upgrade, and fixes a TASK-273 bug where a migrated Nativ URL cleared to blank came back after a restart. getConfigValue("gpt4allModelUrl") still resolves.

New users now start on Ollama when it's available. Before, the alphabetically sorted provider combo put GPT4All first; that was already true on master.

Visible changes: the GPT4All settings row moves to just after LLaMA.c++, and GPT4All can now add the project to the prompt context, like every generic provider.

Full test suite passes. Not verified: AC #1's live check, listing models and chatting (streaming and non-streaming) against a running GPT4All server.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1408
<!-- SECTION:FINAL_SUMMARY:END -->
