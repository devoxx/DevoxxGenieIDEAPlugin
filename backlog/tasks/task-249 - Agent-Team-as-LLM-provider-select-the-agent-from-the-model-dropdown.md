---
id: TASK-249
title: 'Agent Team as LLM provider: select the agent from the model dropdown'
status: Done
assignee: []
created_date: '2026-07-05 07:10'
updated_date: '2026-07-05 07:40'
labels:
  - agent-mode
  - agent-team
  - ui
  - feature
dependencies:
  - TASK-244
  - TASK-245
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/chatmodel/agentteam/AgentTeamChatModelFactory.java
  - src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java
  - src/main/java/com/devoxx/genie/service/agent/team/AgentRegistry.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
User request: instead of Agent Team being an invisible conversation mode, expose it in the
main LLM Provider dropdown. Selecting "Agent Team" as the provider lists the team's agents
in the model-name dropdown (orchestrator, implementer, reviewer, …); the selected agent
handles the prompt.

Follows the existing pseudo-provider pattern (CLIRunners/ACPRunners):

- `ModelProvider.AgentTeam` ("Agent Team", Type.LOCAL), listed only when Agent Team mode is
  enabled (conditional getter in `LLMProviderService`, like the CLI/ACP runners).
- `AgentTeamChatModelFactory`: `getModels()` = orchestrator first + enabled agents;
  `createChatModel`/`createStreamingChatModel` delegate to the agent's bound provider
  factory (fallback: Ollama → OpenAI), so streaming and the normal execution pipeline work
  unchanged.
- The orchestrator becomes a built-in `AgentDefinition` (its underlying model binding is
  user-editable on the Agent Team settings page). It is never delegable — no
  self-delegation, enforced in `DelegateTaskToolExecutor` and excluded from the catalog
  (`AgentRegistry.getDelegable`).
- Selecting the **orchestrator** = the existing team mode (coordinator mandate + catalog +
  delegate_task).
- Selecting a **specialist** runs the conversation AS that agent: its persona replaces the
  coordinator fragment (`<AGENT_PERSONA>` in `ChatMemoryManager`) and its preset-scoped,
  approval-gated toolset replaces the orchestrator chain (`AgentToolProviderFactory`),
  exactly matching what a delegation would give it (no delegate_task).
- Registry top-up seeding adds new built-ins (orchestrator) to lists persisted by earlier
  builds; `AgentRunner` guards against inheriting the pseudo-provider (resolves the agent's
  own binding instead of recursing).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [x] #1 "Agent Team" appears in the provider dropdown only when Agent Team mode is enabled.
- [x] #2 Model dropdown lists orchestrator (first) + enabled agents; disabled agents are hidden.
- [x] #3 Specialist selection: persona system prompt + scoped tools; orchestrator selection: team fragment + delegate_task.
- [x] #4 Orchestrator is not delegable and absent from the delegation catalog.
- [x] #5 Older persisted agent lists are topped up with the orchestrator built-in.
<!-- AC:END -->

## Implementation Notes

Implemented on branch claude/devoxxgenie-multi-agent-setup-iahcby.
- All exhaustive `switch (provider)` sites updated (LlmProviderPanel, AgentSettingsComponent ×2, AgentTeamSettingsComponent); ChatModelFactoryProvider completeness test covers the factory registration.
- Tests: AgentTeamChatModelFactoryTest (model list, disabled agents, bound-provider resolution, unknown-agent error), AgentRegistryTest (top-up seeding, delegable filtering, selectedDirectAgent), AgentTeamPromptTest (persona vs team fragment per selection), DelegateTaskToolExecutorTest (orchestrator delegation rejected) — green.
- Manual smoke in a real IDE still needed: dropdown UX, streaming via a bound provider, model-combo refresh after editing the team.
