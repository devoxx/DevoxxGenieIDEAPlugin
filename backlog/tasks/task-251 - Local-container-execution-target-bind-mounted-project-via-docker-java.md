---
id: TASK-251
title: 'Local container execution target: bind-mounted project via docker-java'
status: In Progress
assignee: []
created_date: '2026-07-05 08:00'
updated_date: '2026-07-05 09:10'
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
- [x] #3 Progress blocks and log panel work identically to the other targets.
- [ ] #4 Editor sees container-made changes (VFS refresh) and the diff is reviewable before the orchestrator continues.
<!-- AC:END -->

## Implementation Notes

Code complete on branch claude/devoxxgenie-multi-agent-setup-iahcby; status stays In Progress
until the live-Docker ACs (#1, #2, #4) are verified in a manual smoke (this dev sandbox has no
usable Docker daemon).

- `AgentExecutionTarget.LOCAL_CONTAINER` ("Local container (project mounted)").
- `service/agent/team/LocalContainerAgentRunner` — docker-java (existing DockerUtil client):
  project bind-mounted at /session/repo (ro when the agent is read-only, rw otherwise), persona
  exported as Genie YAML (with the agent's binding resolved — no "conversation model" exists in
  a container) mounted at /agents, temp artifacts dir at /artifacts. Env carries
  AGENT_NAME/TASK_PROMPT/SESSION_ID/NEEDS_REPO=0/MAX_SESSION_SECONDS plus the provider's
  host/credential (localhost URLs rewritten to host.docker.internal; sk-ant-oat prefix routes to
  ANTHROPIC_AUTH_TOKEN like the DockerAgents api). Caps: 2g/2cpu/512 pids, cap_drop ALL,
  no-new-privileges, extra_hosts host-gateway. result.json parsed via the shared
  RemoteAgentBackend.parseResultPayload (label "container"); missing result.json / missing image
  ("build with bin/build.sh or change the image in settings") / dead daemon all yield readable
  AgentResult errors. Container force-removed on every path; temp dir cleaned.
- Approval (AC #4 first half): spawning a READ-WRITE container is itself gated through
  AgentApprovalService (a rw mount bypasses per-edit approval, so the spawn is the approval
  point); read-only spawns are not gated.
- Diff visibility (AC #4 second half): after a successful rw run the summary is annotated with
  `git status --porcelain` output ("review before committing") and the project VFS is refreshed
  asynchronously so the editor shows the changes.
- Executor routing: third branch in DelegateTaskToolExecutor with indexed cancellation
  (timeout/Stop stops+removes the container); outer wait margin 90s for containerized targets so
  the runner's own timeout result wins.
- Settings: `agentTeamLocalContainerImage` (default geniebuilder-agent-runner:latest) editable in
  the "Container Execution" section; agent list shows a [local container] badge.
- Tests (green): hostAccessibleUrl rewriting, buildEnv mapping (Ollama host), cancelled-before-
  start, no-base-path error, git-status degradation, plain result.json parsing with "container"
  label, LOCAL_CONTAINER enum parse.

Remaining for Done: manual smoke with Docker — ro reviewer run, rw implementer run (approval
dialog → git status annotation → VFS refresh), Stop mid-run removes the container.
