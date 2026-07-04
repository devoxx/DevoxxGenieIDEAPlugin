---
id: TASK-241
title: 'Agent Team foundations: AgentDefinition model, registry and persistence'
status: To Do
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 10:30'
labels:
  - agent-mode
  - agent-team
  - sub-agents
  - feature
dependencies: []
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/model/agent/SubAgentConfig.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 1a of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §4.3–4.4, §7).

Introduce the named-agent data model and registry that everything else builds on:

- New `model/agent/AgentDefinition` POJO: `name`, `description`, `instruction` (persona),
  `modelProvider`/`modelName` ("" = inherit conversation model), `allowedTools`, `readOnly`,
  `maxToolCalls`, `timeoutSeconds`, `temperature`, `builtIn`, `enabled`.
- Persist `List<AgentDefinition> agentDefinitions` on `DevoxxGenieStateService` (same
  XML-serialized pattern as `subAgentConfigs`/`customPrompts`).
- New `service/agent/team/AgentRegistry` application service: `getAll()`, `byName(String)`,
  `buildCatalogPrompt()` (markdown table of name/description/model for the orchestrator system
  fragment), name validation (`^[a-z][a-z0-9-]{1,31}$`), and first-run seeding of the built-in
  personas: architect, implementer, reviewer (read-only tools), documentalist (fetch-only) —
  ported from DockerAgents/agents/*.yml; plus the orchestrator persona text used as the
  team-mode system fragment (not a spawnable agent).
- Toolset presets (`filesystem-ro`, `filesystem`, `shell`, `fetch`, `analysis`) resolving to
  concrete built-in tool names.

Built-ins are editable and reset-able but not deletable. Existing `SubAgentConfig` is untouched
(parallel_explore keeps working; migration handled in TASK-245).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 `AgentDefinition` persists and round-trips through `DevoxxGenieStateService` XML serialization.
- [ ] #2 `AgentRegistry` seeds the built-in personas exactly once and validates unique, well-formed names.
- [ ] #3 `buildCatalogPrompt()` renders enabled agents with name, description and provider:model label.
- [ ] #4 Toolset presets resolve to concrete tool names and honor `getDisabledAgentTools()`.
- [ ] #5 Unit tests cover seeding, validation, preset resolution and persistence round-trip.
<!-- AC:END -->
