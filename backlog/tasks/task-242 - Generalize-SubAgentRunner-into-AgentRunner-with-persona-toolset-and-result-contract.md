---
id: TASK-242
title: Generalize SubAgentRunner into AgentRunner with persona, toolset and result contract
status: To Do
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 10:30'
labels:
  - agent-mode
  - agent-team
  - sub-agents
  - refactoring
dependencies:
  - TASK-241
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/ReadOnlyToolProvider.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 1b of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §4.5).

Generalize `SubAgentRunner` (own model + own memory + own tracker + non-streaming AiServices)
into a role-driven `AgentRunner`:

- **Persona**: system prompt = `AgentDefinition.instruction` + the project-context fragments
  already assembled by `ChatMemoryManager.buildAugmentedSystemPrompt()`.
- **Model**: resolve `definition.modelProvider/modelName`; fallback is the *conversation's*
  active provider/model (replacing the current Ollama→OpenAI auto-detect for team agents).
  Per-agent `temperature` threads through `buildModelConfig()` instead of the global setting.
- **Tools**: new `TeamAgentToolProvider` filtering `BuiltInToolProvider` to
  `definition.allowedTools`, clamped to the parent conversation's effective toolset, wrapped in
  `AgentApprovalProvider` (dialogs labeled with the agent name) then a per-agent
  `AgentLoopTracker` honoring `definition.maxToolCalls`.
- **Result contract** (in-process analog of DockerAgents result.json): `execute()` always
  returns `AgentResult{agent, intent, status(OK|ERROR|TIMEOUT|CANCELLED), summary, toolCalls,
  durationMs, provider, model}` on every exit path — success, budget exhaustion, timeout,
  cancellation, model-creation failure.
- **Local-model resilience**: a local server rejecting tools-enabled requests yields a readable
  summary ("model does not support tool calling — pick a tool-capable model for agent '<name>'"),
  never a stack trace.

`parallel_explore`/`SubAgentRunner` behavior must not regress — SubAgentRunner becomes a thin
wrapper over AgentRunner (or delegates to it) with the existing read-only explorer profile.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 AgentRunner executes a definition with its own provider/model/memory/tools/budget, concurrently safe on the sub-agent pool.
- [ ] #2 AgentResult is returned on every exit path; unit tests cover all five terminal statuses.
- [ ] #3 Child tool allowlist is clamped to the parent conversation's effective toolset.
- [ ] #4 Per-agent temperature/budgets are threaded through — no global DevoxxGenieStateService reads inside the child loop for these values.
- [ ] #5 Existing parallel_explore tests stay green (no behavior change).
<!-- AC:END -->
