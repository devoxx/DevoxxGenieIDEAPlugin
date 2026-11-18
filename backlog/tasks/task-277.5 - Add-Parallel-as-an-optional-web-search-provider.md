---
id: TASK-277.5
title: Add Parallel as an optional web search provider
status: To Do
assignee: []
created_date: '2026-11-18T08:11:00Z'
updated_date: '2026-11-18T08:36:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1423
  - https://docs.parallel.ai/integrations/mcp/quickstart
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
- [ ] #2 Parallel can be selected in Settings -> Web search and works for Test search, /search, the Web toggle and the agent web_search tool
- [ ] #3 No request is sent to Parallel unless it is the selected provider
- [ ] #4 Rate-limit and authentication failures show the user a clear, specific message
- [ ] #5 Unit tests cover request, response mapping, empty results and errors; wiring test passes
- [ ] #6 Web Search documentation describes Parallel
- [ ] #7 End-to-end tests run the Parallel engine through the central service against a local stub server, including empty, rate-limited and error responses
- [ ] #8 docs/features/web-search.md moves Parallel from 'More providers coming soon' into 'Choosing a Provider' with setup steps
- [ ] #9 A Parallel tip is added to docusaurus/static/api/tips.json and the TipService fallback list, published only once a release containing Parallel is out (tips.json is fetched live by all installed versions)
<!-- AC:END -->
