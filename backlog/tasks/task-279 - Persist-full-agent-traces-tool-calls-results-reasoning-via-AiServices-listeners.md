---
id: TASK-279
title: 'Persist full agent traces (tool calls, results, reasoning) via AiServices listeners'
status: In Progress
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
  - src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
  - src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/ui/settings/debug/DebugSettingsComponent.java
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

Approach (option 1 from the investigation, revised 2026-11-25):
- Use LangChain4j **AiServices listeners** (`AiServices.builder(...).registerListeners(...)`, available since 1.x) rather than a `ChatModelListener`. Chat models are built once per provider and cannot tell which tab or conversation a call belongs to. AiServices listeners are registered on each assistant DevoxxGenie builds (`StreamingPromptStrategy`, `NonStreamingPromptExecutionService`, `SubAgentRunner`), so they know the project, tab/memory key and sub-agent id.
- An `AgentTraceRecorder` listens to:
  - `AiServiceStartedEvent`: the user message.
  - `AiServiceResponseReceivedEvent`: each assistant step, with text, thinking/reasoning, tool execution requests (id, name, arguments) and model.
  - `ToolExecutedEvent`: tool-call id, tool name, result text and error flag.
  - `AiServiceCompletedEvent`, `AiServiceErrorEvent` and `AiServiceCancelledEvent`: close the turn.
- Each event gets a timestamp. The recorder buffers each turn in memory, keyed by memory key. `ChatService.saveConversation()` then persists the buffered turn under the conversation id right after the conversation is saved. Turns that are never saved are discarded.
- The feature is opt-in and **off by default**: the recorder is only registered once the user explicitly enables trace recording in Settings. Once enabled, recording does not depend on the agent-debug-logs setting.
- Secrets are kept out of the stored traces in two layers, both ported from copilot-traces-hf to Java:
  - **Regex redaction** (port of `redact.ts`) runs on every string before it is written. Extend the existing `SecretRedactor` instead of adding a second redactor. Add the missing patterns: Hugging Face, GitHub tokens and PATs, Slack, Stripe, JWTs, PEM private keys, and `password=` / `api_key:` style assignments in plain text, not only in JSON. Also replace the user's home directory with `~` and their OS username with `<user>`, and apply any custom regexes from Settings. Trace placeholders replace the whole secret, e.g. `[REDACTED_GITHUB_TOKEN]`. They don't keep the first and last 4 characters the way the Activity Log masking does.
  - **TruffleHog scan** (port of `trufflehog.ts`), used when the `trufflehog` binary is installed. Before a turn is written, a background task writes each of its already-redacted text fields to its own file in a temp dir with owner-only permissions. It runs `trufflehog filesystem <dir> --json --no-update --no-color --results=verified,unknown,unverified --no-verification` and parses the JSON lines (`DetectorName`, `Raw`, `Verified`, `SourceMetadata.Data.Filesystem.file`). Each finding is then replaced in the field it came from with `[REDACTED_TRUFFLEHOG_<DETECTOR>]`. If a finding can't be removed safely, the turn is not written. The temp dir is always deleted afterwards. Scanning before the write means a secret TruffleHog finds never reaches the database.
  - Settings, mirroring copilot-traces-hf:
    - TruffleHog mode: `auto` (scan when installed, the default), `required` (don't record traces unless a scan succeeds) or `off`.
    - What to do with findings: `redact` (the default) or `skip`, which drops the turn's trace.
    - An optional binary path.
    - `verify`, off by default. Turning it on sends candidate secrets to the issuing service, and the setting must say so.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A new `trace_events` table (or equivalent) in `conversations.db` is created by a schema migration that leaves existing databases and the `conversations`/`chat_messages` tables unchanged
- [x] #2 Each agent turn records, in order: the user message, every assistant step (text, reasoning when available, tool calls with id/name/arguments, model, timestamp) and every tool result (tool-call id, tool name, result text, error flag)
- [x] #3 Trace events are linked to the right conversation id, including with several chat tabs open at once and for sub-agent (`parallel_explore`) calls, which are tagged with their sub-agent id
- [x] #4 Trace recording is opt-in: a new setting, **off by default**, that the user must explicitly turn on. When it is off, the recorder is not registered and nothing is written to `trace_events`. The setting explains that traces can include prompts, file contents, command output and secrets. When it is on, recording works whether agent debug logs are on or off
- [x] #5 Very large tool results are truncated at a configurable limit, and trace rows are included in the existing 50 MB cleanup and in conversation delete/clear-all
- [x] #6 Recording never blocks the EDT or the streaming response; write failures are logged and do not break the chat
- [x] #7 Unit tests cover the event-to-row mapping, tool-call/result id matching, conversation correlation and the schema migration (using the test-only `ConversationStorageService(String dbPath)` constructor)
- [ ] #8 Verified manually with at least one provider that returns reasoning (e.g. Anthropic or Ollama thinking) and one that does not, plus an MCP tool call
- [x] #9 Every string is passed through regex redaction before it is written to `trace_events`. `SecretRedactor` is extended with the patterns from copilot-traces-hf's `redact.ts`, home-directory and username replacement, and custom regexes from Settings. Existing Activity Log masking behaviour is unchanged
- [x] #10 When TruffleHog is installed and the mode is `auto` or `required`, each recorded turn is scanned in the background with `trufflehog filesystem --json --no-verification`, off the EDT and without delaying the chat. Findings are replaced with `[REDACTED_TRUFFLEHOG_<DETECTOR>]`, or the turn's trace is dropped when "on findings" is `skip` or a secret can't be removed safely. The temp files are always deleted
- [x] #11 TruffleHog failure modes are handled:
  - Binary not found: `auto` records with regex redaction only; `required` records nothing and shows a one-time notification with install instructions.
  - Non-zero exit (other than 183) or a timeout: treated as a failed scan.
  - Raw secrets are never logged; only masked forms appear in logs or the UI.
- [x] #12 Unit tests cover each new redaction pattern (including `$` and `\` in matched secrets), home/username replacement, parsing TruffleHog JSON output (finding lines, log lines, verified/unverified/unknown status), and replacing findings in stored rows. A fake `trufflehog` script is used so the tests don't need the real binary
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background (2026-11-25): from a chat investigation of NielsRogge/copilot-traces-hf, a VS Code extension that converts Copilot Chat sessions to Hugging Face STS JSONL with redaction and a TruffleHog scan. Option 2, saving a `ChatMemory` snapshot on save, was rejected because the memory window drops older messages in long agent runs. Redaction and the TruffleHog scan are done at record time, so a secret never reaches `conversations.db`, and any later export starts from clean data. Out of scope here: STS export and Hugging Face upload (follow-up task), and showing restored tool calls in the chat UI.

Implementation (2026-11-25, branch feature/task-279-persist-agent-traces):
- Settings: `DevoxxGenieStateService.agentTrace*` fields; UI in `AgentTraceSettingsPanel`, shown in Settings → DevoxxGenie → Debug under "Agent Trace Recording". Recording is off by default; every other option is disabled until it is switched on. Invalid custom regexes block Apply.
- Recording: `AgentTraceRecorder` registers AiServices listeners in `StreamingPromptStrategy`, `NonStreamingPromptExecutionService` (tool-enabled path only) and `SubAgentRunner`, via `registerForTurn` / `registerForSubAgent`, which are no-ops when recording is off. Only agent/MCP turns go through these builders; plain chats without tools and the external CLI/ACP runners are not traced.
- Sub-agents: `ParallelExploreToolExecutor.executeWithContext` maps the parent's LangChain4j invocation id to the tab's memory key (`AgentTraceBuffer.keyForInvocation`), so sub-agent steps join the parent turn tagged with the sub-agent label.
- Buffer and persist: `AgentTraceBuffer` holds the running turn per memory key. `ChatService.saveConversation` hands it to `AgentTraceService.persistTurn` after `addConversation`; a single background writer redacts (`TraceRedaction` + `SecretRedactor.redactForTrace`), truncates tool results, runs `TruffleHogScanner`, then calls `ConversationStorageService.addTraceTurn`. Unsaved turns are dropped on tab close or after an hour.
- Verified from the LangChain4j 1.24 bytecode: in the streaming handler, `AiServiceResponseReceivedEvent` and `AiServiceCompletedEvent` fire before the `onCompleteResponse` consumer that triggers the conversation save, so the last assistant step is buffered before the turn is taken.
- Storage: `trace_events` (conversationId, turnId, role, content, reasoning, toolCalls JSON, toolCallId, toolName, isError, model, subAgentId, timestamp, scanStatus), created with CREATE TABLE IF NOT EXISTS. Rows are deleted with their conversation, by clear-all and by the 50 MB cleanup.
- Tests: SecretRedactorTraceTest, ConversationTraceStorageTest, AgentTraceRecorderTest, TruffleHogScannerTest (fake trufflehog shell script; POSIX only), AgentTraceServiceTest.
- Test run (JDK 21): full `./gradlew test` ran 4522 tests; 7 failed, all live-API tests (AnthropicChatModelTest, BedrockServiceIT) that need provider keys in a `.env` file. The 159 tests in the trace, storage, redactor, ChatService, sub-agent and prompt-strategy classes all pass.
- Test fixtures for provider keys are split into concatenated strings so GitHub push protection doesn't flag them as real secrets.
- Not done: AC #8 (manual check with real providers and an MCP tool call) needs a running IDE.
<!-- SECTION:NOTES:END -->
