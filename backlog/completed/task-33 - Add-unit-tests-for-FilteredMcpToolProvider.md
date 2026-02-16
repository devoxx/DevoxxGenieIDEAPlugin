---
id: TASK-33
title: Add unit tests for FilteredMcpToolProvider
status: Done
assignee: []
created_date: '2026-02-14 11:00'
updated_date: '2026-02-14 11:03'
labels:
  - testing
  - mcp
  - refactoring
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
FilteredMcpToolProvider (72 LOC) has ZERO tests. It's a decorator that filters disabled tools from an MCP ToolProvider. The class has good testability since it's a decorator pattern, but `collectDisabledTools()` is a static method calling `DevoxxGenieStateService.getInstance()` which needs mocking.

**Refactoring needed:** Extract `collectDisabledTools()` from static to instance method, or accept a `Supplier<Set<String>>` in the constructor to make it injectable for testing.

**Test cases needed:**
- provideTools with no disabled tools returns all tools from delegate
- provideTools filters out disabled tools by name
- provideTools with all tools disabled returns empty result
- provideTools with mixed disabled/enabled tools across multiple servers
- collectDisabledTools skips disabled servers
- collectDisabledTools handles null disabledTools list
- collectDisabledTools handles empty servers map
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 FilteredMcpToolProvider has >= 80% line coverage
- [x] #2 collectDisabledTools logic is testable without static mocking
- [x] #3 All filtering edge cases are covered
- [x] #4 Existing behavior is preserved after refactoring
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
**Refactored** `FilteredMcpToolProvider` to accept an injectable `Supplier<Set<String>>` for disabled tools, keeping the public constructor backward-compatible with the settings-based default. Extracted `collectDisabledTools(Map)` as a pure static method testable without platform dependencies.

**Added** `FilteredMcpToolProviderTest` with 13 tests covering:
- provideTools: no disabled, some disabled, all disabled, empty delegate, non-matching disabled, executor preservation
- collectDisabledTools: empty map, enabled server, disabled server skipped, null disabledTools, multiple servers merge, empty set, deduplication across servers

All 53 MCP package tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
