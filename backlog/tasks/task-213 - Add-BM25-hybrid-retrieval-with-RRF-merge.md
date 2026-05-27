---
id: TASK-213
title: Add BM25 hybrid retrieval and RRF-merge with vector results
status: To Do
assignee: []
created_date: '2026-05-26 18:00'
updated_date: '2026-05-26 18:00'
labels:
  - enhancement
  - RAG
  - retrieval-quality
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/rag/SearchResult.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
  - src/main/java/com/devoxx/genie/service/MessageCreationService.java
  - 'https://en.wikipedia.org/wiki/Reciprocal_rank_fusion'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Pure cosine similarity over a single small embedding model is the weakest retrieval setup for code. Identifiers, symbol names, exact substrings — all carry strong semantic weight that small embedders frequently miss. A user asking "where is `AuthService` defined?" can get back chunks with semantically similar themes but the wrong class entirely, while a trivial keyword match would have nailed it.

This task adds a parallel **BM25** keyword retriever over the same chunks the vector store holds, runs it alongside the existing vector retriever, and merges the two ranked lists using **Reciprocal Rank Fusion** (RRF). RRF requires no tuning — it just blends ranks — and consistently outperforms either retriever alone on code corpora.

**Investigation areas:**
- **BM25 index location.** maintain an inverted index on disk alongside the existing manifest. Recommended: keyed by chunk id (file path + chunk index). Lucene is overkill and a heavy dep; a small in-process BM25 implementation (~200 lines) is the right size and the project already has the chunks in memory during indexing.
- **What to tokenize.** Code-aware tokenization beats whitespace splitting: split on case transitions (camelCase → `camel Case`), on `_` and `-`, and lowercase. Treat numeric tokens as low-weight. Optionally remove very common stop tokens.
- **Update hooks.** The BM25 index must be updated by the same code paths that touch the vector store: `ProjectIndexerService.indexFile`, `reindexFiles`, `removeFiles`. The watcher already routes through those, so no listener changes needed.
- **Persistence.** BM25 inverted index → sidecar JSON / binary file in the plugin data dir, peer to the manifest. Loaded once per project. Use the manifest's `flush` discipline so a crash doesn't corrupt state.
- **Merge math.** RRF score for a chunk = `sum(1 / (k + rank_i))` over each retriever's rank lists, with `k = 60` (Cormack et al. default). Final top-K is the configured `IndexerMaxResults`. Document this; do not expose `k` as a setting (unnecessary knob).
- **Toggle.** Add a "Hybrid retrieval (BM25 + vector)" toggle in RAG settings, default OFF for the first release so users can opt in. Once we have confidence, flip the default ON.
- **Logging.** The "Show RAG Only" log entries (task-completed in Phase 2) should annotate each hit with which retriever (or both) surfaced it, so debugging "why this chunk?" is tractable.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A persistent BM25 inverted index is maintained per project, updated by the same code paths that mutate the vector store (`indexFile` / `reindexFiles` / `removeFiles`) so the two stay in sync
- [ ] #2 BM25 tokenization splits camelCase / snake_case / kebab-case, lowercases, and demotes pure-numeric tokens — measurably stronger on code identifiers than a whitespace split
- [ ] #3 `SemanticSearchService.search` runs both retrievers in parallel and returns an RRF-merged ranked list (k=60, configured `IndexerMaxResults` returned); when the BM25 toggle is OFF, behaviour is unchanged from current
- [ ] #4 The "Show RAG Only" log records, per hit, which retrievers contributed (`vector` / `bm25` / `both`) so chunk provenance is debuggable
- [ ] #5 A "Hybrid retrieval (BM25 + vector)" toggle exists in RAG settings; default OFF; persisted via `DevoxxGenieStateService`
- [ ] #6 BM25 index is loaded lazily on first search and persisted via a flush model peer to `IndexManifest` (atomic write); corruption is detected on load and triggers a one-time rebuild rather than a crash
- [ ] #7 Unit tests cover: tokenizer (camelCase/_/-/digit handling), RRF merge correctness on synthetic ranked lists, BM25 update on file add/edit/delete, and end-to-end "exact identifier query returns the right chunk even when vector retrieval misses"
- [ ] #8 Indexing throughput regression is acceptable: BM25 update cost is dominated by reads, so total indexing time stays within +15% of pre-BM25 measurements on a representative project
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background from the Phase-1/2 RAG review: the review explicitly called out hybrid retrieval as one of two cheapest, highest-yield additions for code (the other is task-214 reranking). Vector-only is particularly weak for symbol queries — embedders compress identifier semantics into the same neighbourhood as conceptually related code, often missing the exact symbol the user typed.

Recommended sub-design:
- `Bm25Index` class: `Map<term, PostingsList>`; `PostingsList` = list of `(chunkId, termFrequency)`. Track document length per chunk and average length for the BM25 score formula.
- `Bm25Tokenizer`: regex-driven; pure function.
- `Bm25IndexService` (project-level @Service): owns one `Bm25Index` per project, like `IndexManifestService`.
- Score formula: `score(D, Q) = sum_t IDF(t) * tf(t,D)*(k1+1) / (tf(t,D) + k1*(1 - b + b*|D|/avgDL))` with `k1=1.2`, `b=0.75`.
- Chunk id format: `"{fileAbsPath}#{chunkIndex}"`.
- Storage format: gzipped JSON (or simple binary if size matters). Keep `embeddingSchemaVersion` so a schema bump invalidates BM25 too.

Open question to resolve in implementation: should BM25 update be synchronous (inside `storeSegments`) or async (after the embed batch returns)? Synchronous is simpler and won't outpace the vector store since BM25 is much cheaper.

Out of scope: cross-encoder reranking (separate task 214), AST-aware chunking (task 215), Lucene/Tantivy bindings (over-engineering for a single corpus this size).
<!-- SECTION:NOTES:END -->
