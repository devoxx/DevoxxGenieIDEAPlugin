---
id: TASK-283
title: 'Local agent run view: readable timeline of a recorded conversation'
status: To Do
assignee: []
created_date: '2026-11-26T14:54:00Z'
labels:
  - agent
  - traces
  - ui
dependencies:
  - TASK-279
references:
  - >-
    src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - src/main/java/com/devoxx/genie/service/trace/StoredTraceEvent.java
  - src/main/java/com/devoxx/genie/ui/trace/TraceExportActions.java
  - >-
    src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java
  - src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
priority: medium
ordinal: 37000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Agent traces recorded by TASK-279 sit in `trace_events`, but today the only way to read them is "Preview as Trace" (raw STS JSONL in an editor tab) or uploading them to Hugging Face and opening the Hub viewer. Both are poor for the main use: understanding how the agent worked on a run you just did.

Add a local run view that renders one conversation's trace as a readable timeline, from `ConversationStorageService.getTraceEvents(conversationId)`. It works offline, needs no token or upload, and, unlike the HF upload, includes sub-agent steps. No schema change.

Background (2026-11-26): an uploaded trace showed the agent used `search_files` 5×, `run_command` 5× and `run_script` 4× against 1 `semantic_search`. That's the kind of signal this view should make obvious without leaving the IDE.

Layout:
- Grouped per turn: the user prompt, then each assistant step: its text, its reasoning (collapsed by default), and its tool calls.
- Each tool call shows its name and arguments, with the matching result (linked by tool-call id) under it. Long results are collapsed with a "show more". Errors are highlighted.
- The time since the previous step, from the recorded timestamps. These are approximate: event times, not LLM latency.
- Sub-agent steps (`parallel_explore`) appear nested under the call that started them, labelled with the sub-agent id.
- A small header: model(s), number of turns, steps, tool calls, failed calls and total duration, plus the scan status (CLEAN / NOT_SCANNED).
- Simple markers for suspicious patterns: the same tool called again with the same arguments, and a run that hit the tool-call limit.

Entry point: replace "Preview as Trace" in the conversation history's right-click menu with "View Agent Run". Keep the JSONL preview reachable, e.g. as a secondary action in the view.

Look and feel: should read as related to the Activity Log (`AgentMcpLogPanel`), which shows the same kind of events live. Follow the project's JBList rule: single-line rows with a detail pane, no variable-height multi-line cells, which flicker on JBR.

Out of scope: aggregate statistics across runs ("Agent insights", a possible follow-up), per-step token usage or cost (not recorded), and editing or replaying a run.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 "View Agent Run" in the conversation history's right-click menu opens a run view for that conversation; it is shown only when the conversation has trace rows, and the raw JSONL preview stays available from the view
- [ ] #2 The view shows each turn's user prompt and assistant steps in recorded order, with reasoning collapsed by default and each tool call shown with its arguments and its result (matched by tool-call id)
- [ ] #3 Failed tool calls are visibly marked, and long tool results are collapsed with a way to expand them
- [ ] #4 Sub-agent steps are shown nested under the parallel_explore call that started them, labelled with their sub-agent id
- [ ] #5 A header summarises the run: model(s), turns, steps, tool calls, failed calls, duration and scan status; each step shows the time since the previous one
- [ ] #6 Repeated identical tool calls (same tool, same arguments) are flagged
- [ ] #7 Loading runs off the EDT; a conversation with hundreds of trace rows opens without freezing the IDE, and the layout follows the single-line row + detail pane rule
- [ ] #8 Works in light and dark themes, with no network access or Hugging Face token
- [ ] #9 Unit tests cover building the view model from StoredTraceEvents: turn grouping, call/result matching, sub-agent nesting, error and repeat detection, timing and the summary counts
<!-- AC:END -->
