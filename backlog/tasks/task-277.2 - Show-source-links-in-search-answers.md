---
id: TASK-277.2
title: Show source links in /search answers
status: To Do
assignee: []
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T08:11:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1423
parent_task_id: TASK-277
priority: medium
type: enhancement
ordinal: 27000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The /search command answers with a single paragraph generated from the search results, and the result URLs are dropped, so users can't open or verify the sources behind an answer. The agent's web_search tool already keeps URLs. Raised in #1423; applies to every provider.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A /search answer in the chat lists the title and original URL of the results it was based on, as clickable links
- [ ] #2 Works the same for every registered provider
- [ ] #3 When search returns no results the user sees a clear 'no results' message instead of an empty sources list
- [ ] #4 Unit tests cover sources present, no results, and results without a title
- [ ] #5 Web Search documentation mentions the sources list
- [ ] #6 End-to-end test runs /search against a local stub search server and asserts the result URLs reach the chat message that is shown to the user
<!-- AC:END -->
