---
id: TASK-277.5
title: Add Parallel as an optional web search provider
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-18T08:11:00Z'
updated_date: '2026-11-18T09:57:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1423
  - https://docs.parallel.ai/integrations/mcp/quickstart
modified_files:
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchHttp.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/KeenableWebSearchEngine.java
  - >-
    src/test/java/com/devoxx/genie/service/websearch/ParallelWebSearchEngineTest.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/ParallelWebSearchEngine.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchProvider.java
  - src/main/java/com/devoxx/genie/service/credentials/CredentialKey.java
  - src/main/java/com/devoxx/genie/service/analytics/FeatureId.java
  - src/main/java/com/devoxx/genie/service/analytics/AnalyticsEventBuilder.java
  - >-
    src/test/java/com/devoxx/genie/service/analytics/FeatureUsageTrackerTest.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchProviderRegistry.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchService.java
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurableTest.java
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchLiveSmokeTest.java
  - src/test/java/com/devoxx/genie/service/websearch/ParallelStubServer.java
  - src/test/java/com/devoxx/genie/service/websearch/ParallelE2ETest.java
  - docusaurus/docs/features/web-search.md
  - docusaurus/docs/configuration/settings.md
  - docusaurus/docs/features/overview.md
  - docusaurus/static/api/tips.json
  - src/main/java/com/devoxx/genie/service/tips/TipService.java
parent_task_id: TASK-277
priority: low
type: feature
ordinal: 30000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Requested in #1423 by a Parallel contributor. Parallel can already be used in agent conversations as a generic HTTP MCP server (https://mcp.parallel.ai/mcp?tools=web_search, keyless), but not for /search, the Web toggle or Test search.

Agreed direction: implement it like any other registry provider (a search engine in this repo), not by routing /search through the MCP client. Before implementation, the contributor should answer on #1423: why MCP vs Parallel's REST search API, keyless limits/terms, and result-format stability. Off by default; no Parallel requests unless selected.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Integration route (REST API or MCP) and keyless terms agreed on #1423 and recorded in task notes
- [x] #2 Parallel can be selected in Settings -> Web search and works for Test search, /search, the Web toggle and the agent web_search tool
- [x] #3 No request is sent to Parallel unless it is the selected provider
- [x] #4 Rate-limit and authentication failures show the user a clear, specific message
- [x] #5 Unit tests cover request, response mapping, empty results and errors; wiring test passes
- [x] #6 Web Search documentation describes Parallel
- [x] #7 End-to-end tests run the Parallel engine through the central service against a local stub server, including empty, rate-limited and error responses
- [x] #8 docs/features/web-search.md moves Parallel from 'More providers coming soon' into 'Choosing a Provider' with setup steps
- [x] #9 A Parallel tip is added to docusaurus/static/api/tips.json and the TipService fallback list, published only once a release containing Parallel is out (tips.json is fetched live by all installed versions)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Branch feature/task-277.5-parallel-provider, stacked on feature/task-277.4-deprecate-google-search (#1434 -> #1433 -> #1432 -> #1431).

1. Extract the shared HTTP bits used by provider engines (IDE-proxy HttpClient, User-Agent constant) into WebSearchHttp; Keenable uses it too.
2. ParallelWebSearchEngine (in-repo WebSearchEngine): POST {base}/v1/search with x-api-key, User-Agent DevoxxGenie, JSON (Gson) {search_queries:[query], objective: query, mode: "fast", max_chars_total: 6000, advanced_settings:{max_results:n}}; no redirect following (3xx = failure, so the key never leaves the origin); maps results[] url/title/excerpts -> WebSearchOrganicResult (title falls back to host, excerpts joined as snippet/content, invalid URLs skipped); WebSearchProviderException with user-safe messages for 401/403 (key rejected), 402 (out of credit), 429 (rate limited, retry in a minute), 422, 5xx, unreadable body, I/O.
3. Registry: WebSearchProvider.PARALLEL (before GOOGLE, which stays last as deprecated), CredentialKey.PARALLEL_SEARCH_KEY (required), FeatureId.WEB_SEARCH_PARALLEL + analytics allowlist; test hook useParallelBaseUrlForTesting.
4. Tests: engine unit tests (request shape/headers, mapping, title fallback, empty, each error code, unreadable, unreachable, no redirect follow), ParallelStubServer + E2E for agent tool, /search (real strategy) and Test search, never contacted unless selected, missing key -> readiness message; settings rows; env-gated live smoke (PARALLEL_API_KEY).
5. Docs: move Parallel from "coming soon" into Choosing a Provider with setup + cost note (fast mode); remove the coming-soon section if empty. Tips: TipService fallback + tips.json worded "Update DevoxxGenie..." (same pattern the maintainer chose for Keenable).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-18 maintainer decision: integrate Parallel via its REST Search API, not MCP.
REST contract (docs.parallel.ai, fetched 2026-11-18): POST https://api.parallel.ai/v1/search, header x-api-key (ApiKeyAuth applied globally; no documented keyless REST access or free tier — keyless only exists for the MCP server, which we don't use). Request: search_queries [string] (required), objective, mode (turbo|fast|basic|advanced; default advanced), max_chars_total, advanced_settings.max_results (default 10). Response: search_id, results[] {url (required), title (nullable), publish_date, excerpts [markdown strings]}, warnings, usage. Errors (warnings-and-errors page): 401 invalid/missing key, 402 insufficient credit, 403 forbidden, 422 validation, 429 rate limited (rolling 1-minute window), 500/502/503 retryable, 408 timeout. Pricing: fast/turbo $1 per 1,000 requests, basic/advanced $5 per 1,000 (10 results included).
So for AC #1: route = REST (maintainer decision); keyless terms = not applicable (REST requires a key); result format = documented V1SearchResponse above.

2026-11-18 implementation: ParallelWebSearchEngine (REST, POST /v1/search, x-api-key, User-Agent DevoxxGenie, IDE proxy via shared WebSearchHttp, Gson body {search_queries:[q], objective:q, mode:'fast', max_chars_total:6000, advanced_settings.max_results}); redirects never followed (3xx = failure, key stays on origin); results url/title/excerpts mapped (title -> host fallback, excerpts joined, invalid/hostless URLs skipped, limit enforced); user-safe WebSearchProviderException for 401/403, 402 (out of credit), 429 (retry in a minute), 422, 5xx, unreadable body, I/O. Registry: PARALLEL (before deprecated GOOGLE), CredentialKey.PARALLEL_SEARCH_KEY (required; migration fix from 277.3 handles the missing legacy field), FeatureId web_search_parallel (+ allowlist; GenieBuilder receiver must accept it).
Tests: ParallelWebSearchEngineTest 15, ParallelE2ETest 11 (agent tool, /search via real strategy, Test search; never contacted unless selected; missing key -> readiness message, nothing sent), registry wiring covers PARALLEL, settings rows/readiness, env-gated live smoke (PARALLEL_API_KEY). Full suite 4377 passed, 26 skipped, 0 failed. Docs build OK.
Docs: Parallel in the provider table + 'Parallel Setup' (fast mode, billed to the user's account, error messages); the empty 'More providers coming soon' section became 'Search services with an MCP server'. Tips: TipService fallback + tips.json (worded 'Update DevoxxGenie and pick Parallel...', same pattern as Keenable).
AC #1 left open: the REST decision is the maintainer's and recorded here, but it has not been communicated/agreed on #1423 yet (comment drafted for maintainer approval).

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1435 (stacked on #1434). Remaining: AC #1 (communicate the REST decision on #1423; comment drafted for maintainer approval).
<!-- SECTION:NOTES:END -->
