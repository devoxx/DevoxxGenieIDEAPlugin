---
id: TASK-246
title: 'Chat UI: delegation progress blocks and per-agent activity labels'
status: To Do
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 10:30'
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
- [ ] #1 Each delegation renders one block with live status transitions (running → done/failed/cancelled).
- [ ] #2 Parallel fan-out renders one block per child, updating independently.
- [ ] #3 Log panel lines carry agent name + provider:model.
- [ ] #4 EDT-safe: all rendering driven via the message bus, no direct cross-thread UI calls.
<!-- AC:END -->
