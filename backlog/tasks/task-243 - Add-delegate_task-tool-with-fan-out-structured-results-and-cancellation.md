---
id: TASK-243
title: Add delegate_task tool with fan-out, structured results and cancellation
status: To Do
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 10:30'
labels:
  - agent-mode
  - agent-team
  - sub-agents
  - feature
dependencies:
  - TASK-242
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/prompt/threading/ThreadPoolManager.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 2 of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §4.6).

New `DelegateTaskToolExecutor` (sibling of `ParallelExploreToolExecutor`) exposing:

```json
delegate_task: { "tasks": [ { "agent": "...", "task": "...", "intent": "..." } ] }
```

Ported DockerAgents handoff guarantees:

1. Fail fast on unknown agent names — error listing available agents, nothing spawned.
2. One AgentRunner per task on the sub-agent pool; arrays fan out in parallel bounded by
   `subAgentParallelism`; the slowest child bounds the call (wait_all semantics).
3. Structured partial results — a failed/timed-out/cancelled child becomes a readable entry;
   one dead child never fails the batch.
4. Merged tool result contains per task only: agent, intent, status, provider:model,
   tool-call count and the child's summary — never child transcripts.
5. Implements `AgentLoopTracker.Cancellable`, registered on the parent tracker (Stop cancels
   all children, like parallel_explore).
6. Publishes `ActivityMessage`s (SUB_AGENT_STARTED/COMPLETED/ERROR) with agent name +
   provider:model label.

Depth-1 enforcement: `delegate_task` is registered only for the orchestrating conversation's
tool chain — a child AgentRunner's tool provider can never include it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 Unknown agent name returns an error with the available-agents list; no runner started.
- [ ] #2 Parallel tasks execute concurrently with per-task structured results; partial failure never fails the batch.
- [ ] #3 Parent Stop cancels all in-flight children; child memories are repaired (orphaned tool messages sanitized).
- [ ] #4 Tool result contains summaries only — verified no transcript leakage in tests.
- [ ] #5 Children cannot see delegate_task (depth-1 enforced structurally, covered by a test).
<!-- AC:END -->
