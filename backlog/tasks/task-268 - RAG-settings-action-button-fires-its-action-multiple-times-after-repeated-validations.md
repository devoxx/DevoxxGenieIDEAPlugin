---
id: TASK-268
title: >-
  RAG settings action button fires its action multiple times after repeated
  validations
status: Done
assignee: []
created_date: '2026-11-03T13:25:00Z'
updated_date: '2026-11-04T17:56:00Z'
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

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Closed without a separate fix. The duplicate-listener bug was in `RAGSettingsComponent.updateActionButtonState()`, but `actionButton` was never added to any panel, so nobody could click it and the bug couldn't happen. The button and its code were removed under TASK-267 with the maintainer's approval. The acceptance criteria are left unchecked because they describe a button that no longer exists. The visible action buttons in `ValidatorsPanel` are created fresh on each refresh, so they don't accumulate listeners.
<!-- SECTION:FINAL_SUMMARY:END -->
