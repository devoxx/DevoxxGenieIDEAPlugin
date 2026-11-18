---
id: TASK-277.3
title: Add Keenable as an optional web search provider
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T09:35:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1428
  - https://docs.keenable.ai/api-reference/search
modified_files:
  - >-
    src/main/java/com/devoxx/genie/service/websearch/KeenableWebSearchEngine.java
  - >-
    src/test/java/com/devoxx/genie/service/websearch/KeenableWebSearchEngineTest.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchProviderException.java
  - src/main/java/com/devoxx/genie/service/credentials/CredentialKey.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchProvider.java
  - src/main/java/com/devoxx/genie/service/analytics/FeatureId.java
  - src/main/java/com/devoxx/genie/service/analytics/AnalyticsEventBuilder.java
  - >-
    src/test/java/com/devoxx/genie/service/analytics/FeatureUsageTrackerTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceCredentialMigrationTest.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchProviderRegistry.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchService.java
  - src/test/java/com/devoxx/genie/service/websearch/KeenableStubServer.java
  - src/test/java/com/devoxx/genie/service/websearch/KeenableE2ETest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/strategy/WebSearchStrategyAccess.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchTestSearchAccess.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/WebSearchPromptStrategy.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersComponent.java
  - src/main/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutor.java
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchLiveSmokeTest.java
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurableTest.java
  - docusaurus/docs/features/web-search.md
  - src/main/java/com/devoxx/genie/service/tips/TipService.java
  - docusaurus/docs/configuration/settings.md
  - docusaurus/docs/features/overview.md
parent_task_id: TASK-277
priority: medium
type: feature
ordinal: 28000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Requested in #1428 by a Keenable contributor, who offered to open the PR (maintainer welcomed it on 2026-11-17). Keenable works without an API key (rate-limited per IP; public endpoint POST https://api.keenable.ai/v1/search/public with header X-Keenable-Title: DevoxxGenie) and optionally with a key (POST /v1/search with X-API-Key). It gives users a no-signup option, which matters because Google's search API is being discontinued.

Constraints agreed with the maintainer: build on the provider registry (depends on the registry subtask), keep the engine in this repo rather than adding a langchain4j-community dependency, off by default, and no Keenable requests unless it is the selected provider.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Keenable can be selected in Settings -> Web search and works for Test search, /search, the Web toggle and the agent web_search tool
- [x] #2 Works with no key via the public endpoint; with a key, uses the authenticated endpoint
- [x] #3 No request is sent to Keenable unless it is the selected provider
- [x] #4 Rate-limit and authentication failures show the user a clear, specific message
- [x] #5 Requests identify DevoxxGenie (title header / User-Agent)
- [x] #6 Unit tests cover keyless and keyed requests, response mapping, empty results and error responses; wiring test passes
- [x] #7 Web Search documentation describes Keenable, keyless limits, and how to add a key
- [x] #8 End-to-end tests run the Keenable engine through the central service against a local stub server: keyless and keyed endpoints, headers, rate-limit (429) and auth (401/403) responses
- [x] #9 Optional live keyless smoke test, skipped unless explicitly enabled by an environment variable
- [x] #10 docs/features/web-search.md moves Keenable from 'More providers coming soon' into 'Choosing a Provider' with setup steps
- [ ] #11 A Keenable tip is added to docusaurus/static/api/tips.json and the TipService fallback list, published only once a release containing Keenable is out (tips.json is fetched live by all installed versions)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Context (2026-11-18): the Keenable contributor opened PR #1430 (KeenableWebSearchEngine + tests, wired via its own WebSearchEngineFactory and the old checkboxes, so it conflicts with #1431). Maintainer decision: port it onto the registry with the contributor credited, then close #1430 with thanks once ours merges; draft the #1430 comment for maintainer approval before posting.

Branch: feature/task-277.3-keenable-provider, stacked on feature/task-277.2-search-source-links (PR #1432 -> #1431).

Steps:
1. Commit the contributor's KeenableWebSearchEngine + KeenableWebSearchEngineTest unchanged with them as author (done: c3be4996).
2. Move the engine to com.devoxx.genie.service.websearch (next to the registry) and harden it:
   - drop the API key on any ORIGIN change (scheme/host/port), not only host; their cross-host test uses two 127.0.0.1 servers, so host-only comparison forwards the key;
   - build the JSON body with Gson (hand escaping misses control characters);
   - User-Agent "DevoxxGenie" in addition to X-Keenable-Title;
   - use the JVM/IDE ProxySelector (java.net.http.HttpClient defaults to no proxy);
   - clear user-facing errors: 429 (keyless limit per IP; add a key), 401/403 (key rejected), other HTTP and I/O failures, via a WebSearchProviderException whose message is safe to show.
3. Registry: WebSearchProvider.KEENABLE, CredentialKey.KEENABLE_SEARCH_KEY, FeatureId.WEB_SEARCH_KEENABLE + analytics allowlist (+ schema doc if one exists), definition with an optional (required=false) API key field, so Keenable is ready when selected.
4. Surface WebSearchProviderException messages verbatim in WebSearchPromptStrategy (/search) and the settings Test search (other exceptions keep the class-name-only message there to avoid leaking secrets).
5. Tests: engine unit tests (contributor's, fixed if needed, + errors, escaping, headers, proxy-free), E2E through WebSearchService with MockWebServer: agent tool, /search strategy and Test search, keyless + keyed + 429/401/403; nothing sent to Keenable unless selected; env-gated live keyless smoke test (KEENABLE_LIVE_SMOKE=true).
6. Docs: move Keenable from 'coming soon' to 'Choosing a Provider' with setup + keyless limits. Tip in the TipService fallback only; tips.json tip waits for the release (it is fetched live by all installed versions).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-18 implementation:
- Contributor's engine + 3 tests committed unchanged as c3be4996 (author Karl-Gustav Kallasmaa). Running them showed doesNotForwardApiKeyOnCrossHostRedirect FAILED: sameHost() compared host only and both stub servers are 127.0.0.1 (ports differ), so the key WAS forwarded. Now sameOrigin() (scheme+host+effective port).
- Hardening: Gson request body (hand escaping missed control chars), User-Agent DevoxxGenie, JVM/IDE ProxySelector (java.net.http defaults to no proxy), WebSearchProviderException(kind RATE_LIMITED/UNAUTHORIZED/FAILED) with user-safe messages for 429 (keyless vs keyed wording), 401/403 (key rejected / keyless refused), other HTTP, unreadable JSON, I/O, redirect loop; host as title fallback.
- Found and fixed a latent bug: migrateCredentialsToPasswordSafe treats a CredentialKey without a legacy XML field as a FAILED migration (NoSuchFieldException), so adding KEENABLE_SEARCH_KEY would have left credentialsMigratedV1=false forever for not-yet-migrated users (retry + warn every startup, plaintext never wiped). Keys without a legacy field are now skipped; DevoxxGenieStateServiceCredentialMigrationTest reproduced it first (2 failing) — there were no migration tests before.
- Registry: KEENABLE definition with an optional key (required=false) -> ready when selected; FeatureId WEB_SEARCH_KEENABLE + analytics allowlist (no docs/analytics-schema.md exists despite the test comment; the GenieBuilder receiver may need to accept 'web_search_keenable').
- WebSearchProviderException messages are shown verbatim by /search (WebSearchPromptStrategy), the agent tool and Settings Test search; other exceptions keep the class-name-only message in Test search.
- Tests: KeenableWebSearchEngineTest 15, KeenableE2ETest 10 (agent tool: keyless/keyed endpoints, headers, no key on public endpoint, 429/401 messages, never contacted unless selected; /search: Sources links, 429 message verbatim; Test search: results, 403 message), registry wiring covers KEENABLE, readiness/settings tests, env-gated live smoke (KEENABLE_LIVE_SMOKE=true). Full suite 4331 passed, 25 skipped, 0 failed. Docs build OK.
- AC #11 intentionally open: TipService fallback tip added (ships with the release); the tips.json tip must be added only after a release containing Keenable is out, because tips.json is fetched live by all installed versions.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1433 (stacked on #1432 -> #1431). Remaining: AC #11 (tips.json tip after the release); close #1430 with thanks once #1433 merges (comment drafted for maintainer approval).
<!-- SECTION:NOTES:END -->
