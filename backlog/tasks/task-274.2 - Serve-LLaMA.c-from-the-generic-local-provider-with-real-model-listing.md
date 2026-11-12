---
id: TASK-274.2
title: Serve LLaMA.c++ from the generic local provider with real model listing
status: Done
assignee:
  - '@Claude'
created_date: '2026-11-12T11:22:00Z'
updated_date: '2026-11-12T12:33:00Z'
labels:
  - local-llm
  - refactor
dependencies:
  - TASK-274.1
modified_files:
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/compatible/OpenAiCompatibleChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistryTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceLocalProviderTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/embedding/EmbeddingProviderSettingsTest.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistry.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/ui/settings/ProviderEnabledResolver.java
  - src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingProviderSettings.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/llamacpp/LlamaChatModelFactory.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/llamacpp/LlamaChatModelFactoryTest.java
  - src/test/java/com/devoxx/genie/chatmodel/ChatModelFactoryProviderTest.java
  - src/test/java/com/devoxx/genie/chatmodel/ChatModelProviderTest.java
  - src/test/java/com/devoxx/genie/ui/panel/LlmProviderPanelTest.java
  - src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceTest.java
  - src/test/java/com/devoxx/genie/controller/ProjectContextControllerTest.java
  - docusaurus/docs/llm-providers/local-models.md
  - CLAUDE.md
  - AGENTS.md
parent_task_id: TASK-274
priority: medium
type: enhancement
ordinal: 20000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The LLaMA.c++ provider never asks the server what it serves: it always shows one hard-coded placeholder model with an assumed 8k context window, whatever llama-server actually has loaded. llama-server exposes an OpenAI-compatible /v1/models, so moving it to the generic local provider gives users the real loaded model and its context size, and removes another dedicated factory.

The dedicated factory is also the only local one built outside the shared local base, so it does not use the HTTP/1.1 client the other local providers use; the move changes that and should be checked against a live server. How llama-server reports context length in /v1/models has not been verified yet; confirm it against a running server before relying on it.

Depends on the GPT4All subtask, which establishes settings migration and enabled-by-default support for released providers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The LLaMA.c++ model dropdown shows the model(s) llama-server reports from /v1/models instead of a fixed placeholder
- [x] #2 The model's context window comes from the server when it reports one, with the provider default otherwise
- [ ] #3 Chat works through the generic local provider in streaming and non-streaming modes, verified against a running llama-server
- [x] #4 Upgrading users keep a customised LLaMA.c++ URL and their enabled/disabled choice; LLaMA.c++ stays enabled by default
- [x] #5 The dedicated LLaMA.c++ factory is removed, and any lookup by the old LLaMA.c++ settings keys still resolves
- [x] #6 Tests cover model listing, context parsing and the settings migration cases above
- [x] #7 The docs describe that LLaMA.c++ now lists the server's models
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Verified against llama-server 0.4.1: /v1/models entries carry meta.n_ctx (the running context, from -c) and meta.n_ctx_train (the model's maximum); ids are the GGUF path unless --alias is set; /models and /v1/models both answer, so the default URL http://localhost:8080 stays.

1. Register LLaMA.c++ in LocalProviderRegistry: enabled by default, context from meta.n_ctx then meta.n_ctx_train then 8000, no fallback-context field. Definition order LLaMA.c++, GPT4All, Nativ, Rapid-MLX keeps the Local tab order.
2. Migrate legacy llamaCPPUrl and isLlamaCPPEnabled once through migrateLegacyLocalProvider (TASK-274.1).
3. EmbeddingProviderSettings reads llama.cpp's URL and enabled flag from the generic settings, so RAG on llama.cpp keeps working.
4. SubAgentRunner keeps resolving the saved name "LLaMA", now to the generic URL.
5. Remove LlamaChatModelFactory and its test, the settings fields and UI row, and the special cases in ChatModelProvider, ProviderEnabledResolver and the settings interface; LlmProviderPanel treats LLaMA.c++ as local.
6. LocalProviderRegistryTest: compare default host:port across enabled-by-default providers instead of raw URL strings (Nativ shares port 8080 but is off by default).
7. Tests first; docs (local-models Llamacpp section, CLAUDE.md, AGENTS.md); full suite.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on feature/task-274.2-llamacpp-generic-local-provider, stacked on #1408 (uncommitted).

- Live format check (llama-server 0.4.1, gemma-4-E2B-it Q4_K_M, -c 4096): /v1/models entries carry meta.n_ctx (4096, the running context) and meta.n_ctx_train (131072, the model's maximum). Requests over n_ctx fail, so the context is read from meta.n_ctx, then meta.n_ctx_train, then 8000. Without --alias the id is the GGUF path; the dropdown shows the file name. /models and /v1/models both answer, so the default URL http://localhost:8080 is unchanged.
- LLaMA.c++ is registered first in LocalProviderRegistry (enabled by default, no fallback-context field); the Local tab keeps its order.
- Legacy llamaCPPUrl and isLlamaCPPEnabled migrate once through migrateLegacyLocalProvider.
- EmbeddingProviderSettings now reads llama.cpp's URL and enabled flag from the generic settings, so RAG embeddings through llama.cpp keep working after the migration.
- A sub-agent saved under the enum name "LLaMA" resolves to the generic URL.
- Removed LlamaChatModelFactory and its test, the settings fields and UI row, and the special cases in ChatModelProvider, ProviderEnabledResolver and the settings interface.
- LocalProviderRegistryTest compares default host:port across enabled-by-default providers instead of raw URL strings (Nativ shares 8080 but is off by default).
- Behaviour changes: real models in the dropdown instead of "test-model", streaming chat (the old factory had none), "add project to context" support, and the "is not running" notice when llama-server is down.
- Full suite passes.
- AC #3 still needs a live check in the IDE: streaming and non-streaming chat against a running llama-server.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
LLaMA.c++ is now served by the generic OpenAI-compatible local provider and lists the models llama-server actually serves, instead of a hard-coded test-model.

Checked against llama-server 0.4.1: /v1/models entries carry meta.n_ctx (the context the server runs with, set by -c) and meta.n_ctx_train (the model's maximum). Requests over n_ctx fail, so the context window is read from meta.n_ctx, then meta.n_ctx_train, then 8000. Without --alias the id is the GGUF path and the dropdown shows the file name. /models also answers, so the default URL http://localhost:8080 is unchanged.

LLaMA.c++ is registered in LocalProviderRegistry (enabled by default, no fallback-context setting) and keeps its place on the Local tab. Legacy llamaCPPUrl and isLlamaCPPEnabled migrate once through migrateLegacyLocalProvider. RAG's llama.cpp embedding option now reads the generic URL and enabled flag, and a sub-agent saved as "LLaMA" resolves to the generic URL. LlamaChatModelFactory, its test, its settings fields and UI row, and its special cases are removed. LocalProviderRegistryTest now checks that enabled-by-default providers don't share a default host and port (Nativ shares 8080 but is off by default).

User-visible: the loaded model and its real context in the dropdown, streaming chat (the old factory had none), "add project to context" support, and an "is not running" notice when llama-server is down.

Full test suite passes. Not verified: AC #3, streaming and non-streaming chat against a running llama-server in the IDE.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1409
<!-- SECTION:FINAL_SUMMARY:END -->
