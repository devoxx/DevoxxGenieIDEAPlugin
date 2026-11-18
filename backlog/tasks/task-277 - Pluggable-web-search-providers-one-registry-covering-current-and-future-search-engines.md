---
id: TASK-277
title: >-
  Pluggable web search providers: one registry covering current and future
  search engines
status: Done
assignee: []
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T11:19:00Z'
labels:
  - web-search
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1423
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1428
priority: high
type: feature
ordinal: 25000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Two vendors have asked to add their search engines as Web Search providers: Parallel (#1423) and Keenable (#1428). Today each of Tavily and Google is hard-coded in five separate places (the /search command readiness check, the agent web_search tool, the /search execution service, the chat Web toggle, and the settings Test search), and those copies apply different readiness rules. Adding a provider means editing every copy, and they already disagree, e.g. /search can accept a request the execution service then can't run.

Issue #1428 also reports that Google's Custom Search JSON API is closed to new customers and is discontinued on 2027-01-01, so the Google option is about to stop working.

Decision (maintainer, 2026-11-18): follow the existing registry pattern used for LLM routers (RouterRegistry) and local OpenAI-compatible servers (LocalProviderRegistry). Each web search provider is one definition in a registry; shared code asks the registry/one service instead of naming providers; a wiring test enforces completeness; the settings page lets the user pick one provider (single-select). A config-driven "custom HTTP/JSON" provider was considered and rejected as too fragile.

Subtasks, in order: (1) registry + central service, no behaviour change; (2) source URLs in /search answers; (3) Keenable provider; (4) deprecate Google; (5) Parallel provider.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All five subtasks are Done or explicitly archived with a reason
- [x] #2 Adding a new web search provider requires only an enum entry, a registry definition and an engine class, enforced by a wiring test
- [x] #3 Web Search documentation describes how to choose a provider
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Web search is now one registry of providers that every search path uses (/search, the agent web_search tool, the chat Web toggle and Settings Test search), following the RouterRegistry and LocalProviderRegistry pattern.

- 277.1 registry + WebSearchService (#1431); 277.2 source links in /search answers (#1432); 277.3 Keenable provider (#1433); 277.4 Google Custom Search deprecated before its 2027-01-01 shutdown (#1434); 277.5 Parallel provider (#1435 merged into a stale stacked branch, re-landed in #1436); 277.6 per-provider usage analytics (#1436); 277.7 readable Settings test-search results, shared with the RAG test (#1437).
- AC #2: a new provider needs an enum entry, a registry definition and an engine class; `WebSearchProviderRegistryTest.everyDefinition_isFullyWired` enforces this for every provider.
- AC #3: docusaurus/docs/features/web-search.md "Choosing a Provider" plus per-provider setup sections.

None of this is released yet: all of it is after the v1.18.0 tag. Follow-up: TASK-277.7 AC #3 and #10 (opening results in the browser or editor) are awaiting an in-IDE check; GitHub issues #1423 and #1428 are to be closed with comments once the maintainer approves the wording.
<!-- SECTION:FINAL_SUMMARY:END -->
