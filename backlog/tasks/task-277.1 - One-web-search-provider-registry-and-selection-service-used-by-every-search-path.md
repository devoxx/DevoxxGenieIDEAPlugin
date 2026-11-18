---
id: TASK-277.1
title: >-
  One web search provider registry and selection service used by every search
  path
status: Done
assignee:
  - '@claude'
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T08:59:00Z'
labels:
  - web-search
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1423
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1428
modified_files:
  - >-
    src/test/java/com/devoxx/genie/service/websearch/WebSearchProviderRegistryTest.java
  - >-
    src/test/java/com/devoxx/genie/service/analytics/AnalyticsEventBuilderAccess.java
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceWebSearchTest.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchCredentialField.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchProviderDefinition.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchConfig.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchReadiness.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchNotConfiguredException.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchProviderRegistry.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchService.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/test/java/com/devoxx/genie/service/prompt/command/SearchCommandTest.java
  - src/test/java/com/devoxx/genie/service/websearch/TavilyStubServer.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutorE2ETest.java
  - >-
    src/test/java/com/devoxx/genie/service/websearch/WebSearchServiceTestSupport.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionServiceTest.java
  - src/main/java/com/devoxx/genie/service/prompt/command/SearchCommand.java
  - src/main/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/WebSearchPromptStrategy.java
  - src/main/java/com/devoxx/genie/util/ChatMessageContextUtil.java
  - src/main/java/com/devoxx/genie/service/analytics/FeatureUsageTracker.java
  - >-
    src/main/java/com/devoxx/genie/service/analytics/AnalyticsSessionSnapshotService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptions.java
  - >-
    src/test/java/com/devoxx/genie/service/analytics/AnalyticsSessionSnapshotServiceTest.java
  - src/test/java/com/devoxx/genie/util/ChatMessageContextUtilTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/strategy/WebSearchPromptStrategyTest.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersComponent.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurable.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurableTest.java
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchLiveSmokeTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/websearch/cli/WebSearchCli.java
  - docusaurus/static/api/tips.json
  - src/main/java/com/devoxx/genie/service/tips/TipService.java
  - docusaurus/docs/features/web-search.md
  - docusaurus/docs/configuration/settings.md
  - docusaurus/docs/features/commands.md
  - docusaurus/docs/features/overview.md
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchSettingsScreenshotGenerator.java
  - docusaurus/static/img/web-search-settings.png
parent_task_id: TASK-277
priority: high
type: enhancement
ordinal: 26000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Foundation for TASK-277. Tavily and Google are each hard-coded in seven places with inconsistent readiness rules: SearchCommand and WebSearchToolExecutor check enabled+non-blank keys; WebSearchPromptExecutionService accepts Tavily when enabled even with no key and only null-checks Google keys; ChatMessageContextUtil (Web toggle) checks enabled flags only; the settings Test search has its own branch; AgentSettingsComponent's warning and the analytics classes each check again. Move both existing providers into a registry (mirroring RouterRegistry / LocalProviderRegistry) and route every path through one selection/readiness path, so later provider tasks (Keenable, Parallel) only add a definition and an engine.

Settings move from two independent checkboxes to a single selected provider. Existing users must keep working: Tavily wins today when both are on, so an enabled Tavily migrates to Tavily selected, otherwise an enabled Google to Google, otherwise none. Migration must not read credentials (they live in PasswordSafe).

No new providers and no user-visible behaviour change in this task beyond the single-select settings control, consistent readiness, and provider-neutral messages.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 /search readiness, the Web toggle, the agent web_search tool, /search execution and settings Test search all use the same provider selection and readiness rule
- [x] #2 A selected provider with a blank required credential never sends a search request; /search, the Web toggle, the agent tool and Test search each show a clear message naming the provider and pointing to Settings -> Web search
- [x] #3 Existing settings migrate from the old enabled checkboxes without reading credentials: Tavily enabled -> Tavily selected; else Google enabled -> Google selected; else no provider
- [x] #4 Settings page offers one provider choice and shows only that provider's credential fields; selection and keys persist across restart
- [x] #5 Settings Test search dispatches to the provider currently chosen in the (unsaved) form
- [x] #6 A wiring test fails if any provider enum value lacks a registry definition or engine factory
- [x] #7 Unit tests cover readiness per path, migration cases, and no-provider/inactive behaviour; existing web search tests pass
- [x] #8 Web Search documentation updated for the single provider choice
- [x] #9 End-to-end tests drive the real Tavily engine through the central service against a local stub HTTP server (no real network) for /search execution, the agent web_search tool and settings Test search; with Google selected the Tavily stub receives no requests (Google's engine has no base-URL override, so it is substituted at the registry seam)
- [x] #10 Settings round-trip test: apply the settings form, reload state, and get the same provider and credentials back
- [x] #11 Optional live smoke tests per provider run only when that provider's key is present in the environment and are skipped otherwise
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Branch: feature/task-277.1-web-search-provider-registry (from origin/master 11e8f7be). Unrelated WIP in the working tree (PendingPromptQueue*, groma/*) is NOT part of this task — never stage it.

Research findings: provider logic lives in 7 places, not 5 — SearchCommand, WebSearchToolExecutor, WebSearchPromptExecutionService, ChatMessageContextUtil (Web toggle), WebSearchProvidersComponent (Test search), AgentSettingsComponent.isAnyWebSearchKeyConfigured, analytics (AnalyticsSessionSnapshotService, FeatureUsageTracker). TavilyWebSearchEngine builder has baseUrl(); GoogleCustomWebSearchEngine builder has NO baseUrl, so Google cannot be stub-served — Google e2e uses a substituted engine at the definition seam + an env-gated live smoke test.

1. New package com.devoxx.genie.service.websearch:
   - WebSearchProvider enum (TAVILY, GOOGLE).
   - WebSearchCredentialField record (CredentialKey key, label, helpUrl, required).
   - WebSearchProviderDefinition record (provider, displayName, signupUrl, hint, fields, engineFactory: Function<WebSearchConfig, WebSearchEngine>).
   - WebSearchConfig record: selected provider (nullable) + credential values + maxResults; factories fromState(state) and from the unsaved settings form.
   - WebSearchProviderRegistry: static definitions, definition(provider), all().
   - WebSearchService (application service in plugin.xml): isReady(config) [master switch on, provider selected, all required fields non-blank], createEngine(config) -> Optional, search(config, query) -> results. Constructor takes a definition lookup so tests can point Tavily at MockWebServer.
2. State: new persisted field webSearchProvider (enum name or null). loadState migration only when null: tavilySearchEnabled -> TAVILY, else googleSearchEnabled -> GOOGLE, else none (no key reads during loadState; readiness still requires keys). Legacy booleans kept in sync on write for downgrade safety, no longer read elsewhere.
3. Route all 7 call sites through WebSearchService; make user-facing messages provider-neutral (tool description, tip, errors).
4. Settings UI: provider combo (None + registry entries) and per-provider credential fields generated from the definition (CardLayout); Test search snapshots the form into WebSearchConfig and calls WebSearchService.
5. Docs: docusaurus/docs/features/web-search.md.

Tests (TDD — write failing tests first):
- WebSearchProviderRegistryTest.everyDefinition_isFullyWired.
- WebSearchServiceTest: readiness matrix (switch off, none selected, blank key, Google missing CSI, ready), dispatch per provider.
- DevoxxGenieStateServiceTest: migration cases + already-migrated untouched.
- Path tests: SearchCommand, WebSearchToolExecutor, ChatMessageContextUtil, AgentSettingsComponent warning, analytics, configurable.
- E2E (MockWebServer, real TavilyWebSearchEngine through WebSearchService): agent web_search tool, /search execution (fake ChatModel asserts retrieved content), settings Test search; with GOOGLE selected the Tavily stub receives zero requests.
- Settings round-trip: apply -> getState -> loadState into fresh instance -> same provider + keys.
- Live smoke: @EnabledIfEnvironmentVariable TAVILY_API_KEY / GOOGLE_SEARCH_KEY+GOOGLE_CSI_KEY.

Revision 2026-11-18 (after review): WebSearchService is NOT an application service — it is a final class with static functions (isReady/readiness reason, createEngine, search) over WebSearchConfig, plus a package-private definition-lookup override for tests. Web toggle (ChatMessageContextUtil) sets webSearchRequested when the master switch is on and a provider is selected (no credential read); missing credentials surface as a clear error from the web search execution path. The settings component builds WebSearchConfig from the unsaved form; nothing is written to state until apply().
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-18 decisions (maintainer):
- Google e2e: substitute the engine at the registry factory seam + env-gated live smoke test (no in-repo Google engine; TASK-277.4 deprecates it).
- Web toggle with selected provider but blank key: route to web search and fail fast with a clear provider-named config error (no PasswordSafe read per prompt in ChatMessageContextUtil; never silently fall back to a non-web answer).
- Selection/readiness are static pure functions (LocalProviderRegistry style), not an IntelliJ application service, so existing ApplicationManager-mocking tests keep working; package-private seam for the Tavily stub base URL.
- Settings combo must not mutate state live (unlike the old checkbox listeners) so Test search can use the unsaved form.
- No separate e2e Gradle task exists in the repo: e2e tests are *E2ETest classes in the normal ./gradlew test run.
- Unrelated WIP stashed as stash 'wip-before-task-277.1' (PendingPromptQueue*, groma/).

2026-11-18 progress: core registry/service/config + state migration done; all 7 call sites routed (SearchCommand, agent tool, /search execution + strategy error path, ChatMessageContextUtil, AgentSettingsComponent warning, both analytics classes); settings page rebuilt (provider combo + registry-driven credential rows, no live state writes, Test search via performTestSearch(formConfig)). WebSearchCli (test sources) also uses the registry.

Finding: the chat 'Web toggle' no longer exists in the UI (removed in 63af86b1, 'remove web search session toggle from SearchOptionsPanel'); nothing sets webSearchActivated, so the ChatMessageContextUtil path is dead code that is now merely consistent. Docs/tips no longer mention a per-conversation toggle.

User requests mid-task: (1) update the online docs incl. the soon-to-be-available providers -> web-search.md rewritten with a 'More providers coming soon' section (Keenable, Parallel marked not available yet) and a cautious Google note linking Google's page (shutdown date NOT asserted; TASK-277.4 verifies); settings.md/commands.md/overview.md aligned. (2) new search tips in tips.json (+ TipService fallback). tips.json is fetched live by all installed versions, so tips are worded to be true for the current release too; provider-specific tips wait for 277.3/277.5.

Settings behaviour change: unchecking 'Enable feature' no longer clears the provider choice (old checkbox listeners unticked both providers).

Known doc gap: /img/web-search-settings.png shows the old checkbox layout; needs a new screenshot from a running IDE.

Screenshot: docusaurus/static/img/web-search-settings.png was a 1x1 placeholder pixel; replaced with a real render (1800x942, 2x) of the new WebSearchProvidersComponent produced by WebSearchSettingsScreenshotGenerator (platform test, runs only with GENERATE_DOCS_SCREENSHOTS=true). Full suite: 4273 passed, 23 skipped, 0 failed.

Verification evidence per AC: #1/#2 SearchCommandTest, WebSearchToolExecutorE2ETest, WebSearchPromptExecutionServiceTest, WebSearchPromptStrategyTest (not-configured message verbatim), ChatMessageContextUtilTest (provider selected, no credential reads), WebSearchProvidersConfigurableTest$TestSearch; #3 DevoxxGenieStateServiceWebSearchTest$Migration (9 cases); #4/#10 WebSearchProvidersConfigurableTest$Visibility/$Apply/$Reset/$RoundTrip (XML serialize -> loadState -> reopen page); #5 formConfig + e2e Test search with unsaved key; #6 WebSearchProviderRegistryTest.everyDefinition_isFullyWired (also checks analytics allowlist); #7 WebSearchServiceTest readiness matrix; #8 docs build passes with onBrokenLinks=throw, anchors verified, new screenshot; #9 MockWebServer e2e for tool, /search and Test search, Tavily stub request count 0 when Google selected; #11 WebSearchLiveSmokeTest (env-gated, skipped without keys).

Also fixed (user request): flaky ParallelExploreToolExecutorTest.cancel_setsFlag_andCancelsActiveRunners. Root cause: mockConstruction is thread-local, but the test called execute() from a pool thread, so a REAL SubAgentRunner was built there; the cancel verify was silently skipped by an if-guard, and the real run plus two never-terminated executors leaked threads that IntelliJ's ThreadLeakTracker intermittently reported. Now execute() runs on the test thread, cancel() comes from a helper thread after the runner starts, verification is unconditional, and both pools are shut down and awaited. Passed 3 uncached reruns.

Final full suite: 4273 passed, 24 skipped, 0 failed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Web search providers are now pluggable. Tavily and Google moved into a WebSearchProviderRegistry (enum + one definition per provider: name, hint, credential fields, analytics feature, engine factory), and WebSearchService is the single selection/readiness/engine path used by /search, /search execution, the agent web_search tool, the chat-context web flag, Settings Test search, the Agent settings warning and analytics. This replaces seven copies of provider logic that disagreed (e.g. /search accepted Tavily with no key).

Behaviour:
- One selected provider (persisted webSearchProvider), migrated from the old checkboxes without reading credentials (Tavily wins as before); legacy flags kept in sync for downgrades.
- A selected provider with missing credentials never sends a request; every path shows a provider-specific message, e.g. "Tavily is missing its API key. Enter it in Settings → DevoxxGenie → Web search."
- Settings page: provider dropdown with registry-driven credential rows; nothing written before Apply; Test search uses the unsaved form; all providers' keys are kept.
- Unchecking "Enable feature" no longer clears the provider choice.

Docs/tips: web-search.md rewritten (provider choice, setup, readiness messages, "More providers coming soon" for Keenable/Parallel, cautious Google note), settings/commands/overview pages aligned, real settings screenshot replaces a 1x1 placeholder (WebSearchSettingsScreenshotGenerator), three new search tips in tips.json and the TipService fallback.

Tests: registry wiring test, readiness matrix, migration and XML round-trip, per-call-site tests, MockWebServer end-to-end tests with the real Tavily engine (Google substituted at the registry seam; its engine has no base URL), env-gated live smoke tests. Also fixed the flaky ParallelExploreToolExecutorTest cancel test (thread-local mockConstruction built a real runner on a pool thread). Full suite: 4273 passed, 24 skipped, 0 failed.

Follow-ups: TASK-277.2 (source links), 277.3 (Keenable), 277.4 (verify and deprecate Google), 277.5 (Parallel). The chat Web toggle was removed earlier (63af86b1), so the chat-context web path is currently unreachable from the UI.
<!-- SECTION:FINAL_SUMMARY:END -->
