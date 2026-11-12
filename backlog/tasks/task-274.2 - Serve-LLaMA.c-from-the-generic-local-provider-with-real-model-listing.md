---
id: TASK-274.2
title: Serve LLaMA.c++ from the generic local provider with real model listing
status: To Do
assignee: []
created_date: '2026-11-12T11:22:00Z'
updated_date: '2026-11-12T11:22:00Z'
labels:
  - local-llm
  - refactor
dependencies:
  - TASK-274.1
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
- [ ] #1 The LLaMA.c++ model dropdown shows the model(s) llama-server reports from /v1/models instead of a fixed placeholder
- [ ] #2 The model's context window comes from the server when it reports one, with the provider default otherwise
- [ ] #3 Chat works through the generic local provider in streaming and non-streaming modes, verified against a running llama-server
- [ ] #4 Upgrading users keep a customised LLaMA.c++ URL and their enabled/disabled choice; LLaMA.c++ stays enabled by default
- [ ] #5 The dedicated LLaMA.c++ factory is removed, and any lookup by the old LLaMA.c++ settings keys still resolves
- [ ] #6 Tests cover model listing, context parsing and the settings migration cases above
- [ ] #7 The docs describe that LLaMA.c++ now lists the server's models
<!-- AC:END -->
