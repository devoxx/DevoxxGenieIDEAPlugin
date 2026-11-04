---
id: TASK-266
title: >-
  Add a "Test" button to RAG settings that verifies the unsaved RAG
  configuration end-to-end
status: To Do
assignee: []
created_date: '2026-11-03T13:25:00Z'
updated_date: '2026-11-03T13:30:00Z'
labels:
  - enhancement
  - RAG
  - settings
  - UX
milestone: m-0
dependencies:
  - TASK-265
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/523
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/validator/NomicEmbedTextValidator.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
priority: medium
type: feature
ordinal: 11000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Once embeddings can come from an arbitrary OpenAI-compatible endpoint with custom prefixes and truncated dimensions (TASK-265), there are many ways to misconfigure RAG: wrong URL or key, a chat model instead of an embedding model on the endpoint, a dimension larger than the model supports, or prefixes that break retrieval. Today users only find out after a multi-minute indexing run, or worse, from silently bad search results.

The maintainer wants a "Test" button in the RAG settings so users can confirm, before applying, that their RAG settings will keep working.

Constraints a future implementer needs:
- The existing validators (`RagValidatorService`: Docker → ChromaDB → Ollama → nomic-embed-text) read **saved** state from `DevoxxGenieStateService`. The Test button has to validate the values currently typed in the form, before Apply. The automatic validation and the Test button should share one implementation, not two copies.
- The Settings dialog is modal. Background work started from it must still deliver results while the dialog is open (see project memory: modal settings dialogs defer EDT work unless `ModalityState.any()` is used). Network calls must not run on the EDT.
- The test must be side-effect free: it must not write to, or delete from, the project's Chroma collection or manifest.

TASK-216 (RAG health panel) plans to render the same validator rows. Whichever lands second should reuse the other's rendering rather than duplicate it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The RAG settings panel has a "Test" button that checks the values currently entered in the form, including unsaved, un-applied edits
- [ ] #2 Results are shown as one row per check with pass/fail/warning status, a short message, and elapsed time
- [ ] #3 Checks: ChromaDB (and Docker when applicable) reachable on the configured port; the embedding endpoint reachable for the selected provider (Ollama: configured model present, with the existing Pull action; OpenAI-compatible: URL and API key accepted)
- [ ] #4 A sample document (with document prefix) and a sample query (with query prefix) are embedded; the test fails if no vector is returned, if it is all zeros or contains NaN, or if the configured output dimension exceeds the model's native dimension; the native dimension is displayed
- [ ] #5 A retrieval check verifies a query scores higher against a related snippet than an unrelated one, so a non-embedding model or badly wrong prefixes are reported as a failure even when the endpoint is reachable
- [ ] #6 If the tested settings would change the embedding fingerprint of the existing index, a warning (not a failure) states that applying them requires re-indexing and how many files are affected
- [ ] #7 Running the test never writes to or deletes from the project's Chroma collection or index manifest
- [ ] #8 The test runs off the EDT, the dialog stays responsive while it runs, results appear while the modal Settings dialog is still open, and the button is disabled while a test is in progress
- [ ] #9 Automatic validation on panel open and the Test button use the same validator code path, parameterised by a settings snapshot rather than global saved state
- [ ] #10 Unit tests cover each check's pass and fail outcome against a stubbed embedding model and store, including the retrieval-ordering failure and the fingerprint-change warning
- [ ] #11 The docusaurus RAG page describes the Test button and how to read its results
<!-- AC:END -->
