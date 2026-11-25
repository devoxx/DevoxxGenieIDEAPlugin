---
id: TASK-280
title: 'Export agent traces to Hugging Face STS format and upload to a dataset'
status: In Progress
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
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ConversationToolbar.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ChatScreen.kt
  - src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java
  - src/main/java/com/devoxx/genie/service/debug/SecretRedactor.java
  - src/main/java/com/devoxx/genie/startup/CredentialMigrationStartupActivity.java
  - https://huggingface.co/docs/hub/en/session-traces-format
  - https://huggingface.co/docs/hub/en/agent-traces
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/converter.ts
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/hub.ts
  - https://github.com/NielsRogge/copilot-traces-hf/blob/main/src/extension.ts
  - https://github.com/huggingface/huggingface.js/blob/main/packages/hub/src/lib/commit.ts
priority: low
type: task
ordinal: 35000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-279 records full agent traces, already redacted and TruffleHog-scanned, in `conversations.db`. This task lets users turn those traces into Hugging Face Session Trace Simple Format (STS) JSONL files. Users can save the files locally or upload them to a Hugging Face dataset, where they open in the Hub's agent trace viewer. Storage buckets use a different upload API and are left for a follow-up task (decided 2026-11-25). It is the DevoxxGenie counterpart of NielsRogge/copilot-traces-hf for VS Code Copilot.

**STS converter** (port of `converter.ts`), one `.jsonl` file per conversation:
- A `session` header line: `harness` (`devoxxgenie`), `id` (the conversation id), `name` (the title), `agent`, `source`, `createdAt`, `models`, `mode` (`agent` or `chat`), and the project name when enabled.
- Then `message` lines in order:
  - `user` messages, with `content` and `timestamp`.
  - `assistant` messages, with `content`, `reasoningContent`, `toolCalls` (`[{id, type:"function", function:{name, arguments}}]`) and `model`.
  - `tool` messages, with `toolCallId`, `name` and `content`.
- Conversations saved before TASK-279, or recorded with tracing off, export as plain user/assistant text pairs.

**Chat window upload button** (the main entry point, added 2026-11-25):
- When "Record agent traces" is enabled, the chat window's `ConversationToolbar` shows a **Upload to Hugging Face** button next to "Copy chat". It is hidden when recording is off, and disabled (with a tooltip saying why) while the conversation has no recorded trace yet or a response is still streaming.
- One click converts the current conversation to STS JSONL and uploads it. No manual export, script or `hf` CLI is needed.
- First use opens a short setup dialog: the Hugging Face token (stored in `PasswordSafe`; skipped when `HF_TOKEN` or an `hf auth login` token is found), and the target dataset, prefilled as `<hf-user>/devoxxgenie-traces` from `whoami`, private by default. The choices are saved, so later uploads are one click, plus the confirmation dialog unless it's turned off.
- While it runs, the button shows progress. When it finishes, a notification links to the uploaded file in the Hub's trace viewer. A failure (bad token, rate limit, conversation held back by TruffleHog `required` mode) shows a readable notification and leaves nothing half-uploaded.
- Uploading the same conversation again overwrites its file, so a user can upload after each turn as the conversation grows.

**Other actions**, all running off the EDT with progress and cancellation:
- **Preview as trace**: opens the JSONL in an editor tab, so the user can check redaction before uploading.
- **Export to folder**: writes the selected conversations as `.jsonl` files.
- **Upload to Hugging Face** for several conversations: the selected ones, or all of the project's.
- Entry points for these are the conversation history panel and the IDE actions / Find Action.

**Hugging Face upload** (port of `hub.ts`):
- The upload uses the Hub HTTP API from Java. No new heavy dependency is added, and the HTTP code reuses the plugin's existing HTTP client and proxy settings.
- The steps are: `whoami`, check whether the repo exists, create it if missing (**private** by default), then commit the files to `<folder>/<conversationId>.jsonl`. Uploading again overwrites the previous file.
- The target is `datasets/<user>/<name>`, parsed like `parseTarget()` (also accepts a bare `user/name`, `hf://…` and a huggingface.co URL). `buckets/…` targets are rejected with a message that they aren't supported yet.
- The token is stored in IntelliJ `PasswordSafe`, like the other provider keys. If none is stored, `HF_TOKEN` or the token saved by `hf auth login` is used. The token needs write access.

**Safety**:
- Export and upload run the TASK-279 redaction again, as a second pass.
- If the TruffleHog mode is `required`, a conversation that hasn't been scanned successfully is held back.
- Before a manual upload, a confirmation dialog shows the target, its visibility and the number of conversations. The dialog can be turned off.
- Optional auto-upload is out of scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A converter turns a stored conversation and its `trace_events` into valid STS JSONL: a header line followed by user/assistant/tool messages in the recorded order, with tool calls and tool results linked by id, and `reasoningContent` included when recorded
- [x] #2 Conversations without trace events (older ones, or recorded with tracing off) export as plain user/assistant messages instead of failing
- [x] #3 Tool results above a configurable limit (default 50,000 characters, 0 = no limit) are truncated, with a marker saying so
- [x] #4 "Preview as trace" and "Export to folder…" work from the conversation history panel for one or several selected conversations
- [x] #11 With "Record agent traces" enabled, the chat window toolbar shows an "Upload to Hugging Face" button next to "Copy chat". It is hidden when recording is off and disabled, with a tooltip, while the conversation has no trace rows or a response is streaming
- [x] #12 Clicking the button uploads the current conversation as STS JSONL in one step. The first click opens a setup dialog for the token and target (prefilled `<hf-user>/devoxxgenie-traces`, private), and later clicks reuse the saved choices. A notification links to the uploaded trace on the Hub
- [x] #5 "Upload to Hugging Face…" creates the dataset as private when it doesn't exist (visibility can be set in Settings), uploads `<folder>/<conversationId>.jsonl` files in a single commit, and shows a link to the target when it finishes
- [x] #6 Settings include: the target, the visibility used when creating it, the folder inside the repo, whether to include the project name in the header, the tool-result limit, and the confirm-before-upload toggle. A Hugging Face token can be set or removed through `PasswordSafe`, with fallback to `HF_TOKEN` or the `hf auth login` token
- [x] #7 Redaction runs again on export and upload, and the TruffleHog `required` mode holds back conversations that weren't scanned successfully. The upload summary lists how many conversations were uploaded and how many were held back, and why
- [x] #8 All network and file work runs off the EDT with a progress indicator and cancellation. Hub errors (401/403 bad token, 404, 409 conflict, rate limits) are shown as readable notifications, and the token never appears in logs
- [x] #9 Unit tests cover the converter (agent turn with tool calls, reasoning, a tool error, a plain chat conversation, truncation), target parsing, and the upload client against a mock HTTP server
- [ ] #10 Verified manually: an uploaded conversation opens correctly in the Hugging Face trace viewer
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Follow-up to TASK-279, created 2026-11-25. Depends on TASK-279 for the stored `trace_events`, redaction and TruffleHog settings. Out of scope: auto-upload on an interval, uploading to anywhere other than Hugging Face, and importing traces back into DevoxxGenie.

Implementation (2026-11-25, branch feature/task-280-hf-trace-upload, stacked on feature/task-279-persist-agent-traces):
- `service/trace/hf`:
  - `StsConverter` builds the JSONL. Sub-agent rows are left out; the header carries `mode` agent/chat, `models`, `provider` and `workspace`.
  - `HuggingFaceHubClient` follows the official `@huggingface/hub` `commit.ts` protocol: whoami-v2, dataset exists/create, preupload, the LFS batch with basic or multipart transfer (Xet is not requested), then the NDJSON commit. It uses its own OkHttp client without the shared retry interceptor, which would retry 401/409.
  - `HfTokenResolver`: IDE credential store (`CredentialKey.HUGGING_FACE_TOKEN`), then HF_TOKEN / HUGGING_FACE_HUB_TOKEN, then the `hf auth login` token file.
  - `TraceUploadService` prepares the files, holds back unscanned traces in TruffleHog `required` mode, and commits them in one commit.
- `ui/trace`:
  - `TraceUploadButtonController` keeps each chat tab's toolbar button in sync, via the new `AgentTraceListener` app topic: traceWritten / settingsChanged.
  - `HfTraceUploader` runs the setup dialog, confirmation, background task and notification with an "Open on Hugging Face" link.
  - `TraceExportActions` provides preview (read-only editor tab) and export to a folder.
- Chat toolbar: `ConversationToolbar` and `TraceUploadUiState`, wired through ConversationViewModel, ConversationScreen and ChatScreen.
- History panel: right-click menu (Preview / Export / Upload) and Export All / Upload All buttons, shown while recording is on. Left-click still opens the conversation.
- Tests: StsConverterTest, HuggingFaceHubClientTest (MockWebServer; regular, LFS basic, LFS already stored, multipart, error mapping), HfDatasetTargetTest, HfTokenResolverTest, TraceUploadServiceTest, ConversationToolbarTraceUploadTest, and ConversationTraceStorageTest.hasTraceEvents.
- Bug found by the tests: Jackson's generic `ObjectNode.set()` inside `StringBuilder.append()` was inferred as CharSequence and threw a ClassCastException at runtime; fixed by building the node first.
- Not done: AC #10 (manual upload to the real Hub and opening it in the trace viewer) needs a token and a running IDE.
- Follow-ups: storage bucket targets; optional auto-upload.
<!-- SECTION:NOTES:END -->
