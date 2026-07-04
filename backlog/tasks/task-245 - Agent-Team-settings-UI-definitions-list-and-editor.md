---
id: TASK-245
title: 'Agent Team settings UI: definitions list and editor'
status: To Do
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 10:30'
labels:
  - agent-mode
  - agent-team
  - ui
  - settings
dependencies:
  - TASK-241
references:
  - docs/specs/agent-team-orchestration.md
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsConfigurable.java
  - src/main/java/com/devoxx/genie/service/LLMProviderService.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 3b of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §5).

New "Agent Team" tab in Settings → Agent (`ui/settings/agent/`):

- List of `AgentDefinition`s with Add / Copy / Edit / Delete (built-ins: Edit + Reset-to-default
  only, no Delete), enabled checkbox per row.
- Editor: name, description, persona textarea, provider combo + model combo (reuse the
  `AgentConfigRow` pattern; populate via `LLMProviderService.getAvailableModelProviders()` —
  do not duplicate the provider-enabled predicate), toolset preset checkboxes with a resolved
  tool-list preview, maxToolCalls / timeoutSeconds / temperature fields.
- Migration: existing `List<SubAgentConfig>` rows are offered as read-only explorer
  AgentDefinitions when the tab is first opened (no silent behavior change; parallel_explore
  settings remain functional either way).

Mirrors the DockerAgents web UI's Agents screen (list + detail editor) in Swing settings form.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 CRUD works with validation errors surfaced inline (duplicate/invalid names, empty persona).
- [ ] #2 Built-ins cannot be deleted; Reset restores the shipped persona/config.
- [ ] #3 Provider/model combos only offer enabled providers and their live model lists.
- [ ] #4 isModified/apply/reset lifecycle correct (no phantom "modified" state; settings round-trip).
<!-- AC:END -->
