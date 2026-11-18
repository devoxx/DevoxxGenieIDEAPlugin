---
id: TASK-277.3
title: Add Keenable as an optional web search provider
status: To Do
assignee: []
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T08:36:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1428
  - https://docs.keenable.ai/api-reference/search
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
- [ ] #1 Keenable can be selected in Settings -> Web search and works for Test search, /search, the Web toggle and the agent web_search tool
- [ ] #2 Works with no key via the public endpoint; with a key, uses the authenticated endpoint
- [ ] #3 No request is sent to Keenable unless it is the selected provider
- [ ] #4 Rate-limit and authentication failures show the user a clear, specific message
- [ ] #5 Requests identify DevoxxGenie (title header / User-Agent)
- [ ] #6 Unit tests cover keyless and keyed requests, response mapping, empty results and error responses; wiring test passes
- [ ] #7 Web Search documentation describes Keenable, keyless limits, and how to add a key
- [ ] #8 End-to-end tests run the Keenable engine through the central service against a local stub server: keyless and keyed endpoints, headers, rate-limit (429) and auth (401/403) responses
- [ ] #9 Optional live keyless smoke test, skipped unless explicitly enabled by an environment variable
- [ ] #10 docs/features/web-search.md moves Keenable from 'More providers coming soon' into 'Choosing a Provider' with setup steps
- [ ] #11 A Keenable tip is added to docusaurus/static/api/tips.json and the TipService fallback list, published only once a release containing Keenable is out (tips.json is fetched live by all installed versions)
<!-- AC:END -->
