---
id: TASK-277.4
title: Deprecate the Google Custom Search provider ahead of its 2027-01-01 shutdown
status: To Do
assignee: []
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T08:11:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1428
  - https://developers.google.com/custom-search/v1/overview
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
- [ ] #1 Shutdown date confirmed against Google's documentation and recorded in the task notes (archive this task if not confirmed)
- [ ] #2 Settings show a deprecation warning when Google is selected, naming the shutdown date and suggesting another provider
- [ ] #3 Google is not the suggested/default provider for new setups
- [ ] #4 Search errors from a shut-down Google API produce a message pointing to the deprecation
- [ ] #5 Web Search documentation marks Google as deprecated
- [ ] #6 Unit tests cover the warning and error message
<!-- AC:END -->
