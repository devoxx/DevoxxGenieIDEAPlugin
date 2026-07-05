---
id: TASK-250
title: 'Per-agent execution target: mixed in-process and DockerAgents container delegations'
status: Done
assignee: []
created_date: '2026-07-05 08:00'
updated_date: '2026-07-05 08:30'
labels:
  - agent-mode
  - agent-team
  - isolation
  - feature
dependencies:
  - TASK-248
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/model/agent/AgentExecutionTarget.java
  - src/main/java/com/devoxx/genie/service/agent/tool/DelegateTaskToolExecutor.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase A of per-agent isolation ("run in docker container per team member"). Replaces the
global TASK-248 remote toggle with a per-agent execution target, because agent risk is
asymmetric: read-only agents gain nothing from container overhead, while an implementer
running build commands is exactly what should be sandboxed.

- `AgentExecutionTarget` enum: `IN_PROCESS` (default) | `DOCKER_AGENTS`; stored as a
  String on `AgentDefinition` (`executionTarget`) for XML-serialization stability, with a
  null/unknown-safe `effectiveExecutionTarget()` accessor.
- `DelegateTaskToolExecutor` routes **per task**: one delegate_task fan-out can mix
  in-process runners and DockerAgents container sessions. The backend and its /agents
  directory are prepared once per call; remote-side problems (no URL, unreachable api,
  agent missing remotely) degrade to structured per-task error entries so co-scheduled
  in-process tasks still run (wait_all semantics). Remote tasks keep the +60s transport
  margin over the server-side wait; cancellation still DELETEs active remote sessions.
- Settings UI: "Execution target" combo in the agent editor (disabled for the
  orchestrator — it is never delegated), a `[container]` badge in the agent list, and the
  remote section becomes a plain "DockerAgents Connection" (URL + Test) with the global
  enable checkbox removed. `agentTeamRemoteEnabled` is deprecated (kept for XML compat,
  no longer read).
- Execution target applies to delegations only; direct chat via the Agent Team dropdown
  stays in-process (a streamed interactive conversation doesn't map to a one-shot
  container).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [x] #1 Per-agent target persisted and editable; default IN_PROCESS; orchestrator has no target.
- [x] #2 Mixed fan-out: in-process and container tasks in one delegate_task call, each routed by its agent's target.
- [x] #3 Remote-side failures (no URL / unreachable / agent not exported) become structured per-task errors, never batch failures, and never spawn anything.
- [x] #4 Global remote toggle removed from UI and executor; legacy setting deprecated but still deserializes.
<!-- AC:END -->

## Implementation Notes

Implemented on branch claude/devoxxgenie-multi-agent-setup-iahcby.
- Tests: `AgentExecutionTargetTest` (parse/default), `DelegateTaskToolExecutorTest` (unreachable backend → structured error without spawning; missing URL → structured error; case-insensitive target parse) — green alongside the agent/memory/factory suites.
- Manual smoke still needed: mixed fan-out against a live compose stack (local reviewer + containerized implementer in one prompt).
- Phase B (workspace-preserving local containers) tracked as TASK-251.
