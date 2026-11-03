---
id: TASK-265
title: >-
  Support OpenAI-compatible embedding endpoints with query/document prefixes and
  dimension truncation for RAG
status: To Do
assignee: []
created_date: '2026-11-03T13:24:00Z'
updated_date: '2026-11-03T13:49:00Z'
labels:
  - enhancement
  - RAG
  - settings
milestone: m-0
dependencies:
  - TASK-212
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/523
  - >-
    https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/523#issuecomment-5920487971
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
  - src/main/java/com/devoxx/genie/service/rag/cli/RagCli.java
  - https://huggingface.co/nomic-ai/nomic-embed-text-v1.5
priority: medium
type: feature
ordinal: 10000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
GitHub #523: RAG embeddings can only come from Ollama. A user (comment 5920487971) runs jina-embeddings-v5-text-nano on a local GPU through llama.cpp / crispembed, which expose an OpenAI-compatible `/v1/embeddings` endpoint (e.g. `http://127.0.0.1:8001/v1`), and cannot point DevoxxGenie at it.

The comment raises two more needs:
- **Asymmetric models.** Many modern embedders expect a query prefix on search queries and a different document prefix on indexed chunks (e.g. `Query:` / `Document:`). Without them retrieval quality drops. The current default, nomic-embed-text, is itself asymmetric (`search_query: ` / `search_document: `) and is embedded today with no prefixes, so existing users are also getting worse retrieval than they could.
- **Matryoshka truncation.** Cutting vectors (e.g. 768 → 512/256 dimensions) reduces memory, cache footprint and query time for a small quality cost. Local servers may ignore a requested `dimensions` parameter, so truncation must work regardless of backend.

TASK-212 (prerequisite) makes the Ollama embedding model name configurable and binds stored embeddings to the model, so changing it triggers re-indexing. This task extends that to a second provider type and to the new knobs. Everything that changes the vector space (provider/model, dimensions, document prefix) must be part of that binding. Otherwise switching settings either fails on a dimension mismatch, or, at equal dimensions, silently returns meaningless results because the manifest still reports files as current.

Decisions already made with the maintainer:
- Configuration lives in the RAG Settings UI, not environment variables. IDEs launched from the macOS Dock don't inherit shell env vars. Env vars remain acceptable for the standalone `RagCli` only.
- Prefixes are applied only when computing embeddings. They must never appear in stored segment text, `<SemanticContext>` or `semantic_search` output.
- The Ollama validators must not block RAG when the OpenAI-compatible provider is selected.

A Settings "Test" button that checks these settings end-to-end is tracked as a separate follow-up task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 RAG settings offer an embedding provider choice: Ollama (default, existing behaviour) or OpenAI-compatible
- [ ] #2 For OpenAI-compatible, the user can set base URL, model name and an optional API key (stored securely, not in plain settings XML); indexing and semantic search both use that endpoint
- [ ] #3 Users can configure a query prefix and a document prefix; the query prefix is applied to every search query embedding and the document prefix to every indexed chunk embedding (batch and per-segment fallback paths alike)
- [ ] #4 Prefixes never appear in stored segment text, in the injected <SemanticContext> block, or in semantic_search tool output
- [ ] #5 When nomic-embed-text is the selected model, the prefix fields default to its documented `search_query: ` / `search_document: ` prefixes; other models default to empty
- [ ] #6 An optional output dimension can be set; embeddings are truncated to that size and re-normalised to unit length for both indexing and querying, independent of whether the backend honours a dimensions parameter
- [ ] #7 A configured dimension larger than the model's native dimension is reported as an error rather than silently ignored or padded
- [ ] #8 The embedding fingerprint from TASK-212 covers provider, model, output dimension and document prefix: changing any of them marks the index stale and offers re-indexing; changing only the query prefix does not
- [ ] #9 With the OpenAI-compatible provider selected, the Ollama and nomic-embed-text validators no longer block RAG; a provider-appropriate reachability check is used instead
- [ ] #10 RagCli can target an OpenAI-compatible endpoint via environment variables (URL, model, key, prefixes, dimension)
- [ ] #11 Unit tests cover: prefix application on query vs document paths, prefixes absent from stored text, truncation + re-normalisation, fingerprint change detection per setting, and provider selection in the validator chain
- [ ] #12 The docusaurus RAG page documents the OpenAI-compatible option with a llama.cpp example, prefixes, and truncation
- [ ] #13 Existing installs that upgrade with nomic-embed-text also switch to the `search_query: ` / `search_document: ` defaults; their existing index is detected as stale and the user is told once, with a one-click "Re-index now" action. No re-index starts automatically, and the notification is not repeated after the user acts on or dismisses it
- [ ] #14 Until the user re-indexes, semantic search for an upgraded install keeps working against the existing index (using the settings that built it) instead of failing or returning empty results
- [ ] #15 The TASK-269 benchmark can also target an OpenAI-compatible endpoint, and the plugin's embedding path reuses the prefix/truncation component built in TASK-269 rather than a second implementation
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-03 decision (maintainer): nomic prefix rollout uses option 1, prefixes on by default for everyone including existing installs, with a one-time re-index prompt. Rejected alternatives: grandfathering existing installs (two groups of users with different quality), query-prefix-only for old indexes (off-spec for nomic, may hurt retrieval), background rebuild into a second collection (too complex for now; conflicts with the 'no silent multi-minute embed job' rule from TASK-212), and mixing old/new prefixed vectors in one collection (scores can't be compared). The retrieval benchmark task in this milestone can quantify the gain for the upgrade notification text.
<!-- SECTION:NOTES:END -->
