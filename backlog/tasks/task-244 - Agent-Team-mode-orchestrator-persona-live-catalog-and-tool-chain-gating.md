---
id: TASK-244
title: 'Agent Team mode: orchestrator persona, live catalog and tool-chain gating'
status: In Progress
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 11:45'
labels:
  - agent-mode
  - agent-team
  - orchestration
  - feature
dependencies:
  - TASK-243
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 3a of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §4.7).

Turn the existing main agent loop into the orchestrator — no new loop:

- Conversation-level **Agent Team** toggle (`agentTeamEnabled` on state service + a switch next
  to the agent-mode control; enabled only when agent mode is on and ≥1 specialist is enabled).
- When on, `AgentToolProviderFactory` adds `delegate_task` to the tool chain and (configurable,
  default ON) strips direct write/run tools from the orchestrator — the structural version of
  DockerAgents' "pure coordinator / STRICT FORBIDDEN ACTIONS" persona rule.
- `ChatMemoryManager.buildAugmentedSystemPrompt()` appends an `<AGENT_TEAM_INSTRUCTION>`
  fragment: coordinator mandate (understand → break down → delegate → track → synthesize;
  one-shot children; self-contained task prompts; read only summaries), the live agent catalog
  from `AgentRegistry.buildCatalogPrompt()`, and a delegation decision guide — the in-process
  analog of DockerAgents' runtime DELEGATION TRANSPORT addendum with its live /agents table.
- Team mode raises the orchestrator's effective tool-call budget (delegations are cheap
  round-trips; mirror DockerAgents giving its orchestrator LOCAL_MAX_TURNS=200).
- Streaming, memory, cost display and Stop behave exactly as today.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [x] #1 Toggle gates everything; OFF = zero behavior change (default OFF).
- [x] #2 System prompt contains the team fragment with the live catalog only when team mode is on (platform test on prompt assembly).
- [x] #3 Orchestrator write/run tools stripped when the pure-coordinator option is on; delegate_task present.
- [ ] #4 Works with a local-provider orchestrator model end-to-end (manual smoke: local orchestrator + cloud implementer).
<!-- AC:END -->

## Implementation Notes

Implemented on branch claude/devoxxgenie-multi-agent-setup-iahcby (code complete; AC #4 manual hybrid smoke still to be run in a real IDE).
- `agentTeamEnabled` + `agentTeamPureCoordinator` flags on DevoxxGenieStateService with checkboxes in Settings → Agent → "Agent Team (Experimental)" (full definitions editor remains TASK-245).
- `ChatMemoryManager.buildAugmentedSystemPrompt` appends `<AGENT_TEAM_INSTRUCTION>` (orchestrator mandate + live catalog from AgentRegistry) only when agent mode AND team mode are on.
- `AgentToolProviderFactory`: delegate_task in the chain via BuiltInToolProvider gate; `CoordinatorToolFilter` strips write/run tools from the orchestrating conversation when pure-coordinator is on (children unaffected — they build their own scoped providers).
- Tests: `AgentTeamPromptTest`, `CoordinatorToolFilterTest` (green).
