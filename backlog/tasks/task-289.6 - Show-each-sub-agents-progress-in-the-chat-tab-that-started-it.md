---
id: TASK-289.6
title: Show each sub-agent's progress in the chat tab that started it
status: To Do
assignee: []
created_date: '2026-11-28T10:52:00Z'
updated_date: '2026-11-28T10:52:00Z'
labels:
  - agent
  - sub-agents
  - ui
dependencies:
  - TASK-289.2
  - TASK-288
references:
  - >-
    backlog/tasks/task-289 -
    Improve-parallel-sub-agents-based-on-Codex-multi-agent-architecture.md
  - >-
    backlog/tasks/task-288 -
    Show-live-progress-while-an-agent-run-is-in-flight.md
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AgentProgressLine.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
parent_task_id: TASK-289
priority: medium
type: enhancement
ordinal: 47000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
From the TASK-289 review (findings B5, B11, B12; roadmap R8/R9 says to deliver this before considering async agent tools).

While `parallel_explore` runs, the user cannot see what each sub-agent is doing. The sub-agent events that exist are published only when debug logging is on, so they are not a reliable progress feed. Their start/finish order reflects submission and collection order rather than what actually happened, some terminal events are missing, and events do not carry which sub-agent or which conversation they belong to. Simply removing the debug gate would create orphan rows in other tabs.

TASK-288 added a live progress line for the main agent (running tool, "Waiting for the model", tool-call count, elapsed time) and explicitly left sub-agent progress out of scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 With debug logging off, the chat tab that started a parallel_explore call shows each sub-agent as queued, running or finished (completed, failed, cancelled or timed out) while the call runs
- [ ] #2 Every sub-agent event identifies its sub-agent and its conversation, and is shown only in the owning tab; a test with two active tabs shows no cross-tab rows
- [ ] #3 Start and finish events reflect when each sub-agent actually started and finished, and every sub-agent emits exactly one terminal event
- [ ] #4 The existing debug Activity Log output is unchanged when debug logging is on
<!-- AC:END -->
