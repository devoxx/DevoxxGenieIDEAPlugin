---
id: TASK-36
title: Add unit tests for MCPService static utility
status: Done
assignee: []
created_date: '2026-02-14 11:01'
updated_date: '2026-02-14 15:33'
labels:
  - testing
  - mcp
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MCPService (69 LOC) has ZERO tests. It's a static utility class with methods: `isMCPEnabled()`, `isDebugLogsEnabled()`, `refreshToolWindowVisibility()`, `resetNotificationFlag()`, `logDebug()`.

**Refactoring option:** Since this is a pure utility class with static methods, use `mockStatic` for `DevoxxGenieStateService` and `ApplicationManager`. Alternatively, consider converting to an application service with instance methods, but that would be a larger change.

**Test cases needed:**
- isMCPEnabled returns true when settings say enabled
- isMCPEnabled returns false when settings say disabled
- isDebugLogsEnabled returns true only when BOTH MCP enabled AND debug logs enabled
- isDebugLogsEnabled returns false when MCP disabled (even if debug flag true)
- isDebugLogsEnabled returns false when debug flag is false
- refreshToolWindowVisibility publishes settings changed event
- resetNotificationFlag resets the static flag
- logDebug with debug enabled logs at info level
- logDebug with debug disabled logs at debug level
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MCPService has >= 80% line coverage
- [x] #2 All static methods are tested
- [x] #3 isDebugLogsEnabled compound condition is fully tested
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
**Added** `MCPServiceTest` with 10 tests using `mockStatic` for `DevoxxGenieStateService` and `ApplicationManager` (consistent with existing test patterns in the package).

Tests cover:
- `isMCPEnabled`: true/false based on settings
- `isDebugLogsEnabled`: all 4 combinations of MCP enabled/disabled x debug enabled/disabled (compound AND condition fully tested)
- `refreshToolWindowVisibility`: verifies settings changed event is published
- `resetNotificationFlag`: verifies no-throw
- `logDebug`: both debug-enabled (info level) and debug-disabled (debug level) paths

No refactoring needed — used `mockStatic` approach matching existing package conventions.

Also fixed pre-existing compile error in `AgentRequestHandler.dispatch()` — replaced specific exception types with `AcpException` base class (all three exceptions extend `AcpException`).

All 84 MCP package tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
