---
id: TASK-280
title: 'Export agent traces to Hugging Face STS format and upload to a dataset or bucket'
status: To Do
assignee: []
created_date: '2026-11-25T10:30:00Z'
labels:
  - agent
  - conversations
  - traces
dependencies:
  - TASK-279
references:
  - src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java
  - src/main/java/com/devoxx/genie/service/debug/SecretRedactor.java
  - src/main/java/com/devoxx/genie/startup/CredentialMigrationStartupActivity.java
  - https://huggingface.co/docs/hub/en/session-traces-format
  - https://huggingface.co/docs/hub/en/agent-traces
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/converter.ts
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/hub.ts
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/extension.ts
priority: low
type: task
ordinal: 35000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-279 records full agent traces, already redacted and TruffleHog-scanned, in `conversations.db`. This task lets users turn those traces into Hugging Face Session Trace Simple Format (STS) JSONL files. Users can save the files locally or upload them to a Hugging Face dataset or storage bucket, where they open in the Hub's agent trace viewer. It is the DevoxxGenie counterpart of NielsRogge/copilot-traces-hf for VS Code Copilot.

**STS converter** (port of `converter.ts`), one `.jsonl` file per conversation:
- A `session` header line: `harness` (`devoxxgenie`), `id` (the conversation id), `name` (the title), `agent`, `source`, `createdAt`, `models`, `mode` (`agent` or `chat`), and the project name when enabled.
- Then `message` lines in order:
  - `user` messages, with `content` and `timestamp`.
  - `assistant` messages, with `content`, `reasoningContent`, `toolCalls` (`[{id, type:"function", function:{name, arguments}}]`) and `model`.
  - `tool` messages, with `toolCallId`, `name` and `content`.
- Conversations saved before TASK-279, or recorded with tracing off, export as plain user/assistant text pairs.

**Actions**, all running off the EDT with progress and cancellation:
- **Preview as trace**: opens the JSONL in an editor tab.
- **Export to folder**: writes the selected conversations as `.jsonl` files.
- **Upload to Hugging Face**: uploads the selected conversations, or all conversations of the project.
- Entry points are the conversation history panel and the IDE actions / Find Action.

**Hugging Face upload** (port of `hub.ts`):
- The upload uses the Hub HTTP API from Java. No new heavy dependency is added, and the HTTP code reuses the plugin's existing HTTP client and proxy settings.
- The steps are: `whoami`, check whether the repo exists, create it if missing (**private** by default), then commit the files to `<folder>/<conversationId>.jsonl`. Uploading again overwrites the previous file.
- The target is `datasets/<user>/<name>` or `buckets/<user>/<name>`, parsed the same way as `parseTarget()` (also accepts a bare `user/name`, `hf://…` and a huggingface.co URL).
- The token is stored in IntelliJ `PasswordSafe`, like the other provider keys. If none is stored, `HF_TOKEN` or the token saved by `hf auth login` is used. The token needs write access.

**Safety**:
- Export and upload run the TASK-279 redaction again, as a second pass.
- If the TruffleHog mode is `required`, a conversation that hasn't been scanned successfully is held back.
- Before a manual upload, a confirmation dialog shows the target, its visibility and the number of conversations. The dialog can be turned off.
- Optional auto-upload is out of scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A converter turns a stored conversation and its `trace_events` into valid STS JSONL: a header line followed by user/assistant/tool messages in the recorded order, with tool calls and tool results linked by id, and `reasoningContent` included when recorded
- [ ] #2 Conversations without trace events (older ones, or recorded with tracing off) export as plain user/assistant messages instead of failing
- [ ] #3 Tool results above a configurable limit (default 50,000 characters, 0 = no limit) are truncated, with a marker saying so
- [ ] #4 "Preview as trace" and "Export to folder…" work from the conversation history panel for one or several selected conversations
- [ ] #5 "Upload to Hugging Face…" creates the dataset or bucket as private when it doesn't exist (visibility can be set in Settings), uploads `<folder>/<conversationId>.jsonl` files in a single commit, and shows a link to the target when it finishes
- [ ] #6 Settings include: the target, the visibility used when creating it, the folder inside the repo, whether to include the project name in the header, the tool-result limit, and the confirm-before-upload toggle. A Hugging Face token can be set or removed through `PasswordSafe`, with fallback to `HF_TOKEN` or the `hf auth login` token
- [ ] #7 Redaction runs again on export and upload, and the TruffleHog `required` mode holds back conversations that weren't scanned successfully. The upload summary lists how many conversations were uploaded and how many were held back, and why
- [ ] #8 All network and file work runs off the EDT with a progress indicator and cancellation. Hub errors (401/403 bad token, 404, 409 conflict, rate limits) are shown as readable notifications, and the token never appears in logs
- [ ] #9 Unit tests cover the converter (agent turn with tool calls, reasoning, a tool error, a plain chat conversation, truncation), target parsing, and the upload client against a mock HTTP server
- [ ] #10 Verified manually: an uploaded conversation opens correctly in the Hugging Face trace viewer
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Follow-up to TASK-279, created 2026-11-25. Depends on TASK-279 for the stored `trace_events`, redaction and TruffleHog settings. Out of scope: auto-upload on an interval, uploading to anywhere other than Hugging Face, and importing traces back into DevoxxGenie.
<!-- SECTION:NOTES:END -->
