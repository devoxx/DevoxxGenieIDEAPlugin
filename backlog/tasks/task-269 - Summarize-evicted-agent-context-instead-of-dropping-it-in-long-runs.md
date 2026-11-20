---
id: TASK-269
title: Summarize evicted agent context instead of dropping it in long runs
status: To Do
assignee: []
created_date: '2026-11-07 12:00'
updated_date: '2026-11-07 12:00'
labels:
  - agent-mode
  - memory
  - feature
  - hermes
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/prompt/memory/TaskPreservingMessageWindowChatMemory.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - 'https://hermes-agent.nousresearch.com/docs/developer-guide/architecture'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Long agent runs lose their early context. `TaskPreservingMessageWindowChatMemory` keeps a fixed window of `chatMemorySize` messages. When the window is full it drops the oldest messages, together with their tool calls and results. The system message stays, and so does the original task while a tool loop is running. Everything else is gone: files the agent already read, decisions it made, dead ends it ruled out.

So in a long run the model re-reads files, repeats searches and forgets constraints it found earlier. The loop limit (default `AGENT_MAX_TOOL_CALLS = 50`, up to 500) makes runs long enough for this to happen regularly.

Hermes Agent handles this with a "Compression & Caching" stage in its loop. Its architecture docs don't say exactly how that stage works. This task adds a DevoxxGenie version: summarize the evicted messages instead of dropping them.

## Proposed design

- When the memory window would evict messages, collect the oldest block that can be removed safely: a whole assistant tool-call turn plus all of its tool results. Never split a `tool_calls` / tool result pair. This is the same invariant that `ToolErrorRecovery` protects.
- Replace that block with a single synthetic message, `<CompactedContext>…</CompactedContext>`, written by a summarizer model. The summary keeps: the files and symbols inspected, key findings, edits made (path plus one line on what changed), commands run and their outcome, and open questions.
- Merge repeated compactions into the same `<CompactedContext>` message rather than stacking several of them.
- The summarizer model can be configured, like the sub-agent model is. If none is set, use the main chat model. If summarizing fails or times out, fall back to the current drop behaviour. A failed compaction must never fail the run.
- Run the summarizer off the EDT. Show a "Compacted N messages" entry in the agent debug log.

## Out of scope

- Prompt caching (provider-specific; separate task).
- Compacting normal chat when agent mode is off. Design for it, but turn it on only for agent mode in v1.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new `agentContextCompactionEnabled` setting (default false) is persisted in `DevoxxGenieStateService` and exposed in Agent settings, with an optional summarizer provider and model
- [ ] #2 When enabled and the window overflows during an agent run, evicted messages are replaced by one `<CompactedContext>` message instead of being dropped
- [ ] #3 Compaction never separates an assistant `tool_calls` message from its tool results, and the system message and the running task's user message are always kept
- [ ] #4 Repeated compactions update the existing `<CompactedContext>` message rather than adding new ones
- [ ] #5 If the summarizer errors or exceeds its timeout, memory falls back to today's eviction behaviour and the run continues
- [ ] #6 Summarization never runs on the EDT, and each compaction is logged in the agent debug log with the number of messages compacted
- [ ] #7 Sub-agents (`SubAgentRunner`) are unaffected unless the setting is explicitly extended to them
- [ ] #8 Unit tests cover safe block selection, merging repeated compactions, fallback on summarizer failure, and setting off giving identical behaviour to today
- [ ] #9 The plugin builds and all tests pass (`./gradlew test`)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Came from comparing the DevoxxGenie agent harness with Nous Research's Hermes Agent (2026-11-07). Hermes has a dedicated compression & caching stage in its agent loop. DevoxxGenie only has a sliding message window. Related: TASK-270 (cross-session recall), which covers memory beyond a single run.
<!-- SECTION:NOTES:END -->
