---
id: TASK-166
title: 'Fix java:S3776 in MCPConfigurationParser.java at line 195'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 12:08'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/mcp/MCPConfigurationParser.java`
- **Line:** 195
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 34 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 195 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/mcp/MCPConfigurationParser.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 34 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `MCPConfigurationParser.java:195` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `exportToJson` (line 195) in `MCPConfigurationParser.java` to reduce cognitive complexity from 34 to well under 15.

Extracted four private helper methods from the monolithic loop body:
- `buildServerConfig(MCPServer, boolean)` - builds the JsonObject for one server (complexity ~6)
- `getTransportString(TransportType)` - maps transport enum to string (switch, complexity 1)
- `addTransportSpecificFields(JsonObject, MCPServer, boolean)` - handles HTTP vs STDIO fields (complexity ~14)
- `addDisabledToolsArray(JsonObject, MCPServer)` - adds disabledTools array (complexity ~5)

`exportToJson` itself now has complexity of 1 (single for-loop). All 13 existing tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed java:S3776 (cognitive complexity too high) in `MCPConfigurationParser.java:195`.\n\n## Problem\nThe `exportToJson` method had a cognitive complexity of 34 (limit: 15) due to deeply nested conditionals and loops all inside a single method body.\n\n## Solution\nRefactored by extracting the per-server build logic into focused private helper methods:\n\n1. **`buildServerConfig(MCPServer, boolean)`** — builds the complete `JsonObject` for one server; delegates to helpers for transport fields and disabled tools\n2. **`getTransportString(TransportType)`** — maps transport enum to the JSON string identifier\n3. **`addTransportSpecificFields(JsonObject, MCPServer, boolean)`** — handles HTTP (url, headers) vs STDIO (command, args) fields\n4. **`addDisabledToolsArray(JsonObject, MCPServer)`** — adds the `disabledTools` JSON array when present\n\nResulting complexities:\n- `exportToJson`: 1 (single for-loop)\n- `buildServerConfig`: ~6\n- `addTransportSpecificFields`: ~14 (just under the 15 limit)\n- All other helpers: ≤5\n\n## Verification\nAll 13 existing `MCPConfigurationParserTest` tests pass. No new tests were needed as the existing suite fully covers `exportToJson` and all extracted helper methods. No new SonarQube issues were introduced.">
<!-- SECTION:FINAL_SUMMARY:END -->
