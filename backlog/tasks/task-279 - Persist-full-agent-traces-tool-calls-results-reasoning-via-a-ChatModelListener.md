---
id: TASK-279
title: 'Persist full agent traces (tool calls, results, reasoning) via a ChatModelListener'
status: To Do
assignee: []
created_date: '2026-11-25T10:00:00Z'
labels:
  - agent
  - conversations
  - traces
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - src/main/java/com/devoxx/genie/service/ChatService.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelFactory.java
  - src/main/java/com/devoxx/genie/service/debug/RawTrafficListenerService.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryService.java
  - https://github.com/NielsRogge/copilot-traces-hf
  - https://huggingface.co/docs/hub/en/session-traces-format
priority: medium
type: task
ordinal: 34000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Today DevoxxGenie keeps only flat Q&A pairs for a conversation. `ChatService.saveConversation()` writes two rows per turn to `chat_messages`: the user prompt and the final `AiMessage.text()`, with no role beyond user/assistant, no tool data and no reasoning. The agent loop does exist, but only in memory:
- `ChatMemoryService` holds the `AiMessage.toolExecutionRequests()` and `ToolExecutionResultMessage`s, but in an in-memory store that is windowed and never saved.
- `AgentLoopTracker` publishes tool requests and results to the Activity Log, but only when debug logs are on, and without tool-call ids or a conversation id.

Once a session ends, the tool calls, tool results and reasoning are lost, and restoring a conversation from history rebuilds only text pairs.

The goal is to record the full agent trace durably, so it can later be replayed, inspected or exported. A follow-up task will add export to Hugging Face's Session Trace Simple Format (STS), similar to NielsRogge/copilot-traces-hf for VS Code Copilot.

Approach (option 1 from the investigation):
- Add a `ChatModelListener`, a "trace recorder", next to `MCPListenerService` and `RawTrafficListenerService` in `ChatModelFactory.getListener()`.
- On each `onResponse`, append the returned `AiMessage` to a new SQLite table keyed by conversation id: text, thinking/reasoning when the provider returns it, and tool execution requests with id, name and arguments. Also store the model name and a timestamp.
- The feature is opt-in and **off by default**: the listener is only registered once the user explicitly enables trace recording in Settings, the same way `RawTrafficListenerService` is gated.
- Take tool results from the next request's `ToolExecutionResultMessage`s, or from `AgentLoopTracker`, whichever gives reliable id matching. Once enabled, recording must not depend on the agent-debug-logs setting.
- The listener has to know which conversation and tab a call belongs to. Today `ChatService` assigns the conversation id only after the response completes, so this needs a correlation design, e.g. passing the conversation/tab id through `ChatRequest` attributes or the listener context.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new `trace_events` table (or equivalent) in `conversations.db` is created by a schema migration that leaves existing databases and the `conversations`/`chat_messages` tables unchanged
- [ ] #2 Each agent turn records, in order: the user message, every assistant step (text, reasoning when available, tool calls with id/name/arguments, model, timestamp) and every tool result (tool-call id, tool name, result text, error flag)
- [ ] #3 Trace events are linked to the right conversation id, including with several chat tabs open at once and for sub-agent (`parallel_explore`) calls, which are tagged with their sub-agent id
- [ ] #4 Trace recording is opt-in: a new setting, **off by default**, that the user must explicitly turn on. When it is off, the listener is not registered and nothing is written to `trace_events`. The setting explains that traces can include prompts, file contents, command output and secrets. When it is on, recording works whether agent debug logs are on or off
- [ ] #5 Very large tool results are truncated at a configurable limit, and trace rows are included in the existing 50 MB cleanup and in conversation delete/clear-all
- [ ] #6 Recording never blocks the EDT or the streaming response; write failures are logged and do not break the chat
- [ ] #7 Unit tests cover the listener-to-row mapping, tool-call/result id matching, conversation correlation and the schema migration (using the test-only `ConversationStorageService(String dbPath)` constructor)
- [ ] #8 Verified manually with at least one provider that returns reasoning (e.g. Anthropic or Ollama thinking) and one that does not, plus an MCP tool call
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background (2026-11-25): from a chat investigation of NielsRogge/copilot-traces-hf, a VS Code extension that converts Copilot Chat sessions to Hugging Face STS JSONL with redaction and a TruffleHog scan. Option 2, saving a `ChatMemory` snapshot on save, was rejected because the memory window drops older messages in long agent runs. Out of scope here: STS export, redaction and Hugging Face upload (follow-up task), and showing restored tool calls in the chat UI.
<!-- SECTION:NOTES:END -->
