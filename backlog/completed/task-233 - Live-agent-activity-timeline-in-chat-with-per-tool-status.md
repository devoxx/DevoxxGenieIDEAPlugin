---
id: TASK-233
title: >-
  Live agent activity timeline in chat with per-tool status and always-on status
  line
status: Done
assignee: []
created_date: '2026-06-10 12:00'
updated_date: '2026-06-10 21:48'
labels:
  - enhancement
  - UX
  - agent
dependencies: []
references:
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ActivitySection.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/model/activity/ActivityMessage.java
  - src/main/java/com/devoxx/genie/model/agent/AgentType.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalService.java
  - src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
modified_files:
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ActivitySection.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/MessagePair.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ChatScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ConversationScreen.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/ComposeConversationViewController.kt
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalService.java
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModelTest.kt
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/ReadFileToolExecutorTest.java
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
- [x] #1 A tool call renders as one row that transitions RUNNING (spinner) → SUCCESS (✓) or ERROR (✗); request and response are no longer two separate entries
- [x] #2 While the agent loop runs, a one-line live status ("Running <tool>… (step n/max)") is visible in the AI bubble even when "Show tool activity in chat output" is disabled; it disappears when the loop completes
- [x] #3 Clicking a row expands/collapses full arguments and truncated result inline; truncation limits match AgentMcpLogPanel's (no multi-MB strings in Compose state)
- [x] #4 APPROVAL_REQUESTED shows a visible "waiting for approval" state on the relevant row; GRANTED/DENIED resolves it (denied renders as ERROR-style with "denied" text)
- [x] #5 SUB_AGENT_STARTED/COMPLETED/ERROR events render as indented child rows under their parent tool call
- [x] #6 The Activity section header offers a way to open the DevoxxGenie Logs tool window
- [x] #7 Events for other projects are still filtered out (existing projectLocationHash check preserved), and entries still attach to the correct (latest) AI message during streaming
- [x] #8 Unit tests for the ViewModel pairing logic: request→response matching by callNumber, error path, approval lifecycle, unmatched response does not crash, out-of-order events tolerated
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. MessageUiModel.kt: add ActivityStatus enum (RUNNING/SUCCESS/ERROR/PENDING_APPROVAL/INFO); extend ActivityEntryUiModel with status, isToolActivity, subAgentId, children; add MessageUiModel.showToolActivity captured per prompt.
2. ConversationViewModel.kt: pair TOOL_RESPONSE/TOOL_ERROR to open TOOL_REQUEST by (subAgentId, toolName, callNumber); ERROR final per row; APPROVAL_REQUESTED → PENDING_APPROVAL on latest open row for tool, GRANTED → RUNNING, DENIED → ERROR("denied"); SUB_AGENT_* nest as children under open parallel_explore row; entries always tracked regardless of showToolActivityInChat (setting only gates list rendering); truncate args/results at write time (500 chars/line, 10 lines per AgentMcpLogPanel).
3. AgentApprovalService.java: publish APPROVAL_REQUESTED before dialog, GRANTED/DENIED after, gated on agentDebugLogsEnabled; AgentLoopTracker untouched.
4. ActivitySection.kt: per-row status icons (spinner/check/cross/pause), click-to-expand inline details, indented children, Open Logs header link.
5. AiBubble.kt: always-on live status line while in-flight, derived from latest RUNNING/PENDING_APPROVAL entry.
6. Wire onOpenLogs through ComposeConversationViewController → screens → MessagePair → ActivitySection via ToolWindowManager("DevoxxGenieActivityLogs").show().
7. ConversationViewModelTest.kt: pairing, error, approval lifecycle, unmatched/out-of-order tolerance, sub-agent nesting, MCP terminal status, live-status derivation; adjust 2 existing tests to the new always-track contract.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned, with these discoveries/decisions:
- APPROVAL_* events were defined in AgentType and rendered by AgentMcpLogPanel but never published anywhere. Added publishing in AgentApprovalService (REQUESTED before the dialog, GRANTED/DENIED after, DENIED also on timeout), gated behind agentDebugLogsEnabled like the loop tracker's events. AgentLoopTracker untouched.
- Added ActivityStatus.INFO as a fifth status for lifecycle-less entries (agent reasoning, MCP/RAG log lines) — terminal immediately, renders a neutral dot instead of a misleading green check.
- Pairing key is (subAgentId, toolName, callNumber): sub-agent internal tool calls (subAgentId like "sub-agent-1:provider:model") use an independent callNumber counter that collides with the main loop's, so toolName+callNumber alone is ambiguous.
- APPROVAL_* events carry no callNumber (published by the approval layer, not the tracker) — they match the latest RUNNING row for the same toolName. A row resolved to ERROR by APPROVAL_DENIED is final; the tracker's follow-up TOOL_RESPONSE (carrying the denial string) cannot flip it to SUCCESS.
- Tool entries are now ALWAYS tracked in the ViewModel regardless of "Show tool activity in chat output"; the setting is snapshotted per message (MessageUiModel.showToolActivity) and only gates rendering of detailed rows. The live status line in AiBubble derives from the latest RUNNING/PENDING_APPROVAL entry and shows while isLoadingIndicatorVisible || isStreaming.
- Args/results truncated at write time (500 chars/line, 10 lines, "(N more lines)") before entering Compose state.
- Known limitation (pre-existing contract, kept intentionally): all events except LOOP_LIMIT are gated behind Settings → Agent → debug logs (agentDebugLogsEnabled) in AgentLoopTracker, so the timeline and status line require that setting on.
- Also fixed pre-existing failure ReadFileToolExecutorTest.execute_validRequest_readsFileSuccessfully (user request): commit 4c9b5ac2 swapped ReadAction.compute → ReadAccess.compute in the executor but the test still mocked ReadAction; both end-to-end tests now mock ReadAccess.
Full test suite green: 2979 tests, 0 failures.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Turned the flat in-chat activity list into a live, stateful agent timeline.

**ViewModel (pairing state machine):** `ConversationViewModel.onActivityMessage()` now pairs TOOL_RESPONSE/TOOL_ERROR into the open TOOL_REQUEST row by (subAgentId, toolName, callNumber) — one row per tool call transitioning RUNNING → SUCCESS/ERROR. ERROR rows are final (a denial's follow-up TOOL_RESPONSE can't flip them green). Unmatched/out-of-order events are tolerated. APPROVAL_REQUESTED pauses the matching row (PENDING_APPROVAL, "Waiting for your approval…"); GRANTED resumes, DENIED resolves as error. SUB_AGENT_* events nest as indented children under the open parallel_explore row. MCP/RAG lines get an immediate terminal INFO status (no eternal spinner). Arguments/results are truncated at write time (500 chars/line, 10 lines — AgentMcpLogPanel's limits) so no multi-MB strings enter Compose state.

**Always-on status line:** tool entries are always tracked in the model; the "Show tool activity in chat output" setting is snapshotted per message and only gates the detailed rows. AiBubble shows "Running <tool>… (step n/max)" / "Waiting for your approval…" while the run is in flight, removing the dead-silence failure mode.

**Approval plumbing:** APPROVAL_* events existed in AgentType (and AgentMcpLogPanel rendered them) but were never published — AgentApprovalService now emits REQUESTED before the dialog and GRANTED/DENIED after (DENIED also on the 120s timeout), gated on agentDebugLogsEnabled. AgentLoopTracker and the bus contract untouched.

**UI:** per-row status icons (pulsing spinner / green ✓ / red ✗ / ⏸), click-to-expand inline args+result, indented sub-agent children, "Open Logs" header link focusing the DevoxxGenieActivityLogs tool window (wired controller → screens → MessagePair → ActivitySection).

**Tests:** 10 new ViewModel tests (pairing by callNumber, error path, approval lifecycle incl. denied-then-response, unmatched/out-of-order tolerance, sub-agent nesting + orphan fallback, subAgentId disambiguation, MCP INFO status, truncation); 3 existing tests updated to the always-track contract. Also fixed pre-existing ReadFileToolExecutorTest failure (mocked ReadAction although commit 4c9b5ac2 moved the executor to ReadAccess). Full suite green: 2979 tests, 0 failures.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1109
<!-- SECTION:FINAL_SUMMARY:END -->
