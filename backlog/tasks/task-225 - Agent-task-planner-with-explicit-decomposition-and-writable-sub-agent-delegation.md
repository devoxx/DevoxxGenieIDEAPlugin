---
id: TASK-225
title: Agent task planner with explicit decomposition and writable sub-agent delegation
status: To Do
assignee: []
created_date: '2026-06-05 10:17'
updated_date: '2026-06-05 10:17'
labels:
  - agent-mode
  - planner
  - sub-agents
  - feature
  - hermes
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - 'https://github.com/NousResearch/hermes-agent'
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add an explicit **Planner** and a **writable `delegate_task`** capability to Agent Mode. In Hermes Agent the Planner "breaks tasks into sub-tasks and spawns sub-agents", and `delegate_task` "spawns child agent instances with isolated context, restricted toolsets, and their own terminal sessions". DevoxxGenie has the *spawning* half but neither an explicit plan nor writable delegation.

### Current state (from codebase exploration)

- `ParallelExploreToolExecutor` + `SubAgentRunner` already spawn 2–5 sub-agents with isolated `MessageWindowChatMemory`, their own (configurable) model, an `AgentLoopTracker` with a lower call limit, per-thread timeout, and cancellation propagation registered on the parent tracker.
- **Limitation 1 — read-only only.** Sub-agents get a `ReadOnlyToolProvider` (only `read_file` / `list_files` / `search_files`). They can explore but cannot *do* a sub-task (no write/edit/run). This makes `parallel_explore` a research fan-out, not true delegation.
- **Limitation 2 — no explicit plan.** Task decomposition is implicit in the LLM's prompting. There is no user-visible plan, no checklist, no per-sub-task status. The "Planner" box in the Hermes diagram has no DevoxxGenie equivalent.

This task fills both gaps while reusing the existing sub-agent machinery.

### Design principles

- **Reuse, don't rebuild.** Build on `SubAgentRunner` and the `AgentLoopTracker` cancellation tree. `delegate_task` is `parallel_explore`'s sibling with a broader (still scoped + approval-gated) toolset.
- **Plan is transparent.** The plan must be visible to the user (rendered as a checklist in the output panel) so autonomous multi-step work stays inspectable — this is also a safety property.
- **Writable delegation stays safe.** A delegated sub-agent that can write/run must still honour `AgentApprovalProvider` (write approvals bubble to the user) and a bounded tool-call budget. Default the delegated toolset to read-only-plus-scoped-write, opt-in, and never broader than the parent's allowed set.

## Implementation Plan

### 1. `plan_task` tool / planning pre-pass

- Add a `plan_task(goal)` built-in tool (registered in `BuiltInToolProvider`, gated by a `taskPlannerEnabled` flag) that asks the model to emit an ordered list of sub-tasks as structured output (id, title, optional dependency ids, suggested toolset scope).
- Persist the plan for the current run (in-memory, keyed like the agent loop) and render it as a **checklist** in `PromptOutputPanel`, updating each item's status (pending / in-progress / done / failed) as the run proceeds. Reuse existing output-panel rendering rather than introducing a new UI surface.
- `plan_task` is read-only (planning produces no side effects) → add to `AgentApprovalProvider.READ_ONLY_TOOLS`.

### 2. `delegate_task` tool (writable sibling of parallel_explore)

- New `DelegateTaskToolExecutor` under `service/agent/tool/`, modelled on `ParallelExploreToolExecutor` but for a single (or small set of) delegated sub-task(s) that may modify the workspace.
- Each delegated sub-agent uses a `SubAgentRunner` configured with a **scoped, opt-in writable** tool provider (e.g. read tools + `edit_file`/`write_file`/`run_command` only when the parent enabled delegation-writes), a dedicated `AgentLoopTracker` (lower budget), its own memory, and timeout — all already supported.
- Cancellation: register each delegated runner as a `Cancellable` child of the parent tracker (same pattern `ParallelExploreToolExecutor` already uses) so user cancellation stops the whole tree.
- Approval: delegated write/run calls route through the parent's `AgentApprovalProvider`, so the user still sees and approves writes initiated by sub-agents. The delegated toolset must never exceed the parent's permitted set.
- Result: collect each sub-task's outcome and merge into a markdown summary returned to the parent agent (mirroring `parallel_explore`'s result merge), and update the plan checklist statuses.

### 3. Settings & gating

- Add to `DevoxxGenieStateService`: `taskPlannerEnabled` (default false), `delegateTaskEnabled` (default false), `delegateAllowWrites` (default false), and a delegated sub-agent tool-call budget (reuse existing sub-agent parallelism/limit fields where possible).
- Add a "Planner & Delegation" section to `AgentSettingsComponent` with these toggles and clear help text warning that delegated writes are still approval-gated.

### 4. Tests

- Unit test `plan_task` output parsing (well-formed plan → checklist model; malformed → graceful error string).
- Test `DelegateTaskToolExecutor` toolset scoping: with `delegateAllowWrites=false` the delegated provider exposes read-only tools only; with it true, the scoped write tools appear but never exceed the parent's set.
- Test cancellation propagation: cancelling the parent `AgentLoopTracker` stops delegated runners (assert the `Cancellable` registration + flag broadcast).
- Test that delegated write calls are still gated by `AgentApprovalProvider`.
- Verify graceful degradation when both flags are off (neither tool registered).

## Out of scope (capture as follow-ups)

- Hermes' separate terminal/session per sub-agent across remote backends (Docker/SSH/Modal/Daytona) — DevoxxGenie sub-agents run in-process against the local workspace.
- Automatic re-planning / plan repair on sub-task failure (v1: surface failure in the checklist and let the parent agent decide).
- Persisting plans across IDE restarts.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `plan_task(goal)` built-in tool (gated by `taskPlannerEnabled`, default false) produces an ordered, structured list of sub-tasks with ids and optional dependencies
- [ ] #2 The generated plan is rendered as a status-tracking checklist in the output panel and each item's status updates (pending/in-progress/done/failed) as the run proceeds
- [ ] #3 `plan_task` is registered as read-only in `AgentApprovalProvider.READ_ONLY_TOOLS`
- [ ] #4 A `delegate_task` built-in tool (gated by `delegateTaskEnabled`, default false) spawns a sub-agent via the existing `SubAgentRunner` to execute a delegated sub-task
- [ ] #5 Delegated sub-agents default to a read-only scoped toolset; write/run tools are exposed only when `delegateAllowWrites` is enabled, and the delegated toolset never exceeds the parent agent's permitted set
- [ ] #6 Write/run calls made by a delegated sub-agent still route through the parent `AgentApprovalProvider` and require user approval
- [ ] #7 Each delegated sub-agent runs with its own `AgentLoopTracker` budget and timeout, and is registered as a Cancellable child so cancelling the parent stops the whole sub-agent tree
- [ ] #8 An Agent Mode "Planner & Delegation" settings section exposes the planner/delegation/allow-writes toggles and budget, persisted via `DevoxxGenieStateService`
- [ ] #9 Unit tests cover plan parsing, delegated-toolset scoping (writes off vs on), approval gating of delegated writes, and cancellation propagation
- [ ] #10 With both flags off, neither tool is registered; plugin builds (`./gradlew buildPlugin`) and all tests pass (`./gradlew test`)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Originated from research comparing DevoxxGenie's agent mode to Nous Research's Hermes Agent (https://github.com/NousResearch/hermes-agent). Hermes' "Planner" breaks tasks into sub-tasks and spawns sub-agents, and its `delegate_task` spawns child agents with isolated context and restricted toolsets. DevoxxGenie already has the spawning primitive (`ParallelExploreToolExecutor` + `SubAgentRunner`, with isolated memory, per-sub-agent `AgentLoopTracker`, timeout, and cancellation propagation) but (a) sub-agents are read-only via `ReadOnlyToolProvider`, and (b) there is no explicit, user-visible plan. This task adds an explicit `plan_task` (rendered as a checklist) and a writable, approval-gated `delegate_task` that reuses the same machinery.

Reuse points: `SubAgentRunner` (sub-agent execution context, configurable model/memory/limit/timeout), `ParallelExploreToolExecutor` (thread-pool fan-out + result merge + Cancellable registration pattern), `AgentLoopTracker` (budget + cancellation tree), `AgentApprovalProvider` (write approvals — keep delegated writes gated), `BuiltInToolProvider` (flag-gated registration). Per CLAUDE.md: feature branch before code changes; UI/checklist updates on the EDT, long-running sub-agent work off it; behavioural tests first where practical. Keep scope tight — no remote backends, no automatic re-planning in v1.
<!-- SECTION:NOTES:END -->
