---
id: TASK-246
title: 'Chat UI: delegation progress blocks and per-agent activity labels'
status: Done
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 13:15'
labels:
  - agent-mode
  - agent-team
  - ui
dependencies:
  - TASK-243
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
  - src/main/java/com/devoxx/genie/model/activity/ActivityMessage.java
  - src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 3c of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §5.3).

Render delegations as first-class progress in the conversation instead of flat debug log lines:

- Extend `ActivityMessage` payload with agent name, intent and provider:model label (the
  `subAgentId` field already exists).
- Compose view renders a collapsible block per delegation:
  `🤖 reviewer (Ollama · qwen3.6) — running… → ✓ done (first summary line)` with status
  transitions driven by SUB_AGENT_STARTED/COMPLETED/ERROR events (existing
  `ACTIVITY_LOG_MSG` subscription in `ConversationPanel` → viewController).
- Blocks show progress + status, not transcripts; the child summary appears via the
  orchestrator's final answer.
- `AgentMcpLogPanel` lines use the `agentName:Provider:model` label (extend the existing
  sub-agent label format).
- Respect the existing `showToolActivityInChat` gate; delegation blocks shown by default in
  team mode.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [x] #1 Each delegation renders one block with live status transitions (running → done/failed/cancelled).
- [x] #2 Parallel fan-out renders one block per child, updating independently.
- [x] #3 Log panel lines carry agent name + provider:model.
- [x] #4 EDT-safe: all rendering driven via the message bus, no direct cross-thread UI calls.
<!-- AC:END -->

## Implementation Notes

Implemented on branch claude/devoxxgenie-multi-agent-setup-iahcby.
- `ActivityMessage`/`AgentMessage`: new `agentModelLabel` field ("Ollama · qwen3"), mapped in `fromAgent`.
- `DelegateTaskToolExecutor`: subAgentId now stays the bare agent name on every event of one delegation (was inconsistently `result.label()` on completion — would have broken keyed row updates); provider·model travels in the new label field, resolved from the definition (start) / AgentResult (completion).
- `ConversationViewModel.handleSubAgentEvent`: recognizes `delegate_task` as a parent alongside `parallel_explore`; children keyed by `subAgentId ?: toolName` so the completion event resolves the started row in place; children/orphans carry subAgentId + agentLabel; RUNNING children get `startedAt` for the elapsed ticker.
- `ActivityEntryUiModel`: new `agentLabel` field.
- `ActivitySection.ActivityEntryRow`: delegate_task children render "🤖 reviewer (Ollama · qwen3)"; tool rows executed INSIDE a sub-agent keep their action summary (only delegate_task rows are agent-identity rows). Status icon transitions (pulsing dot → ✓/✗) come free from the existing StatusIcon.
- `AgentMcpLogPanel`: sub-agent lines append the provider·model label.
- Tests: 3 new ConversationViewModelTest cases (keyed transition, error resolve, orphan fallback) — green. EDT safety unchanged (message-bus → snapshot copy).
