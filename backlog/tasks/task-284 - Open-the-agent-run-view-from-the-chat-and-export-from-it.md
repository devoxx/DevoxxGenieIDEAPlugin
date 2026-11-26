---
id: TASK-284
title: Open the agent run view from the chat, and export from it
status: To Do
assignee: []
created_date: '2026-11-26T14:55:00Z'
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
<!-- AC:END -->
