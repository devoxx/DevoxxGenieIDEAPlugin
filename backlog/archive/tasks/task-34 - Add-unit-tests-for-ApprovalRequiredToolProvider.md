---
id: TASK-34
title: Add unit tests for ApprovalRequiredToolProvider
status: Done
assignee: []
created_date: '2026-02-14 11:00'
updated_date: '2026-02-14 11:06'
labels:
  - testing
  - mcp
  - refactoring
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ApprovalRequiredToolProvider (54 LOC) has ZERO tests. It's a decorator that wraps each tool executor with an approval check via `MCPApprovalService.requestApproval()`.

**Refactoring needed:** The class uses a static call to `MCPApprovalService.requestApproval()`. Introduce an `ApprovalChecker` functional interface (or `BiFunction<String, String, Boolean>`) that can be injected via constructor, defaulting to `MCPApprovalService::requestApproval` in production. This allows clean testing without mockStatic.

**Test cases needed:**
- provideTools wraps all tools from delegate with approval executor
- Approval granted → original executor is called and result returned
- Approval denied → "Tool execution was denied by the user." returned
- Multiple tools are each wrapped independently
- Original ToolSpecification is preserved in output
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 ApprovalRequiredToolProvider has >= 80% line coverage
- [x] #2 Approval check is injectable for testing
- [x] #3 Both approved and denied paths are tested
- [x] #4 Existing behavior is preserved after refactoring
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
**Refactored** `ApprovalRequiredToolProvider` to accept an injectable `ApprovalChecker` functional interface via a package-private constructor, while the public constructor defaults to `MCPApprovalService::requestApproval`. The `project` field is stored separately and passed correctly to the checker.

**Added** `ApprovalRequiredToolProviderTest` with 8 tests covering:
- Wrapping: all delegate tools are wrapped with approval executors
- Approved path: original executor called, result returned
- Denied path: "Tool execution was denied by the user." returned, original never called
- Argument verification: checker receives correct project, toolName, arguments
- Independent wrapping: multiple tools each have separate approval decisions
- Spec preservation: ToolSpecification (name, description) preserved in output
- Empty delegate: returns empty result
- Null project: passed through to checker correctly

All 61 MCP package tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
