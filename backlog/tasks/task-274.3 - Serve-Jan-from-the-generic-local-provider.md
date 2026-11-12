---
id: TASK-274.3
title: Serve Jan from the generic local provider
status: To Do
assignee: []
created_date: '2026-11-12T11:22:00Z'
updated_date: '2026-11-12T11:22:00Z'
labels:
  - local-llm
  - refactor
dependencies:
  - TASK-274.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1051
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
- [ ] #2 Requests to Jan are still sent as single-line JSON, so the issue #1051 hang does not return
- [ ] #3 The context window is read from the nested settings value, then the top-level value, then the provider default
- [ ] #4 A model without a display name is shown by its id
- [ ] #5 Upgrading users keep a customised Jan URL and their enabled/disabled choice; Jan stays enabled by default
- [ ] #6 The Jan factory, model service and Jan-only DTOs are removed, and any lookup by the old Jan settings keys still resolves
- [ ] #7 Tests cover single-line request bodies, context resolution, display-name fallback and the settings migration cases above
<!-- AC:END -->
