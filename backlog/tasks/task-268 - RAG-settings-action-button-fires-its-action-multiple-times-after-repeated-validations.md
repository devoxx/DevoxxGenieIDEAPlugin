---
id: TASK-268
title: >-
  RAG settings action button fires its action multiple times after repeated
  validations
status: To Do
assignee: []
created_date: '2026-11-03T13:25:00Z'
updated_date: '2026-11-03T13:30:00Z'
labels:
  - bug
  - RAG
  - settings
milestone: m-0
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
priority: low
type: bug
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`RAGSettingsComponent.updateActionButtonState()` registers a new `ActionListener` on the shared `actionButton` every time it runs and never removes the old ones. After validation has run N times, one click runs the validation action (e.g. start ChromaDB, pull the embedding model) N times. Some of those listeners hold stale validation results, so they may act on a status that no longer applies.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Clicking the RAG settings action button runs its action exactly once, no matter how many times validation has run
- [ ] #2 The action uses the most recent validation result, not a stale one
- [ ] #3 A unit test reproduces the duplicate-listener behaviour and passes after the fix
<!-- AC:END -->
