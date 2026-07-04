---
id: TASK-248
title: 'Optional remote execution backend: delegate_task via DockerAgents orchestrator-api'
status: To Do
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 10:30'
labels:
  - agent-mode
  - agent-team
  - interop
  - experimental
dependencies:
  - TASK-244
references:
  - docs/specs/agent-team-orchestration.md
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 4b of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §8).

Because `delegate_task`'s contract (spawn named agent with task+intent → collect
status+summary) is deliberately identical to the DockerAgents `/sessions` API contract
(`POST /sessions`, `GET /sessions/{id}/wait`, `GET /sessions/wait_all`,
result.json `{status, summary, parent_session_id, intent}`), add an execution-backend switch:

- **In-process** (default): AgentRunner threads as built in TASK-242/243.
- **Remote**: `delegate_task` POSTs to a configured DockerAgents `orchestrator-api` base URL
  and waits via `/wait` / `wait_all` (bounded polling with timeout retries, mirroring the
  POC's local-provider recipe), mapping result.json entries into the same structured tool
  result. Children then run in isolated Docker containers with the POC's own provider
  bindings, repo cache and lifetime controls.

Settings: backend selector + base URL + connectivity test (wraps `GET /agents`). Agent-name
validation against the remote `/agents` directory when the remote backend is active.

This keeps the IDE UX identical while letting heavyweight/isolated execution be offloaded —
and validates that the tool seam is genuinely backend-agnostic.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 Backend switch with in-process as default; remote requires a passing connectivity test.
- [ ] #2 Remote delegation round-trips: spawn → wait → structured summary entry, including timeout/error mapping.
- [ ] #3 Cancellation DELETEs the remote session(s).
- [ ] #4 Chat progress blocks (TASK-246) work identically for remote children.
<!-- AC:END -->
