---
id: TASK-283
title: 'Local agent run view: readable timeline of a recorded conversation'
status: In Progress
assignee: []
created_date: '2026-11-26T14:54:00Z'
updated_date: '2026-11-26T15:23:00Z'
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
modified_files:
  - src/main/java/com/devoxx/genie/service/trace/view/AgentRun.java
  - src/main/java/com/devoxx/genie/service/trace/view/AgentRunModelBuilder.java
  - >-
    src/test/java/com/devoxx/genie/service/trace/view/AgentRunModelBuilderTest.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunText.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunTreeBuilder.java
  - src/test/java/com/devoxx/genie/ui/trace/view/AgentRunViewTest.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunFile.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunEditorProvider.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunEditor.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunPanel.java
  - src/main/java/com/devoxx/genie/ui/trace/view/AgentRunViewer.java
  - src/main/resources/META-INF/plugin.xml
  - >-
    src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - >-
    src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java
  - >-
    src/test/java/com/devoxx/genie/service/conversations/ConversationTraceStorageTest.java
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
- [x] #1 "View Agent Run" in the conversation history's right-click menu opens a run view for that conversation; it is shown only when the conversation has trace rows, and the raw JSONL preview stays available from the view
- [x] #2 The view shows each turn's user prompt and assistant steps in recorded order, with reasoning collapsed by default and each tool call shown with its arguments and its result (matched by tool-call id)
- [x] #3 Failed tool calls are visibly marked, and long tool results are collapsed with a way to expand them
- [ ] #4 Sub-agent steps are shown nested under the parallel_explore call that started them, labelled with their sub-agent id
- [x] #5 A header summarises the run: model(s), turns, steps, tool calls, failed calls, duration and scan status; each step shows the time since the previous one
- [x] #6 Repeated identical tool calls (same tool, same arguments) are flagged
- [x] #7 Loading runs off the EDT; a conversation with hundreds of trace rows opens without freezing the IDE, and the layout follows the single-line row + detail pane rule
- [ ] #8 Works in light and dark themes, with no network access or Hugging Face token
- [x] #9 Unit tests cover building the view model from StoredTraceEvents: turn grouping, call/result matching, sub-agent nesting, error and repeat detection, timing and the summary counts
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Pure view model (service/trace/view): AgentRunModelBuilder turns List<StoredTraceEvent> into AgentRun (Summary, Turn, Step, ToolCallNode, SubAgentRun). Covers call/result matching by tool-call id, repeat detection (same tool + same arguments earlier in the run), the tool-call limit (result starting with "Error: Agent loop limit reached"), timings and summary counts. Sub-agent rows (subAgentId) are nested under the parallel_explore call they fall between, by timestamp. Tests first: AgentRunModelBuilderTest.
2. Tree model and detail text, as pure functions with their own tests.
3. UI: an editor tab via a new fileEditorProvider (AgentRunVirtualFile + AgentRunEditorProvider + AgentRunEditor). Header with summary and a Raw JSONL button; OnePixelSplitter with a single-line-row tree (ColoredTreeCellRenderer, plain text) and a read-only detail pane.
4. Entry point: the conversation history menu's "Preview as Trace" becomes "View Agent Run"; trace rows are loaded and the model built off the EDT.
5. Full test suite, then a manual check with the 32-row trace (conversation 1795703237209).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation 2026-11-26:
- Model: service/trace/view/AgentRun (Summary, Turn, Step, ToolCallNode, SubAgentRun) + AgentRunModelBuilder (pure). Results are matched by tool-call id, falling back to the first pending call of the same tool. A repeat is the same tool with the same arguments (canonical JSON) earlier in the run, across turns; each sub-agent has its own scope. The tool-call limit is detected from the result text. Sub-agent rows (labels like sub-agent-1:…, reused by every parallel_explore) are placed by time window under the latest parallel_explore call asked for before them whose result came after; rows that fit no call become the turn's unattached sub-agents.
- UI (ui/trace/view): AgentRunFile (LightVirtualFile carrying the run), AgentRunEditorProvider (registered in plugin.xml, HIDE_DEFAULT_EDITOR), AgentRunEditor, AgentRunPanel (summary header + Raw JSONL link; OnePixelSplitter with a single-line ColoredTreeCellRenderer tree and a read-only monospace detail pane), AgentRunText (row, detail and summary text, plain strings) and AgentRunTreeBuilder. AgentRunViewer.open loads rows and builds the model off the EDT.
- The turn row shows the text inside <UserPrompt>; the detail pane also shows the full message sent to the model.
- History panel: "Preview as Trace" is replaced by "View Agent Run", shown only for conversations in ConversationStorageService.getConversationIdsWithTraces() (loaded with the list, so the menu never queries on the EDT). The raw JSONL is the Raw JSONL link in the view.
- Durations are approximate: a call's duration runs from its step to its result, so calls from the same step whose results are recorded together show the same time.
- Verified by an offscreen render (light theme) of the real 32-row trace (conversation 1795703237209): 1 turn, 13 steps, 18 calls; layout and text as intended. Not yet verified: icons and the dark theme in the IDE (AC #8), and a real parallel_explore run (AC #4, covered by unit tests only).
- Tests: AgentRunModelBuilderTest (16), AgentRunViewTest (10), ConversationTraceStorageTest.conversationIdsWithTraces. Full ./gradlew test --rerun passes (JDK 21).
<!-- SECTION:NOTES:END -->
