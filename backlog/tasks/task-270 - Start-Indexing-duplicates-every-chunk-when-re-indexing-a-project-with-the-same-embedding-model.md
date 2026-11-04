---
id: TASK-270
title: >-
  "Start Indexing" duplicates every chunk when re-indexing a project with the
  same embedding model
status: To Do
assignee: []
created_date: '2026-11-03T17:16:00Z'
labels:
  - bug
  - RAG
milestone: m-0
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
priority: medium
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while working on TASK-212 (2026-11-03), from reading the code; not reproduced against a live ChromaDB yet.

The RAG settings "Start Indexing" button calls `ProjectIndexerService.indexFiles(project, forceReindex = true, …)`. With `forceReindex`, every file skips the manifest's `isCurrent` check and goes through `processPath` → `storeSegments` → `EmbeddingStore.addAll`. Nothing removes the file's existing chunks first. Only `reindexFiles` (the file watcher path) calls `removeChunksFromStore` before re-embedding. langchain4j's Chroma `addAll` generates new random ids, so each manual re-index adds a second copy of every chunk.

Expected effects: the collection's "Indexed Segments" count grows by the full chunk count on every click, and search returns the same chunk several times, pushing other results out of the top-K.

TASK-212 does not have this problem when the model changes: a full index with a different embedding model clears the whole collection first (`prepareIndexFor`). The bug only applies to re-indexing with the same model.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A test reproduces the duplication first: indexing the same file twice through `indexFiles(..., forceReindex = true, ...)` leaves exactly one copy of each chunk in the store
- [ ] #2 A forced re-index replaces a file's chunks instead of adding to them (e.g. remove the file's chunks before storing new ones, as `reindexFiles` does), without changing behaviour when the manifest says the file is current
- [ ] #3 The fix does not add a per-file delete round-trip when it isn't needed (for example, files the manifest has never tracked)
- [ ] #4 Existing RAG indexer tests and the chunking characterization test still pass
<!-- AC:END -->
