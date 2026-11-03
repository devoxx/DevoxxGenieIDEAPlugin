---
id: TASK-267
title: RAG settings validation must not run network calls on the EDT
status: To Do
assignee: []
created_date: '2026-11-03T13:25:00Z'
updated_date: '2026-11-03T13:30:00Z'
labels:
  - bug
  - RAG
  - threading
milestone: m-0
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
priority: medium
type: bug
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Opening or interacting with the RAG settings panel runs `RagValidatorService.validate()` inside `invokeLater` (`RAGSettingsHandler.performValidation()` and `RAGSettingsComponent.updateActionButtonState()`). The validators make network and Docker calls (Docker daemon, ChromaDB, Ollama `/api/tags`), so they run on the EDT. If one of those services is slow or unreachable, the Settings dialog and the whole IDE freeze until the call times out. This will get worse once embedding endpoints can be remote (TASK-265).

Constraint: the Settings dialog is modal, so results posted back to the UI must use `ModalityState.any()` or they won't appear until the dialog closes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Opening the RAG settings panel does not run any validator on the EDT
- [ ] #2 With Ollama or ChromaDB unreachable (e.g. connection hanging until timeout), the Settings dialog stays responsive while validation runs
- [ ] #3 Validation results still render while the modal Settings dialog is open
- [ ] #4 A test (or reproducible manual check documented in the task notes) demonstrates validation no longer executes on the EDT
<!-- AC:END -->
