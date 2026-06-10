---
id: TASK-233
title: Live agent activity timeline in chat with per-tool status and always-on status line
status: To Do
assignee: []
created_date: '2026-06-10 12:00'
updated_date: '2026-06-10 12:00'
labels:
  - enhancement
  - UX
  - agent
dependencies: []
references:
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ActivitySection.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/model/activity/ActivityMessage.java
  - src/main/java/com/devoxx/genie/model/agent/AgentType.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalService.java
  - src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The event plumbing for agent feedback is already complete — `AgentLoopTracker` publishes `TOOL_REQUEST`, `TOOL_RESPONSE`, `TOOL_ERROR`, `LOOP_LIMIT` (plus `APPROVAL_*` from `AgentApprovalService` and `SUB_AGENT_*` from parallel_explore) on `AppTopics.ACTIVITY_LOG_MSG` — but the in-chat rendering wastes it. `ActivitySection.kt` renders a flat monospace list where a request and its response are two unrelated rows, an in-flight tool looks identical to a finished one, and approval/sub-agent/limit events never reach the chat at all. When the "Show tool activity in chat output" setting is off, a multi-minute agent run is completely silent except for streamed reasoning text.

Turn the section into a live, stateful timeline:

1. **Pair request/response into one row.** In `ConversationViewModel.onActivityMessage()`, match a `TOOL_RESPONSE`/`TOOL_ERROR` to the open `TOOL_REQUEST` with the same `callNumber` (and tool name) instead of appending a new entry. Extend `ActivityEntryUiModel` (in `MessageUiModel.kt`) with a `status: RUNNING | SUCCESS | ERROR | PENDING_APPROVAL` field and an optional result payload.
2. **Per-row status visuals.** RUNNING renders a small spinner (reuse the `infiniteTransition` dot pattern from `ThinkingIndicator.kt`), SUCCESS a green check, ERROR a red cross, PENDING_APPROVAL a pause glyph. Keep the existing `tool: first-arg (n/max)` summary text.
3. **Always-on one-line status header.** While the agent loop is active, show a single live line in the bubble — "Running search_files… (step 4/25)" — driven by the most recent open request. This line shows **regardless** of the "Show tool activity in chat" setting (precedent: `INTERMEDIATE_RESPONSE` is already always shown, `ConversationViewModel` lines ~214-226); only the detailed entry list stays opt-in. This removes the dead-silence failure mode.
4. **Expandable rows.** Clicking a row toggles an inline detail area showing the full arguments and (truncated) result in monospace, mirroring the truncation limits used by `AgentMcpLogPanel` (500 chars/line, 10 lines). Add a "Open Logs" affordance on the section header that focuses the existing DevoxxGenie Logs tool window for the full trace.
5. **Surface approvals and sub-agents.** `APPROVAL_REQUESTED` marks the matching row PENDING_APPROVAL with text "Waiting for your approval…" (so the user understands why everything froze while the `AgentApprovalService` dialog with its 120s timeout is up); `APPROVAL_GRANTED/DENIED` resolves it. `SUB_AGENT_STARTED/COMPLETED/ERROR` render as indented child rows under the spawning `parallel_explore` call.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A tool call renders as one row that transitions RUNNING (spinner) → SUCCESS (✓) or ERROR (✗); request and response are no longer two separate entries
- [ ] #2 While the agent loop runs, a one-line live status ("Running <tool>… (step n/max)") is visible in the AI bubble even when "Show tool activity in chat output" is disabled; it disappears when the loop completes
- [ ] #3 Clicking a row expands/collapses full arguments and truncated result inline; truncation limits match AgentMcpLogPanel's (no multi-MB strings in Compose state)
- [ ] #4 APPROVAL_REQUESTED shows a visible "waiting for approval" state on the relevant row; GRANTED/DENIED resolves it (denied renders as ERROR-style with "denied" text)
- [ ] #5 SUB_AGENT_STARTED/COMPLETED/ERROR events render as indented child rows under their parent tool call
- [ ] #6 The Activity section header offers a way to open the DevoxxGenie Logs tool window
- [ ] #7 Events for other projects are still filtered out (existing projectLocationHash check preserved), and entries still attach to the correct (latest) AI message during streaming
- [ ] #8 Unit tests for the ViewModel pairing logic: request→response matching by callNumber, error path, approval lifecycle, unmatched response does not crash, out-of-order events tolerated
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- All state derivation lives in `ConversationViewModel.onActivityMessage()` — keep `AgentLoopTracker` and the message-bus contract untouched so `AgentMcpLogPanel` (which subscribes to the same topic) is unaffected.
- `ActivityEntryUiModel` is a Compose-immutable data class: status transitions create copies via `copy()`, list updates must replace the entry in place (stable keys = callNumber + toolName) so `AnimatedVisibility`/recomposition stays cheap.
- The live status line is derived state (latest entry with status == RUNNING), not a separate event stream — no new topics needed.
- MCP-sourced and RAG-sourced activity messages flow through the same topic with `source` MCP/RAG; they typically have no paired response event — give them a terminal status immediately so they don't show an eternal spinner.
- Auto-expand behavior: keep current default (expanded while active, collapsed once completed); completion signal already exists via `mcpLogsCompleted`.
- "Open Logs": `ToolWindowManager.getInstance(project).getToolWindow(<logs id>)?.show()` — same approach task-216 plans for the RAG filter.
- Out of scope: streaming intermediate stdout of run_command into rows (that's task-236), redesigning AgentMcpLogPanel.
<!-- SECTION:NOTES:END -->
