---
id: TASK-277.4
title: Deprecate the Google Custom Search provider ahead of its 2027-01-01 shutdown
status: Done
assignee:
  - '@claude'
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T09:44:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1428
  - https://developers.google.com/custom-search/v1/overview
modified_files:
  - >-
    src/test/java/com/devoxx/genie/service/websearch/WebSearchDeprecationTest.java
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/service/websearch/WebSearchProviderRegistryTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurableTest.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutorE2ETest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/strategy/WebSearchSourcesE2ETest.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchDeprecation.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchProviderDefinition.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/websearch/WebSearchProviderRegistry.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchService.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersComponent.java
  - docusaurus/docs/features/web-search.md
parent_task_id: TASK-277
priority: medium
type: task
ordinal: 29000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Issue #1428 reports that Google's Custom Search JSON API (behind our Google web search option) is closed to new customers and discontinued on 2027-01-01. Not yet verified by the maintainer; verify against Google's overview page first. If confirmed, users still on Google need to be warned and pointed at another provider before it stops working, and new users shouldn't be steered toward it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Shutdown date confirmed against Google's documentation and recorded in the task notes (archive this task if not confirmed)
- [x] #2 Settings show a deprecation warning when Google is selected, naming the shutdown date and suggesting another provider
- [x] #3 Google is not the suggested/default provider for new setups
- [x] #4 Search errors from a shut-down Google API produce a message pointing to the deprecation
- [x] #5 Web Search documentation marks Google as deprecated
- [x] #6 Unit tests cover the warning and error message
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. WebSearchProviderDefinition gains an optional WebSearchDeprecation (end date, replacement hint, details URL). Google's: 2027-01-01, link to Google's overview page. Messages switch tense once the date has passed (Clock-injectable for tests).
2. WebSearchService.createEngine wraps the engine of a deprecated provider so ANY failure becomes a WebSearchProviderException pointing to the deprecation (before the date: check key/ID, note the shutdown and suggest Keenable/Tavily; after: "was discontinued", choose another provider). Wrapping in the service (not the factory) keeps it on for substituted engines in tests. /search, the agent tool and Test search already show these messages verbatim.
3. Settings: dropdown shows "Google Custom Search (deprecated)"; a warning row naming the date and suggesting Keenable or Tavily appears only while Google is selected.
4. New setups: default stays None; reorder providers so Google is listed last (TAVILY, KEENABLE, GOOGLE; names are persisted, not ordinals).
5. Registry wiring test: a deprecated definition must have a date and a non-blank message.
6. Tests: deprecation messages before/after the date, wrapper (success passes through, failure mapped, provider exceptions keep kind), settings warning visibility + label + order, E2E via the agent tool, /search and Test search with a failing substituted Google engine.
7. Docs: mark Google deprecated with the confirmed date and Google's own wording/link; recommend Keenable/Tavily.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-18 AC#1 evidence, from https://developers.google.com/custom-search/v1/overview (fetched 2026-11-18): "The Custom Search JSON API is closed to new customers." / "Existing Custom Search JSON API customers have until January 1, 2027 to transition to an alternative solution." / pricing: "until the service discontinuation on January 1, 2027". Google recommends Vertex AI Search (up to 50 domains) or contacting Google for full web search. Shutdown CONFIRMED -> task proceeds.

Branch feature/task-277.4-deprecate-google-search stacked on feature/task-277.3-keenable-provider (#1433 -> #1432 -> #1431, none merged yet). Maintainer delegated all required steps.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Google Custom Search is now deprecated in DevoxxGenie, ahead of Google discontinuing its Custom Search JSON API on January 1, 2027 (confirmed on Google's overview page, 2026-11-18: closed to new customers, existing customers have until 2027-01-01).

- WebSearchProviderDefinition gains an optional WebSearchDeprecation (end date, Google's announcement URL, alternatives "Keenable or Tavily"); messages switch to "was discontinued" on the date (Clock-injectable).
- WebSearchService.createEngine wraps a deprecated provider's engine: any failure becomes a WebSearchProviderException naming the shutdown and suggesting Keenable/Tavily (before the date: also check key and Search Engine ID). The raw failure is kept as the cause but never shown. /search, the agent web_search tool and Settings Test search show it.
- Settings: "Google Custom Search (deprecated)" in the dropdown, listed last (enum reordered to TAVILY, KEENABLE, GOOGLE; names are persisted, not ordinals); a warning row with the date and alternatives appears only while Google is selected. The default stays None.
- Registry wiring test: a deprecated definition must have a date, an https announcement URL and alternatives.
- Docs: Google marked deprecated with Google's own wording and date; new setups pointed to Keenable or Tavily; the setup section notes it's for existing customers only.

Verification: WebSearchDeprecationTest (7), WebSearchServiceTest$DeprecatedProviders (3) + provider order, settings Visibility (warning shown/hidden, default None) and TestSearch (failing Google), E2E agent tool and /search with a failing substituted Google engine; docs build OK; full suite 4348 passed, 25 skipped, 0 failed.

Stacked on TASK-277.3 (#1433).
<!-- SECTION:FINAL_SUMMARY:END -->
