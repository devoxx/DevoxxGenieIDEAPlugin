---
id: TASK-38
title: Expand MCPRegistryService test coverage for HTTP and caching
status: Done
assignee: []
created_date: '2026-02-14 11:01'
updated_date: '2026-02-14 16:34'
labels:
  - testing
  - mcp
  - refactoring
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MCPRegistryService (206 LOC) has 12 tests but only covers `convertToMCPServer()` and `getServerType()`. The HTTP methods (`fetchAllServers`, `searchServers`) and caching logic are completely untested.

**Refactoring needed:** The `OkHttpClient` is created directly from `HttpClientProvider.getClient()`. Make it injectable via constructor to allow mocking HTTP responses in tests.

**Test cases needed:**
- fetchAllServers returns cached results when not forcing refresh
- fetchAllServers fetches fresh data when forceRefresh=true
- fetchAllServers handles pagination (multiple pages with cursors)
- fetchAllServers handles empty response
- searchServers builds URL with query parameter
- searchServers builds URL with cursor parameter
- searchServers throws IOException on non-successful response
- searchServers throws IOException on null response body
- searchServers handles null result from JSON parsing
- convertToMCPServer fallback when no remotes and no packages
- convertPackageServer with unknown registry type uses identifier as command
- getServerType returns "Unknown" when no remotes or packages
- getServerType returns raw registryType for non-npm/non-oci packages
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MCPRegistryService has >= 80% line coverage
- [x] #2 HTTP methods are testable via injectable OkHttpClient
- [x] #3 Caching behavior is verified
- [x] #4 Pagination logic is tested
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Expanded MCPRegistryService test coverage from 12 to 23 tests. Refactored the service to accept injectable OkHttpClient and Gson via a package-private constructor. Created a TestMCPRegistryService subclass to redirect HTTP requests to MockWebServer.\n\nNew test coverage:\n- **ConvertToMCPServer** (11 tests): remote servers, headers, env vars, npm/oci/unknown packages, fallback\n- **GetServerType** (5 tests): Remote, npm, Docker, unknown, no-packages-or-remotes\n- **SearchServers** (5 tests): query/cursor URL parameters, error handling, JSON parsing, null handling\n- **FetchAllServers** (4 tests): caching, force refresh, pagination with cursors, empty responses\n\nAll 23 tests pass. Full test suite verified green.
<!-- SECTION:FINAL_SUMMARY:END -->
