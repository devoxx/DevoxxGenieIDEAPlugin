---
id: TASK-257
title: Let users edit built-in agent tool descriptions from Settings
status: Done
assignee: []
created_date: '2026-08-24 10:21'
updated_date: '2026-08-24 10:34'
labels:
  - agent
  - settings
  - enhancement
dependencies: []
modified_files:
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptions.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/ReadOnlyToolProvider.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/agent/ToolDescriptionEditorDialog.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptionsTest.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/BuiltInToolProviderTest.java
  - docusaurus/docs/features/agent-mode.md
priority: medium
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Today the LLM-facing description of every built-in agent tool (read_file, edit_file, run_command, the PSI tools, …) is a hard-coded string literal in BuiltInToolProvider. Users who want to steer the agent — e.g. disable edit_file and tell the model "for all edits, use run_command instead" — can disable the tool (that already works via disabledAgentTools) but cannot change what the remaining tools tell the model.

This task makes those descriptions user-editable and persisted, so a user can tailor agent behaviour without a plugin change.

Scope is built-in tools only. MCP tool descriptions and the hard-coded agent guidance in ChatMemoryManager are explicitly OUT of scope.

Key constraint discovered during investigation: the short strings currently shown next to each checkbox in the Agent settings panel (CORE_AGENT_TOOLS in AgentSettingsComponent, PsiToolCatalog) are NOT the descriptions sent to the LLM — they are separate, shorter UI labels. The edit UI must show and edit the real LLM-facing text, otherwise users edit strings the model never sees.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The LLM-facing description of each built-in agent tool can be viewed and edited from the Agent settings panel via a per-tool edit affordance
- [x] #2 Each tool with a customised description offers a reset-to-default action that restores the shipped text
- [x] #3 Customised descriptions persist across IDE restarts
- [x] #4 An agent run sends the customised description to the model in place of the shipped one, for built-in tools
- [x] #5 MCP tool descriptions and Skills tool descriptions are unaffected by these overrides
- [x] #6 A tool whose description is customised is visually distinguishable from one using the shipped default in the settings panel
- [x] #7 The settings panel displays the real LLM-facing description text, not a separate shorter UI label
- [x] #8 Overrides survive a tool being renamed or removed without breaking agent startup (unknown names are ignored)
- [x] #9 Unit tests cover: override applied, reset to default, unknown tool name ignored, and non-built-in tools left untouched
- [x] #10 The Agent settings section is documented so users can discover the feature
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Centralise the LLM-facing descriptions of every tool that BuiltInToolProvider/ReadOnlyToolProvider register from their own inline specs into a new `BuiltInToolDescriptions` catalog (20 tools: 8 core + 9 PSI + run_tests/parallel_explore/web_search).
2. Resolve the user override inside the catalog (`effective(name)`) rather than decorating the tool provider chain — this keeps MCP and Skills tools untouched by construction and needs no extra link in the provider chain.
3. Persist overrides as `Map<String,String> toolDescriptionOverrides` on DevoxxGenieStateService.
4. Add a pencil button + "custom description" marker to each tool row in the Agent settings panel, backed by a `ToolDescriptionEditorDialog` with a Reset to Default action.
5. Tests + docs.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Chose catalog-resolution over a `ToolProvider` decorator. A decorator sitting after CompositeToolProvider would also have worked (AiServiceTool.toBuilder()/ToolSpecification.toBuilder() both exist in the langchain4j version in use), but it would have had to re-filter built-in vs MCP/Skills tools by name anyway, and it would not have reached sub-agents. Resolving inside the catalog gives 'built-in only' for free and covers ReadOnlyToolProvider too.

The short strings in `AgentSettingsComponent.CORE_AGENT_TOOLS` and `PsiToolCatalog` are UI labels, deliberately NOT the LLM-facing text. Rows stay single-line (multi-line HTML rows cause IDE-wide flicker on JBR), so the real description is surfaced as the row tooltip and in the editor dialog.

`effective()` swallows settings-access failures and falls back to the shipped default: ReadOnlyToolProviderTest runs without an IntelliJ Application, and tool registration must never fail because settings are unavailable.

An override is stored only when it differs from the default, so tools left alone keep tracking the shipped wording and pick up future plugin improvements.

Known scope edge: if an MCP server exposes a tool with a built-in name (e.g. read_file), CompositeToolProvider lets the MCP tool win, so the override does not apply to it. Consistent with the built-in-only scope.

Backlog (20 tools) and security-scan tools are excluded — their specs live in BacklogToolSpecifications / SecurityScanToolSpecification and they have no per-tool row in the Agent settings panel.

Manually verified by Stephan in a running IDE (2026-08-24): the settings rows, pencil editor and override behaviour work as intended. This supersedes the 'Swing panel not rendered' caveat recorded in the final summary at the time of writing.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## What changed

Built-in agent tools now expose their LLM-facing description as an editable setting, so users can steer the agent without a plugin change — e.g. uncheck `edit_file` and rewrite `run_command`'s description to say all edits must go through the shell.

### New
- **`BuiltInToolDescriptions`** — single source of truth for the descriptions of the 20 tools that `BuiltInToolProvider`/`ReadOnlyToolProvider` register inline (8 core, 9 PSI, `run_tests`, `parallel_explore`, `web_search`). `effective(name)` returns the user's override when set and non-blank, otherwise the shipped default. Unknown/renamed tool names and blank values are ignored; settings-access failures fall back to the default.
- **`ToolDescriptionEditorDialog`** — per-tool editor with a *Reset to Default* action. Reports "no override" when the text equals the default or is cleared, so no redundant copies are stored.

### Changed
- `BuiltInToolProvider` / `ReadOnlyToolProvider` build their specs from the catalog instead of inline string literals. A fresh provider is built per prompt, so edits apply from the next prompt without an IDE restart — and sub-agents see the same text as the main agent.
- `DevoxxGenieStateService.toolDescriptionOverrides` (`Map<String,String>`) persists the overrides.
- `AgentSettingsComponent`: every description-editable tool row gains a pencil button and a *custom description* marker; the row tooltip shows the description currently sent to the model. Edits are pending until Apply, and honour `isModified()`/`apply()`/`reset()`.

### Scope
Built-in tools only. MCP and Skills tools keep their provider's descriptions — guaranteed by construction, since resolution happens inside the built-in catalog rather than in a provider decorator. Backlog and security-scan tools are excluded (separate spec classes, no per-tool settings row). `ChatMemoryManager`'s hard-coded agent guidance was explicitly out of scope; note that it still tells the model to use `write_file`/`edit_file` after code changes, which can partially work against an override aimed at those tools.

### Tests
- New `BuiltInToolDescriptionsTest` (13 tests): defaults present and non-blank, override applied/trimmed, blank treated as absent, reset-to-default, unknown tool name ignored, null map and unavailable settings fall back to the default, PSI catalog fully covered.
- New tests in `BuiltInToolProviderTest`: override reaches the `ToolSpecification`, other tools stay on their default, unknown-name override ignored, backlog tools are not overridable, and a regression guard that every inline-registered tool has a catalog entry.
- Full `./gradlew test` suite green.

### Not verified
The Swing settings panel was not rendered — no sandbox IDE run. Coverage is at the catalog/provider level; the row layout and dialog have been compiled but not visually inspected.

### Docs
`docusaurus/docs/features/agent-mode.md` gains an *Editing a Tool's Description* section and a settings-table row.
<!-- SECTION:FINAL_SUMMARY:END -->
