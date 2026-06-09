# Fine-grained PSI tool control — Design

**Date:** 2026-06-09
**Task:** task-229 (Extend PSI agent tools with call-graph navigation)
**Status:** Approved

## Problem

The Agent settings expose a single master checkbox, **"Enable PSI Tools"**, that turns
all PSI (Program Structure Interface) code-intelligence tools on or off as a group. Its
label is also stale: it enumerates only the original five tools
(`find_symbols`, `document_symbols`, `find_references`, `find_definition`,
`find_implementations`) and omits the four added under task-229
(`find_callees`, `trace_call_chains`, `calculate_complexity`, `find_dead_code`).

Developers want **fine-grained control** — the ability to enable/disable each PSI tool
individually — and the new tools must be listed.

## Key insight

The enforcement plumbing already exists. `BuiltInToolProvider.provideTools()` filters
**every** registered tool (PSI tools included, since they share the same `tools` map)
against the `disabledAgentTools` list:

```java
for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
    if (!disabledSet.contains(entry.getKey().name())) {
        builder.add(entry.getKey(), entry.getValue());
    }
}
```

The "Built-in Tools" settings section already renders per-tool checkboxes wired to that
same `disabledAgentTools` list via the `toolCheckboxes` map. `apply()`, `isModified()`,
and `reset()` all derive `disabledAgentTools` by iterating `toolCheckboxes`.

Therefore this is a **UI-only** change. No change to `BuiltInToolProvider`, and no new
state field.

## Scope

- **In scope:** `AgentSettingsComponent` PSI section UI.
- **Out of scope:** `BuiltInToolProvider`, `DevoxxGenieStateService` schema, any change to
  the PSI tool executors themselves.

## Behavior

- The master **"Enable PSI Tools"** checkbox keeps its current meaning: when off,
  `registerPsiTools()` is never called, so no PSI tool is registered (group gate).
  Its label drops the stale parenthetical tool list.
- Below it, **9 indented per-tool checkboxes** — one per PSI tool — each with a short
  description:
  - `find_symbols` — search definitions
  - `document_symbols` — list file structure
  - `find_references` — find usages
  - `find_definition` — go to definition
  - `find_implementations` — find interface/class implementations
  - `find_callees` — outgoing calls from a method
  - `trace_call_chains` — caller/callee call paths
  - `calculate_complexity` — cyclomatic complexity
  - `find_dead_code` — unreferenced-symbol candidates
- Unchecking a sub-tool adds its name to `disabledAgentTools` →
  `provideTools()` filters it out on the next agent run.
- When the master is **off**, the 9 sub-checkboxes grey out (`setEnabled(false)`) but keep
  their checked-state, so per-tool preferences survive toggling the group off and on.

## Implementation approach

1. Add a `PSI_TOOLS` `String[][]` constant (tool name + short description) mirroring the
   existing `CORE_AGENT_TOOLS`.
2. In the PSI settings section, after the master checkbox, loop `PSI_TOOLS` creating an
   **indented** `JBCheckBox` for each, initialized from `!disabledSet.contains(name)`, and
   register it in the **same `toolCheckboxes` map** the Built-in Tools section uses.
3. Add an `ItemListener` on the master checkbox to enable/disable the 9 sub-checkboxes;
   set their initial enabled-state from the master's loaded value.
4. `apply()` / `isModified()` / `reset()` require **no changes** — they already iterate
   `toolCheckboxes`. The master `psiToolsEnabled` modified-check also already exists.
5. Add a small indented-row helper (or reuse `addFullWidthRow` with a left inset).

## Testing

- State round-trip test: an unchecked PSI sub-tool ends up in `disabledAgentTools` after
  `apply()`, and a re-check removes it; `isModified()` flips correctly.
- `provideTools()` filtering of disabled PSI tools is already exercised by the existing
  disabled-tool filter; add an assertion that a disabled PSI tool name is excluded from
  the provided tools.
- UI layout (indentation, grey-out) is verified manually in `runIde`; not unit-tested.

## Risks / notes

- The `disabledAgentTools` list is shared between the Built-in Tools and PSI sections.
  Registering PSI checkboxes into the same `toolCheckboxes` map is what makes the shared
  `apply()`/`isModified()`/`reset()` correct; do **not** introduce a separate map (it would
  cause `apply()` to clobber one section's entries).
- No migration needed: existing installs have `psiToolsEnabled=true` and an empty/unrelated
  `disabledAgentTools`, so all PSI sub-tools render as checked by default.
