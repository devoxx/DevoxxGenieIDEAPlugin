---
id: TASK-170
title: 'Fix java:S3776 in MCPToolsManager.java at line 124'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 12:29'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/mcp/MCPToolsManager.java`
- **Line:** 124
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 124 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/mcp/MCPToolsManager.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `MCPToolsManager.java:124` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `showMCPToolsPopup()` in `MCPToolsManager.java` to reduce cognitive complexity from 36 to ≤15.

**Files modified:**
- `src/main/java/com/devoxx/genie/ui/mcp/MCPToolsManager.java` — extracted 8 private helper methods from the monolithic `showMCPToolsPopup()` method
- `src/test/java/com/devoxx/genie/ui/mcp/MCPToolsManagerTest.java` — new test class with 11 tests covering tool state management logic

**Extracted methods:**
1. `buildMCPListPanel()` — builds the server list panel
2. `createMCPHeader()` — creates the header with title and buttons
3. `addServerEntry()` — adds one MCP server row (checkbox + tool count label + tools panel)
4. `createServerCheckbox()` — creates the per-server enable/disable checkbox
5. `onServerCheckboxToggled()` — handles server toggle action (≤4 complexity)
6. `buildToolsPanel()` — creates the per-tool checkbox panel for a server
7. `createToolCheckbox()` — creates a single tool checkbox
8. `onToolCheckboxToggled()` — handles tool toggle action (≤2 complexity)

Also added `HashSet` and `Set` imports to replace the inline fully-qualified type references.

**Tests:** 11 passing tests across 4 nested groups covering tool enable/disable state, tool count calculation, initial checkbox state, and server enable/disable state. All tests verified with `./gradlew test --tests com.devoxx.genie.ui.mcp.MCPToolsManagerTest`.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed SonarQube rule `java:S3776` (Cognitive Complexity) in `MCPToolsManager.java` at line 124. The `showMCPToolsPopup()` method had a complexity of 36 (limit: 15) due to deeply nested loops, conditionals, and lambda action listeners.\n\n## Approach\n\nExtracted the monolithic 145-line `showMCPToolsPopup()` into 8 focused private helper methods, each with low cognitive complexity:\n\n| Method | Complexity |\n|--------|------------|\n| `showMCPToolsPopup()` (entry point) | 2 |\n| `buildMCPListPanel()` | 0 |\n| `createMCPHeader()` | 0 |\n| `addServerEntry()` | 1 |\n| `createServerCheckbox()` | 1 |\n| `onServerCheckboxToggled()` | 4 |\n| `buildToolsPanel()` | 2 |\n| `createToolCheckbox()` | 2 |\n| `onToolCheckboxToggled()` | 2 |\n\nNo behavioral changes — pure structural refactoring. Also cleaned up fully-qualified `java.util.Set` and `java.util.HashSet` references by adding proper imports.\n\n## Tests\n\nCreated `MCPToolsManagerTest.java` with 11 passing tests covering:\n- Tool enable/disable toggle behavior (`onToolCheckboxToggled` logic)\n- Tool count calculation (`updateMCPToolsCounter` logic)\n- Initial checkbox selection state (`buildToolsPanel` logic)\n- Server enable/disable toggle behavior (`onServerCheckboxToggled` logic)\n\nAll tests verified: `BUILD SUCCESSFUL` with 11/11 tests passing.
<!-- SECTION:FINAL_SUMMARY:END -->
