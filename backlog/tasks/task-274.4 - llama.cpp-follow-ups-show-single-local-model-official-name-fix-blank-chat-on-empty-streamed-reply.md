---
id: TASK-274.4
title: >-
  llama.cpp follow-ups: show single local model, official name, fix blank chat
  on empty streamed reply
status: Done
assignee: []
created_date: '2026-11-12T14:48:00Z'
updated_date: '2026-11-12T14:49:00Z'
labels: []
dependencies: []
modified_files:
  - src/main/java/com/devoxx/genie/ui/panel/LlmProviderPanel.java
  - src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistry.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - docusaurus/docs/llm-providers/local-models.md
  - README.md
  - CLAUDE.md
parent_task_id: TASK-274
priority: medium
type: bug
ordinal: 24000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
After moving LLaMA.c++ to the generic local provider (TASK-274.2): the model dropdown stayed hidden because llama-server serves one model and single-choice dropdowns are hidden (#1327); the provider used the unofficial name "LLaMA.c++"; and with streaming now enabled, a reply that streamed no visible text (thinking model, reasoning_content only) re-added the prompt bubble's id, crashing the Compose chat list ("Key ... was already used") and leaving the chat blank.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Local model servers (Ollama, LM Studio, Exo, generic local providers) show the model dropdown even with a single model; cloud/router providers still hide a single choice
- [x] #2 The provider is displayed as "llama.cpp" in the provider list, settings, chat header and docs
- [x] #3 Settings saved with the former name "LLaMA.c++" still resolve and are re-saved under the new name
- [x] #4 A streamed reply with no visible partial text fills the existing prompt bubble instead of adding a duplicate chat item
- [x] #5 Tests cover each of the above
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
- Model dropdown: local model servers (Ollama, LM Studio, Exo, generic local providers) now show the dropdown even when they serve a single model, so llama-server users can see which model is loaded. Cloud and router providers still hide a single choice (#1327). Visibility goes through `LlmProviderPanel.shouldShowModelNameComboBox`.
- Name: the LLaMA provider is displayed as "llama.cpp" (provider list, settings label, docs, README). `ModelProvider` gained legacy names; `fromString`, `ChatModelFactoryProvider` and the panel's restore accept "LLaMA.c++", and a restored legacy selection is re-saved under the new name. The chat header now uses the provider's display name instead of the enum constant ("llama.cpp : model" rather than "LLaMA : model").
- Blank chat: a streamed reply that produced no visible partial text completed through `addChatMessage` with the id of the prompt bubble already shown, so the Compose LazyColumn got a duplicate key and stopped rendering. `ConversationViewModel.addChatMessage` now fills the existing bubble when that id is present.
- Tests: LlmProviderPanelTest (single local model shown, single cloud model hidden, legacy-name restore), ModelProviderLlamaCppTest, ChatModelFactoryProviderTest (legacy name resolves), ConversationViewModelDuplicateIdTest. Full suite green.
- Known limitation, not fixed: small thinking models (Qwen3.5-0.8B) can end an agent turn with a tool call written inside reasoning_content and empty content; the bubble is then empty rather than crashing.
<!-- SECTION:FINAL_SUMMARY:END -->
