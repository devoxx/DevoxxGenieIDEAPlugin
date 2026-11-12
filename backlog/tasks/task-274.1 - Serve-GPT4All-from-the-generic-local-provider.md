---
id: TASK-274.1
title: Serve GPT4All from the generic local provider
status: To Do
assignee: []
created_date: '2026-11-12T11:21:00Z'
labels:
  - local-llm
  - refactor
dependencies: []
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
- [ ] #2 GPT4All models get a context window (the provider default when the server reports none) instead of none
- [ ] #3 A user upgrading with a customised GPT4All URL keeps that URL
- [ ] #4 A user upgrading who had GPT4All disabled keeps it disabled; a fresh install and an upgrading user who never changed it have it enabled, as today
- [ ] #5 A generic local provider can be declared enabled by default, and Nativ and Rapid-MLX stay disabled by default
- [ ] #6 The GPT4All factory, model service and GPT4All-only DTOs are removed, and any lookup by the old GPT4All settings keys still resolves
- [ ] #7 Tests cover model listing, context fallback and the settings migration cases above
- [ ] #8 LocalProviderRegistryTest.everyDefinition_isFullyWired passes with GPT4All registered
<!-- AC:END -->
