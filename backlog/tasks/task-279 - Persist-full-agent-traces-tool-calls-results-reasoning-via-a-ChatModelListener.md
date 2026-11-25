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
  - src/main/java/com/devoxx/genie/service/debug/SecretRedactor.java
  - https://github.com/NielsRogge/copilot-traces-hf
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/redact.ts
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/trufflehog.ts
  - https://github.com/trufflesecurity/trufflehog
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
- Secrets are kept out of the stored traces in two layers, both ported from copilot-traces-hf to Java:
  - **Regex redaction** (port of `redact.ts`) runs on every string before it is written. Extend the existing `SecretRedactor` instead of adding a second redactor. Add the missing patterns: Hugging Face, GitHub tokens and PATs, Slack, Stripe, JWTs, PEM private keys, and `password=` / `api_key:` style assignments in plain text, not only in JSON. Also replace the user's home directory with `~` and their OS username with `<user>`, and apply any custom regexes from Settings. Trace placeholders replace the whole secret, e.g. `[REDACTED_GITHUB_TOKEN]`. They don't keep the first and last 4 characters the way the Activity Log masking does.
  - **TruffleHog scan** (port of `trufflehog.ts`), used when the `trufflehog` binary is installed. After a turn is recorded, a background task writes that turn's already-redacted trace to a temp dir with owner-only permissions. It runs `trufflehog filesystem <dir> --json --no-update --no-color --results=verified,unknown,unverified --no-verification` and parses the JSON lines (`DetectorName`, `Raw`, `Verified`, `SourceMetadata.Data.Filesystem.file/line`). Each finding is then replaced in the stored rows with `[REDACTED_TRUFFLEHOG_<DETECTOR>]`. If a finding can't be removed safely, that turn's trace rows are deleted. The temp dir is always deleted afterwards.
  - Settings, mirroring copilot-traces-hf:
    - TruffleHog mode: `auto` (scan when installed, the default), `required` (don't record traces unless a scan succeeds) or `off`.
    - What to do with findings: `redact` (the default) or `skip`, which drops the turn's trace.
    - An optional binary path.
    - `verify`, off by default. Turning it on sends candidate secrets to the issuing service, and the setting must say so.
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
- [ ] #9 Every string is passed through regex redaction before it is written to `trace_events`. `SecretRedactor` is extended with the patterns from copilot-traces-hf's `redact.ts`, home-directory and username replacement, and custom regexes from Settings. Existing Activity Log masking behaviour is unchanged
- [ ] #10 When TruffleHog is installed and the mode is `auto` or `required`, each recorded turn is scanned in the background with `trufflehog filesystem --json --no-verification`, off the EDT and without delaying the chat. Findings are replaced with `[REDACTED_TRUFFLEHOG_<DETECTOR>]`, or the turn's trace is dropped when "on findings" is `skip` or a secret can't be removed safely. The temp files are always deleted
- [ ] #11 TruffleHog failure modes are handled:
  - Binary not found: `auto` records with regex redaction only; `required` records nothing and shows a one-time notification with install instructions.
  - Non-zero exit (other than 183) or a timeout: treated as a failed scan.
  - Raw secrets are never logged; only masked forms appear in logs or the UI.
- [ ] #12 Unit tests cover each new redaction pattern (including `$` and `\` in matched secrets), home/username replacement, parsing TruffleHog JSON output (finding lines, log lines, verified/unverified/unknown status), and replacing findings in stored rows. A fake `trufflehog` script is used so the tests don't need the real binary
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background (2026-11-25): from a chat investigation of NielsRogge/copilot-traces-hf, a VS Code extension that converts Copilot Chat sessions to Hugging Face STS JSONL with redaction and a TruffleHog scan. Option 2, saving a `ChatMemory` snapshot on save, was rejected because the memory window drops older messages in long agent runs. Redaction and the TruffleHog scan are done at record time, so a secret never reaches `conversations.db`, and any later export starts from clean data. Out of scope here: STS export and Hugging Face upload (follow-up task), and showing restored tool calls in the chat UI.
<!-- SECTION:NOTES:END -->
