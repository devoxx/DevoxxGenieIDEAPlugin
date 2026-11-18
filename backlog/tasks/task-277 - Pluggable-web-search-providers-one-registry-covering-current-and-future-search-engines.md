---
id: TASK-277
title: >-
  Pluggable web search providers: one registry covering current and future
  search engines
status: To Do
assignee: []
created_date: '2026-11-18T08:10:00Z'
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
- [ ] #1 All five subtasks are Done or explicitly archived with a reason
- [ ] #2 Adding a new web search provider requires only an enum entry, a registry definition and an engine class, enforced by a wiring test
- [ ] #3 Web Search documentation describes how to choose a provider
<!-- AC:END -->
