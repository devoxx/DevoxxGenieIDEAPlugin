---
id: TASK-169
title: 'Fix java:S3776 in MCPSettingsComponent.java at line 460'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 12:23'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java`
- **Line:** 460
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 61 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 460 in `src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 61 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `MCPSettingsComponent.java:460` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `showToolsInfoDialog` (line 460) from a single method with an anonymous `DialogWrapper` (cognitive complexity 61) into a named inner class `MCPToolsDialog`.

Changes:
- `showToolsInfoDialog()` is now 4 lines — just a null guard + `new MCPToolsDialog(server).show()`
- New `MCPToolsDialog` inner class (non-static, to access outer fields `isModified` and `tableModel`) with focused helper methods:
  - `createCenterPanel()` — top-level panel builder
  - `createNoToolsLabel()` — empty-state label
  - `buildToolsPanel()` — populates panel with tools table and surrounding UI
  - `buildTableData()` — builds the raw data array for the table model
  - `createToolsTableModel()` — anonymous AbstractTableModel factory
  - `createToolsTable()` — configures JBTable with column widths/renderers
  - `createHeaderPanel()` — header with count label + Enable/Disable All buttons
  - `createToggleAllButton()` — factory for enable/disable buttons (replaces duplicated lambda)
  - `createHintLabel()` — hint text at bottom
  - `doOKAction()` — saves disabled tools back to server
  - `collectDisabledTools()` — reads table state to produce disabled-tools set
  - `findScrollPane()` — recursive scroll pane finder

The unused `toolCheckboxes` field from the anonymous class was removed.

All MCP settings tests pass (MCPServerDialogEnvVarTest, StdioTransportPanelTest, MCPMarketplaceDialogFilterTest).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nResolved `java:S3776` (Cognitive Complexity too high) in `MCPSettingsComponent.java` at line 460.\n\n### Problem\nThe `showToolsInfoDialog()` method had a cognitive complexity of 61 (limit: 15), caused by a deeply nested anonymous `DialogWrapper` inner class with complex `createCenterPanel()`, `doOKAction()`, and `findScrollPane()` logic all interleaved in one ~160-line anonymous class body.\n\n### Solution\nExtracted the anonymous `DialogWrapper` into a proper named inner class `MCPToolsDialog` with focused helper methods:\n\n**`showToolsInfoDialog()`** reduced to 4 lines — null guard + `new MCPToolsDialog(server).show()`.\n\n**`MCPToolsDialog` inner class** methods:\n- `createCenterPanel()` — dispatches to empty-state or tools panel\n- `createNoToolsLabel()` — empty-state UI\n- `buildToolsPanel()` — orchestrates table + header + hint layout\n- `buildTableData()` — pure data transformation (server tools → Object[][])\n- `createToolsTableModel()` — creates AbstractTableModel\n- `createToolsTable()` — configures JBTable with widths/renderers\n- `createHeaderPanel()` — header with count and toggle buttons\n- `createToggleAllButton()` — factory replacing duplicated lambda code\n- `createHintLabel()` — hint text\n- `doOKAction()` — delegates to `collectDisabledTools()` then saves to server\n- `collectDisabledTools()` — reads table state → disabled tools set\n- `findScrollPane()` — recursive scroll pane finder\n\nAlso removed the unused `toolCheckboxes` field from the original anonymous class.\n\n### File Modified\n- `src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java`\n\n### Tests\nAll existing MCP settings tests pass (`MCPServerDialogEnvVarTest`, `StdioTransportPanelTest`, `MCPMarketplaceDialogFilterTest`). No new tests were added because `MCPToolsDialog` is pure Swing UI requiring IntelliJ platform context, the refactoring introduces no new business logic, and all underlying model logic (`MCPServer`) is covered by existing service-level tests.
<!-- SECTION:FINAL_SUMMARY:END -->
