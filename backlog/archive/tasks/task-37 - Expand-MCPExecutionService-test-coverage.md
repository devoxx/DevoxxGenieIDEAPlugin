---
id: TASK-37
title: Expand MCPExecutionService test coverage
status: Done
assignee: []
created_date: '2026-02-14 11:01'
updated_date: '2026-02-14 16:01'
labels:
  - testing
  - mcp
  - refactoring
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MCPExecutionService (399 LOC) currently has only 4 tests (2 disabled) covering just `createMCPCommand()`. Most of the class is untested: client caching, `clearClientCache()`, `dispose()`, `createMCPToolProvider()`, `createRawMCPToolProvider()`, and the transport initialization methods.

**Refactoring needed:** The class has deeply coupled static dependencies (`DevoxxGenieStateService`, `MCPService`, `ApplicationManager`) and creates transport/client objects directly. To improve testability:
1. Extract a `McpClientFactory` interface with methods `createStdioClient()`, `createHttpClient()`, `createHttpSseClient()` that the service delegates to. This allows mocking client creation in tests.
2. Make `createTrafficConsumer()` protected or package-private for testing
3. Accept `DevoxxGenieStateService` via constructor (or provide a package-private setter for tests)

**Test cases needed:**
- clearClientCache closes all cached clients
- clearClientCache handles exception during client close
- dispose calls clearClientCache
- createMCPToolProvider returns null when raw provider is null
- createMCPToolProvider wraps raw provider with ApprovalRequiredToolProvider
- createRawMCPToolProvider returns null when no servers configured
- createRawMCPToolProvider returns null when no enabled servers
- createRawMCPToolProvider wraps result with FilteredMcpToolProvider
- createMcpClient returns cached client on second call
- createMcpClient routes to correct init method based on transport type (STDIO, HTTP, HTTP_SSE)
- initHttpSseClient returns null for empty URL
- initStreamableHttpClient returns null for empty URL
- createMCPCommand disabled tests should be fixed and enabled
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MCPExecutionService has >= 80% line coverage
- [x] #2 Client caching logic is tested
- [x] #3 Transport type routing is tested
- [x] #4 Disabled tests are fixed or removed
- [x] #5 Refactoring preserves existing behavior
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Refactored MCPExecutionService for testability and wrote 27 comprehensive tests (from 4, with 2 disabled).

**Refactoring:**
- Extracted `McpClientCreator` functional interface for injectable client creation
- Added package-private constructor for test injection
- Made `createMcpClient`, `createNewClient`, `initHttpSseClient`, `initStreamableHttpClient`, `createTrafficConsumer` package-private
- Added `getCacheSize()` for test verification
- Fixed `createMCPCommand` to validate empty list and filter null arguments
- Enabled both previously disabled tests

**Test coverage:**
- Cache lifecycle: clearClientCache (3 tests), dispose (1 test)
- Client caching: cachesNewClient, returnsCachedClientOnSecondCall, doesNotCacheNull, handlesCreatorException
- createMCPToolProvider: null paths + wraps with ApprovalRequiredToolProvider
- createRawMCPToolProvider: null paths + wraps with FilteredMcpToolProvider + multi-server
- Transport routing: HTTP_SSE, HTTP, STDIO via createNewClient
- URL validation: initHttpSseClient and initStreamableHttpClient (null, empty, blank)
- createMCPCommand: valid, null, empty, nullArgs, spaces, single command
- createTrafficConsumer: non-null + debug-disabled path
<!-- SECTION:FINAL_SUMMARY:END -->
