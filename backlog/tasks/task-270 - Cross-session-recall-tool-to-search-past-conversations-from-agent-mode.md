---
id: TASK-270
title: Cross-session recall tool to search past conversations from agent mode
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
  - src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptions.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - 'https://github.com/NousResearch/hermes-agent'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

The agent can't recall what happened in earlier sessions. `ConversationStorageService` already saves every conversation per project in SQLite (`conversations` and `chat_messages` tables), but only the Conversation History panel reads it. The only knowledge that carries over between sessions is `DEVOXXGENIE.md`, which is written by hand.

Hermes Agent searches past sessions with SQLite FTS5 full-text search plus LLM summaries. This task adds the DevoxxGenie version: a read-only `recall_conversations` agent tool over the history we already store.

## Proposed design

- Add an FTS5 virtual table over `chat_messages` content, kept in sync with triggers. Fill it once for existing rows. If the bundled SQLite driver lacks FTS5, fall back to a `LIKE` query.
- New built-in tool `recall_conversations(query, limit?)`:
  - Searches only conversations of the **current project**.
  - Returns the top matching conversations, each with the conversation title and date and a short snippet around each match. The reply is capped so it can't flood the context.
  - Excludes the conversation that is currently running.
- Optional second step: `recall_conversation(id)` returns an LLM-written summary of one past conversation, kept under a token cap. A follow-up is fine.
- The tool only reads, so it goes in `AgentApprovalProvider.READ_ONLY_TOOLS`.
- It's gated by `conversationRecallToolEnabled` (default false), following the pattern of the other optional tools in `BuiltInToolProvider`.
- Add the tool description to `BuiltInToolDescriptions` so it can be overridden like the others.

## Privacy

Past conversations can contain secrets that were pasted into chat. Results never cross projects. Snippets only go to the model the user has configured, the same as any other tool result. The settings UI says this clearly.

## Out of scope

- Automatic user modelling or profiling (Hermes "user modeling").
- Writing to memory automatically. TASK-224 (Skill Writer) covers durable learned knowledge.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A full-text index over stored chat messages is created and kept in sync, with existing rows filled once; a `LIKE` fallback is used when FTS5 is unavailable
- [ ] #2 A `recall_conversations(query, limit?)` built-in tool, gated by `conversationRecallToolEnabled` (default false), returns matching past conversations of the current project only, with title, date and capped snippets
- [ ] #3 The currently running conversation is excluded from results
- [ ] #4 The tool is registered in `AgentApprovalProvider.READ_ONLY_TOOLS` and its description is overridable through `BuiltInToolDescriptions`
- [ ] #5 Database access runs off the EDT and a failed query returns a readable error string rather than throwing
- [ ] #6 The setting is persisted in `DevoxxGenieStateService` and exposed in Agent settings with a short privacy note
- [ ] #7 Unit tests cover project scoping, exclusion of the current conversation, the result size cap, the FTS5 fallback, and the tool not being registered when the setting is off
- [ ] #8 The plugin builds and all tests pass (`./gradlew test`)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Came from comparing the DevoxxGenie agent harness with Nous Research's Hermes Agent (2026-11-07). Hermes searches past sessions with FTS5 plus LLM summaries. DevoxxGenie already stores conversations in SQLite but doesn't let the agent search them. Related: TASK-269 (compacting context within a run), TASK-224 (Skill Writer).
<!-- SECTION:NOTES:END -->
