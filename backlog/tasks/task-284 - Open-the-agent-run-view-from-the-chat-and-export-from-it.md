---
id: TASK-284
title: Open the agent run view from the chat, and export from it
status: In Progress
assignee: []
created_date: '2026-11-26T14:55:00Z'
updated_date: '2026-11-28T11:26:00Z'
labels:
  - agent
  - traces
  - ui
dependencies:
  - TASK-283
references:
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/components/ConversationToolbar.kt
  - src/main/java/com/devoxx/genie/ui/trace/TraceUploadButtonController.java
  - src/main/java/com/devoxx/genie/ui/trace/TraceExportActions.java
  - src/main/java/com/devoxx/genie/ui/trace/HfTraceUploader.java
modified_files:
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/TraceUploadUiState.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/AgentRunUiState.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/components/ConversationToolbar.kt
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/components/ConversationToolbarTraceUploadTest.kt
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/components/ConversationToolbarAgentRunTest.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/ComposeConversationViewController.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ConversationScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ChatScreen.kt
  - src/main/java/com/devoxx/genie/ui/trace/TraceUploadButtonController.java
  - src/main/java/com/devoxx/genie/ui/trace/AgentRunButtonController.java
  - src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
  - >-
    src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationManager.java
  - >-
    src/test/java/com/devoxx/genie/ui/panel/conversation/ConversationManagerTest.java
  - src/test/java/com/devoxx/genie/ui/trace/AgentRunButtonControllerTest.java
  - src/main/java/com/devoxx/genie/service/trace/hf/HfUploadSettings.java
  - src/test/java/com/devoxx/genie/service/trace/hf/TraceUploadServiceTest.java
  - src/main/java/com/devoxx/genie/ui/trace/TraceExportActions.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunPanel.java
  - src/test/java/com/devoxx/genie/ui/trace/view/AgentRunViewerTest.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunViewer.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/debug/AgentTraceSettingsPanel.java
  - src/main/java/com/devoxx/genie/ui/trace/HfTraceUploader.java
  - src/main/java/com/devoxx/genie/service/trace/AgentTraceListener.java
priority: low
ordinal: 38000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up to TASK-283. The run view is opened from the conversation history; this task makes it reachable from where the run happened, and makes it the place to share a run from.

- Chat toolbar: while trace recording is on, the chat window's toolbar (next to "Copy chat" and "Upload to Hugging Face") gets a "View agent run" action for the current conversation. Disabled with a tooltip while there are no trace rows yet or a response is streaming, like the upload button.
- Export from the view: an "Export…" menu in the run view with "Save as .jsonl…" (the existing STS export) and "Upload to Hugging Face…" (the existing HfTraceUploader). Hugging Face becomes one export destination of the local view instead of a separate feature.
- Decide whether the chat toolbar keeps a separate "Upload to Hugging Face" button or only "View agent run" (with upload in its Export menu), to keep the toolbar small. Record the decision in the task notes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 With trace recording on, the chat toolbar has a "View agent run" action that opens TASK-283's view for the current conversation; it is hidden when recording is off and disabled with a tooltip while there is no trace yet or a response is streaming
- [ ] #2 The run view has an Export menu with "Save as .jsonl…" and "Upload to Hugging Face…", reusing the existing export and upload code (same redaction and TruffleHog rules)
- [ ] #3 The decision on keeping a separate toolbar upload button is recorded in the task notes and the toolbar matches it
- [ ] #4 Tests cover the toolbar action's visible/enabled states
- [ ] #5 Reopening the run view for a conversation that already has a run-view tab replaces that tab with a fresh snapshot (no duplicate tabs)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Research (2026-11-28, 4 parallel readers): chat toolbar is Compose (ConversationToolbar.kt) fed by TraceUploadButtonController via ComposeConversationViewController -> ConversationViewModel.traceUpload; run view is Swing (AgentRunPanel header: summary CENTER, "Raw JSONL" ActionLink EAST); export/upload/preview all go through TraceExportActions.prepare -> TraceUploadService.prepare (REQUIRED hold-back + second redaction pass), so reusing those keeps the redaction/TruffleHog rules.

1. Toolbar decision (AC #3) - PENDING USER: recommended to replace the toolbar "Upload to Hugging Face" button with "View agent run" and move upload into the run view's Export menu (the task's intent: HF becomes one export destination of the local view). Record the decision in the notes.
2. Toolbar action (AC #1): extend the existing controller rather than adding a second bus connection. Rename TraceUploadButtonController -> AgentRunButtonController and TraceUploadUiState -> AgentRunUiState(visible, hasTrace) (drop `uploading` if the upload button goes). Click -> AgentRunViewer.open(project, currentConversation); null conversation -> "nothing recorded yet" notification. Thread onViewAgentRunClick through ComposeConversationViewController -> ConversationViewModel -> ConversationScreen -> ChatScreen -> ConversationToolbar. Hidden when recording is off; disabled + tooltip when no trace rows or a response is in flight.
3. In-flight rule: use messages.any { isStreaming || isLoadingIndicatorVisible } instead of the last message's isStreaming (today non-streaming agent runs and a queued prompt bubble leave the button enabled mid-run).
4. Stale state after "+": the "+" button calls ConversationManager.startNewConversation directly and ConversationPanel.startNewConversation/onNewConversation refreshes never run, so hasTrace keeps the previous conversation's value. Refresh the controller when a new conversation starts.
5. Run view Export menu (AC #2): header EAST becomes "Raw JSONL" + an "Export" DropDownLink with "Save as .jsonl..." and "Upload to Hugging Face...". Save-as: new TraceExportActions.saveAs(Project, Conversation) with a save-file dialog (default name <id>.jsonl) built on the existing private prepare(), so the same rules apply; held-back traces report the reason. Upload: TraceExportActions.upload(project, List.of(conversation)).
6. PENDING USER (scope): reopening the view for the same conversation currently adds a second, stale tab; with a toolbar button on a live chat this will happen often. Proposed: reopen replaces the existing tab for that conversation with a fresh snapshot.
7. Tests (AC #4): plain JUnit like ConversationToolbarTraceUploadTest - pure viewAgentRunEnabled/Tooltip(state, inFlight) and responseInFlight(messages) (incl. non-streaming and queued-bubble cases); the controller's state decision extracted to a pure function and tested (recording off -> hidden, no conversation/no rows -> disabled); save-as default file name. Reproduce items 3 and 4 with failing tests first.
8. Verify: ./gradlew test; manual check in the sandbox (toolbar states during/after a run, Export menu save/upload, light/dark).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decisions (2026-11-28, confirmed by the task owner):
- AC #3: the chat toolbar no longer has a separate "Upload to Hugging Face" button. It shows "View agent run" next to "Copy chat"; upload lives in the run view's Export menu (with "Save as .jsonl…"), so the run view is the one place to inspect and then share a run.
- Scope addition: reopening the run view for a conversation replaces that conversation's existing tab with a fresh snapshot instead of adding a second, stale tab.

Implementation (2026-11-28): toolbar upload button replaced by "View agent run" (AgentRunButtonController + AgentRunUiState); run view header gets an Export dropdown (TraceExportActions.saveAs + upload); AgentRunViewer replaces an open tab for the same conversation; ConversationManager notifies a listener on "+"/history so the button never keeps the previous conversation's state; in-flight = latest non-queued/steering message streaming or loading (non-streaming runs and queued bubbles no longer leave the button enabled mid-run). Full suite: 4,635 tests, 0 failures.
Review (3 lenses + adversarial verify): fixed a stale-flag regression (web search, /find, CLI and ACP turns never clear isLoadingIndicatorVisible, so scanning every message disabled the button for the rest of the conversation; now only the latest real turn counts, with a test) and the Settings help text that still described the removed upload button.
Follow-up candidate (not in scope): clear the loading/streaming flags centrally when any prompt completes; the same stale flags also keep TASK-288's progress line running after web search, /find, CLI and ACP turns.
<!-- SECTION:NOTES:END -->
