---
id: TASK-214
title: Add a cross-encoder reranker stage after retrieval
status: To Do
assignee: []
created_date: '2026-05-26 18:00'
updated_date: '2026-05-26 18:00'
labels:
  - enhancement
  - RAG
  - retrieval-quality
dependencies:
  - TASK-213
references:
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - src/main/java/com/devoxx/genie/service/rag/SearchResult.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/MessageCreationService.java
  - 'https://huggingface.co/BAAI/bge-reranker-v2-m3'
  - 'https://ollama.com/library/bge-reranker'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Bi-encoder (vector) retrieval is fast but coarse: it compares query and chunk in independently-computed embedding spaces, missing fine-grained relevance signals. A **cross-encoder reranker** runs the query and each candidate chunk *together* through a small transformer and produces a scalar relevance score — much more accurate, but too slow to apply at the corpus level. The standard pattern is to retrieve top-K (e.g. 30) cheaply and rerank that shortlist down to top-N (e.g. 5).

Reranking alone — without changing anything else — frequently doubles answer quality on RAG benchmarks. It's especially worth it once we have BM25 (task-213) feeding more candidates into the reranker.

**Investigation areas:**
- **Where the reranker lives.** Two options: (a) Ollama-hosted reranker model (e.g. `bge-reranker`) — keeps the stack one-runtime, slow on CPU; (b) cloud reranker (Cohere Rerank, Voyage AI rerank) — fast but adds an API key and an external dependency. Recommended: support (a) as the default to stay local-first, and (b) as an opt-in via existing API key plumbing. Phase 4 if there's demand for hosted.
- **Shortlist size.** Retrieval should fetch a wider net than today's top-K. Suggested: retrieval top-K = 30 (configurable via existing `IndexerMaxResults` setting, with a separate "reranker shortlist size" knob), reranked to the user-configured `IndexerMaxResults` for the prompt. Document the cost tradeoff (30 reranker calls per query).
- **Latency budget.** Local CPU reranking can take 1-3s on 30 candidates with a base model; that's added to every prompt when RAG is active. The "skip RAG on short follow-ups" feature (task-completed in Phase 2) limits the damage but it's still meaningful. Add a "Reranker timeout" setting (e.g. 2000ms) — if it expires, fall back to the unranked retrieval results and log a warning to the RAG log panel.
- **Toggle and default.** Off by default for the first release — opt-in via a "Rerank results" toggle in RAG settings, with the model name configurable. Once stable, consider enabling by default for the cloud reranker path (where latency is sub-second).
- **Provenance in logs.** The "Show RAG Only" log already records vector hits; extend it with each chunk's pre-rerank rank and reranker score so debugging "why did the reranker drop X?" is possible.

This task depends on task-213 (BM25) because reranking shines most when the candidate pool is diverse — a vector-only pool tends to be redundant near the embedding ridge, so reranking 30 vector-only candidates returns diminishing returns. Doing 213 first means 214's evaluation is realistic.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An optional reranker stage runs after retrieval (vector + optional BM25 via RRF) and reorders the shortlist before it reaches `MessageCreationService`; the prompt receives the reranker's top-N (configured `IndexerMaxResults`)
- [ ] #2 Local-first reranking via an Ollama-hosted reranker model is supported and is the default backend; the model name is a setting (default e.g. `bge-reranker` once available, or the chosen equivalent)
- [ ] #3 A separate "Reranker shortlist size" setting controls how many candidates retrieval returns to the reranker (default 30); the existing `IndexerMaxResults` continues to control how many reach the prompt
- [ ] #4 A "Reranker timeout (ms)" setting (default 2000) bounds the reranker call; on timeout, the original retrieval order is used and a `WARN`-level RAG log entry records the fallback
- [ ] #5 A "Rerank results" toggle (default OFF) gates the feature in RAG settings; persisted via `DevoxxGenieStateService`
- [ ] #6 The "Show RAG Only" log records, per final hit, its pre-rerank rank and the reranker's score so chunk-survival/elimination is debuggable
- [ ] #7 A new validator (or extension of the existing chain) checks the reranker model is available in Ollama; failure surfaces in the RAG status with a pull action, matching the existing `nomic-embed-text` pattern
- [ ] #8 Unit tests mock the reranker call and cover: reordering happens, timeout falls back cleanly, top-N truncation is correct, OFF toggle is a no-op
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background from the Phase-1/2 RAG review: reranking was called out as one of the two cheapest, highest-yield retrieval improvements (the other being BM25 hybrid retrieval). Reranking can double answer quality on its own; combined with BM25 it's compounding.

Suggested layering:
- `Reranker` interface: `List<SearchResult> rerank(String query, List<SearchResult> candidates, int topN, long timeoutMs)`.
- `OllamaReranker` impl: POSTs query+candidate pairs to Ollama's embeddings or chat completion endpoint depending on what the chosen reranker model supports. Run in parallel within a small bounded executor (like the Phase 2 indexer parallelism).
- `NoOpReranker` for tests / when the toggle is off.
- Plug into `SemanticSearchService.search` as the last step before returning.

Open question: as of writing, Ollama's official model library may not host a first-class reranker. If not, the practical first implementation may need to use a chat-completion reranker prompt ("Score this code chunk for relevance to the query") which is slower but works with any chat model the user already has. Document this in the task when implementing.

Out of scope: cloud reranker integrations (Phase 4 if demand), per-language reranker selection.

Depends on task-213 because the reranker's value depends on having a diverse candidate pool, which the BM25 retriever ensures.
<!-- SECTION:NOTES:END -->
