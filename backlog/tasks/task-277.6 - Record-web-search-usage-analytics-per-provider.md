---
id: TASK-277.6
title: Record web search usage analytics per provider
status: Done
assignee:
  - '@claude'
created_date: '2026-11-18T10:18:00Z'
updated_date: '2026-11-18T10:26:00Z'
labels:
  - web-search
  - analytics
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1435
modified_files:
  - >-
    src/test/java/com/devoxx/genie/service/analytics/FeatureUsageTrackerWebSearchTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutorE2ETest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurableTest.java
  - >-
    src/test/java/com/devoxx/genie/service/analytics/AnalyticsSessionSnapshotServiceTest.java
  - src/main/java/com/devoxx/genie/service/analytics/FeatureUsageTracker.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionService.java
  - src/main/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/analytics/AnalyticsSessionSnapshotService.java
parent_task_id: TASK-277
priority: medium
type: bug
ordinal: 31000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Analytics only show which web search provider is selected, not how much web search is used. The per-prompt feature_used event for web search (FeatureUsageTracker.emitForPrompt) fires only when ChatMessageContext.isWebSearchActivated() is true, and that flag came from the chat Web toggle removed in 63af86b1, so it never fires for any provider (Tavily, Google, Keenable, Parallel). /search only counts as the generic custom_prompt, and agent web_search tool calls are only counted as the tool being enabled. Also, the session snapshot reports the selected provider as enabled even when web search is switched off and the agent tool is disabled, overstating adoption.

Context: provider feature ids exist and are allowlisted (web_search_tavily, web_search_google, web_search_keenable, web_search_parallel; TASK-277.3/.5). Every real search goes through WebSearchService.search. Only anonymous enum values and buckets may be sent (task-209 rules: no query text, no URLs, no keys).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Each completed /search sends one feature_used event with the selected provider's feature id
- [x] #2 Each completed agent web_search tool call sends feature_used for the provider's feature id and for web_search_agent_tool
- [x] #3 Settings Test search sends no usage event
- [x] #4 Failed searches (not configured, provider errors) send no usage event
- [x] #5 The dead isWebSearchActivated branch no longer emits, so a search is never counted twice
- [x] #6 The session snapshot reports a web search provider as enabled only when web search is on or the agent web_search tool is enabled
- [x] #7 No query text, URLs or credentials are sent; analytics failures never affect the search
- [x] #8 Unit tests cover each path above, including Test search, failures and the snapshot rule
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Branch feature/task-277.6-web-search-usage-analytics, stacked on local feature/task-277.5-parallel-provider @013863d7 (being merged via #1433 -> #1434 -> #1435). After #1435 merges: git rebase --onto origin/master 013863d7, then PR against master.

1. FeatureUsageTracker.webSearchUsed(FeatureId providerFeature, boolean viaAgentTool, @Nullable LanguageModel model): emits feature_used(providerFeature) and, for the agent tool, feature_used(web_search_agent_tool); bucket "0"; provider_type from the chat model (NONE for the agent tool, which has no chat context, as for other context-less events). Fail-silent.
2. Call sites (successful searches only, after results return):
   - WebSearchPromptExecutionService.searchWeb (/search) -> webSearchUsed(feature, false, context.getLanguageModel()); also for 'no results' (a search was made).
   - WebSearchToolExecutor.execute (agent) -> webSearchUsed(feature, true, null).
   - Settings Test search: never.
3. Remove the dead isWebSearchActivated branch from emitForPrompt (prevents double counting if a Web toggle ever returns: such prompts also run through searchWeb).
4. AnalyticsSessionSnapshotService: emit the provider feature only when a provider is selected AND (web search enabled OR the agent web_search tool is enabled).
5. Tests: tracker unit tests with a recording AnalyticsService; call-site tests for /search (results, no results, not configured, provider error), agent tool (success, error), Test search (none), snapshot matrix.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-18 verification: FeatureUsageTrackerWebSearchTest 5 (/search -> provider only with chat model provider_type; agent -> provider + web_search_agent_tool with provider_type none; null feature -> nothing; analytics failure swallowed; emitForPrompt with webSearchActivated no longer emits); call sites: WebSearchPromptExecutionServiceTest (completed search counts once with the chat model, no-results still counts, not-configured and 500 not counted), WebSearchToolExecutorE2ETest (completed tool search counts provider+agent tool, failures not counted), WebSearchProvidersConfigurableTest (Test search never counted); AnalyticsSessionSnapshotServiceTest (provider reported only when web search on or agent tool on; none when no provider). Full suite 4391 passed, 26 skipped, 0 failed. Note: tests were written before the code but not run in between, so their failing state was not observed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Web search usage is now counted per provider. FeatureUsageTracker.webSearchUsed sends feature_used with the selected provider's feature id (web_search_tavily/google/keenable/parallel) after every completed search: /search (provider_type from the chat model) and the agent web_search tool (also web_search_agent_tool; provider_type none, as the tool has no chat context). Searches with no results count; not-configured searches, provider errors and Settings Test search do not. The dead per-prompt branch keyed on the removed Web toggle is gone, so nothing is counted twice. The session snapshot reports a provider as enabled only when web search is on or the agent web_search tool is enabled. Only enum values and the "0" bucket are sent; analytics failures are swallowed.

Tests: FeatureUsageTrackerWebSearchTest (5) plus call-site and snapshot tests; full suite 4391 passed, 26 skipped, 0 failed.
<!-- SECTION:FINAL_SUMMARY:END -->
