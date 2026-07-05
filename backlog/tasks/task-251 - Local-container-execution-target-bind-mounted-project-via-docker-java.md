---
id: TASK-251
title: 'Local container execution target: bind-mounted project via docker-java'
status: To Do
assignee: []
created_date: '2026-07-05 08:00'
updated_date: '2026-07-05 08:00'
labels:
  - agent-mode
  - agent-team
  - isolation
  - feature
dependencies:
  - TASK-250
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/model/agent/AgentExecutionTarget.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase B of per-agent isolation: a third `AgentExecutionTarget.LOCAL_CONTAINER` that
spawns the agent in a Docker container on the developer's machine with the **project
directory bind-mounted** — process/host isolation while keeping the "edits appear in my
open editor" IDE semantics that the DockerAgents clone-based backend gives up.

Design sketch:

- Use the plugin's existing **docker-java** dependency (already shipped for ChromaDB) —
  no orchestrator-api needed.
- Run the DockerAgents `geniebuilder-agent-runner` image (or a slim variant); mount the
  project at `/session/repo` — **read-only** when the agent's readOnly flag is set,
  read-write otherwise. `cap_drop=ALL`, `no-new-privileges`, mem/cpu/pids limits
  mirroring DockerAgents `PROFILE_CAPS`; network optional per agent.
- Inject the persona via the Genie YAML export (TASK-247 mapper) into a temp mount, or
  `--append-system-prompt`; provider credentials/hosts per the agent's binding
  (host.docker.internal for local providers).
- Result contract: the runner's `result.json` → `AgentResult`; stream stdout events to
  the activity bus so TASK-246 progress blocks work unchanged.
- Cancellation = stop/remove the container; timeout = per-agent budget.

Open questions to resolve during implementation:
- Approval semantics: a rw bind mount bypasses per-call IDE approval — consider
  requiring a git-diff review step before accepting the summary, or defaulting rw agents
  to DOCKER_AGENTS (clone + branch) instead.
- Windows/macOS bind-mount performance and path mapping.
- VFS refresh after container writes so the editor picks up changes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 LOCAL_CONTAINER target spawns a container with the project bind-mounted (ro/rw per agent readOnly flag) and returns a structured AgentResult.
- [ ] #2 Resource caps + cap_drop applied; container removed on completion/cancel/timeout.
- [ ] #3 Progress blocks and log panel work identically to the other targets.
- [ ] #4 Editor sees container-made changes (VFS refresh) and the diff is reviewable before the orchestrator continues.
<!-- AC:END -->
