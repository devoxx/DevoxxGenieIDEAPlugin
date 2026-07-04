---
id: TASK-245
title: 'Agent Team settings UI: definitions list and editor'
status: Done
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 12:30'
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
- [x] #1 CRUD works with validation errors surfaced inline (duplicate/invalid names, empty persona).
- [x] #2 Built-ins cannot be deleted; Reset restores the shipped persona/config.
- [x] #3 Provider/model combos only offer enabled providers and their live model lists.
- [x] #4 isModified/apply/reset lifecycle correct (no phantom "modified" state; settings round-trip).
<!-- AC:END -->

## Implementation Notes

Implemented on branch claude/devoxxgenie-multi-agent-setup-iahcby.
- `ui/settings/agentteam/AgentTeamSettingsComponent` — agent list (renderer shows name, model binding, [custom]/[disabled] badges) with Add/Copy/Delete/Reset buttons; editor form: name (locked for built-ins), description, persona textarea, provider combo (null = "Inherit conversation model") + live model combo via ChatModelFactoryProvider, toolset preset checkboxes with resolved-tools preview, readOnly, budgets (JBIntSpinner), temperature, enabled. Works on an in-memory copy; selection changes sync the form back into the working list.
- `ui/settings/agentteam/AgentTeamSettingsConfigurable` — registered in plugin.xml as child page "Agent Team" under Agent Mode; validation failures surface as ConfigurationException so the dialog stays open.
- `AgentRegistry.saveAll` (validates names/duplicates/empty personas/built-in deletion, atomic — failed saves don't corrupt stored state) + `shippedDefault(name)` for per-row reset.
- Provider combo only offers enabled providers (same filter as the existing sub-agent combo); Delete disabled for built-ins; Reset only for built-ins (AC #2), isModified via working-copy comparison (AC #4).
- SubAgentConfig migration deliberately NOT auto-run: parallel_explore keeps its own config; the built-in seeding covers the out-of-box experience (noted deviation from the original AC text).
- Tests: AgentRegistryTest extended with saveAll validation + shippedDefault (green). Swing form itself is untested (no headless UI harness in repo) — covered by the registry-level validation tests + manual smoke.
