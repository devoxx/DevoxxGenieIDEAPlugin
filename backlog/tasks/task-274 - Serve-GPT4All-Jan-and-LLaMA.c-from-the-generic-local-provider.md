---
id: TASK-274
title: Serve GPT4All, Jan and LLaMA.c++ from the generic local provider
status: To Do
assignee: []
created_date: '2026-11-12T11:21:00Z'
labels:
  - local-llm
  - refactor
dependencies:
  - TASK-273
references:
  - backlog/tasks/task-273 - Add-Rapid-MLX-as-a-local-LLM-provider.md
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1404
priority: medium
type: enhancement
ordinal: 18000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-273 introduced a generic OpenAI-compatible local provider (LocalProviderRegistry / LocalProviderDefinition / OpenAiCompatibleChatModelFactory) and moved Nativ and Rapid-MLX onto it. Three other local providers are still served by near-duplicate dedicated factories, model services and DTOs even though they talk to OpenAI-compatible servers: GPT4All, Jan and LLaMA.c++. Each duplicate has to be wired by hand in many places (enum, factory provider, settings rows, enabled resolver, sub-agent runner, project context controller), which is where new-provider bugs come from.

Unlike Rapid-MLX (which never shipped as a dedicated provider), these three are released, so existing users have saved URLs and enabled flags that must survive the move, and all three are enabled by default today while the generic provider treats an unknown provider as disabled.

Out of scope, confirmed during analysis: LM Studio (lists models from a different endpoint, /api/v1/models, that returns a bare array), Ollama (native API), Exo (needs model-instance preparation), Custom OpenAI (API key, headers, costs), CLI/ACP runners (not HTTP).

Each provider is delivered as its own subtask / PR, in order: GPT4All, then LLaMA.c++, then Jan.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GPT4All, LLaMA.c++ and Jan are each served by the generic local provider (tracked by the subtasks)
- [ ] #2 No dedicated factory, model service or provider-specific DTO remains for these three providers
- [ ] #3 CLAUDE.md and the docusaurus local-models page describe which local providers are generic and which are dedicated
<!-- AC:END -->
