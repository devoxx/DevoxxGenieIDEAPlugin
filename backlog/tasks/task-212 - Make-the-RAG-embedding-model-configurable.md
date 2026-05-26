---
id: TASK-212
title: Make the RAG embedding model configurable (default to a code-tuned model)
status: To Do
assignee: []
created_date: '2026-05-26 18:00'
updated_date: '2026-05-26 18:00'
labels:
  - enhancement
  - RAG
  - settings
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/rag/IndexerConstants.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
  - src/main/java/com/devoxx/genie/service/rag/validator/NomicEmbedTextValidator.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - 'https://ollama.com/library/nomic-embed-text'
  - 'https://huggingface.co/nomic-ai/nomic-embed-code'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The embedding model is hardcoded to `nomic-embed-text` in `ChromaEmbeddingService.EMBEDDING_MODEL_NAME`. That model is a strong generic embedder, but code-tuned alternatives (e.g. `nomic-embed-code`, `jina-embeddings-v2-base-code`, `bge-code-v1`) consistently outperform it on code retrieval benchmarks. Power users on capable hardware should be able to opt in.

This task adds a single setting — the Ollama model name used for RAG embeddings — and propagates it through the indexing, retrieval, and validator paths. It must coexist cleanly with the v2 schema written by the post-Phase-1 indexer: changing the model invalidates every existing embedding, so the change must trigger (or prompt for) a full re-index.

**Investigation areas:**
- **Settings UI.** Add a "RAG embedding model" text field (or combo with curated suggestions) to `RAGSettingsComponent`, persisted on `DevoxxGenieStateService`. Default = `nomic-embed-text` so existing users see no behavior change.
- **Schema-version coupling.** Different models produce different-dimensional, incompatible embeddings. Either bump `IndexerConstants.CURRENT_EMBEDDING_SCHEMA_VERSION` whenever the model changes (e.g. compose it with the model name), or record the model name on every manifest entry and refuse to use stored chunks that were produced by a different model. The manifest already carries `schemaVersion`; the cheapest fix is to make the "schema" include the model name.
- **Validator update.** `NomicEmbedTextValidator` currently hardcodes the prefix `nomic-embed-text`. Generalize it to check whichever model the user configured, and update its messaging and the "pull this model" action (`ValidationActionType.PULL_NOMIC`) accordingly. Consider renaming the validator to something model-agnostic.
- **Re-index UX.** On model change: show a notification telling the user the existing index is now stale and offer a one-click "Re-index now" action. Do NOT silently start a multi-minute embed job.
- **Documentation.** Mention 1-2 recommended code-tuned models in the docusaurus RAG page and link to their Ollama tags.

This is a small but high-leverage knob: it unlocks a quality dimension the Phase 1+2 work cannot reach on its own.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A "RAG embedding model" setting (Ollama model name) is exposed in the RAG settings panel and persisted via `DevoxxGenieStateService`; default is `nomic-embed-text` so existing installs see no change
- [ ] #2 `ChromaEmbeddingService.getEmbeddingModel()` reads the configured model name (no more hardcoded constant); the cached model is rebuilt when the model name changes, mirroring the existing URL-change behaviour
- [ ] #3 The embedding schema is bound to the model so that mixing models is impossible: either `CURRENT_EMBEDDING_SCHEMA_VERSION` includes the model name, or every manifest entry stores the model name and `isCurrent` rejects entries written by a different model
- [ ] #4 Changing the model in settings notifies the user that the existing index is stale and offers a one-click "Re-index now" action; no automatic re-index happens silently
- [ ] #5 The Ollama model validator is generalized to verify whatever model the user configured (not only the `nomic-embed-text` prefix) and its action/messaging reflects the configured name
- [ ] #6 Unit tests cover: settings round-trip, the model-change-invalidates-manifest behaviour, and the validator working for a non-`nomic` model name
- [ ] #7 The docusaurus RAG page mentions at least one recommended code-tuned alternative and how to pull it via Ollama
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background from the Phase-1/2 RAG review: the embedding model was identified as a low-effort, high-yield quality knob. Phase 1 fixed correctness (embed content not paths) and Phase 2 added caching/batching/parallelism/manifest, but the embedding model itself stayed pinned. Code-tuned models on the same Ollama runtime tend to lift retrieval recall meaningfully on code queries.

Implementation sequence suggestion:
1. Add `getEmbeddingModelName()` / `setEmbeddingModelName()` to `DevoxxGenieStateService` (default `"nomic-embed-text"`).
2. Replace `ChromaEmbeddingService.EMBEDDING_MODEL_NAME` reads with the state lookup; keep the cache invalidation that already watches URL changes and add the model name to its key.
3. Decide schema-binding strategy. Two equivalent options:
   - Concatenate model name into the schema constant: `CURRENT_EMBEDDING_SCHEMA_VERSION = "v2:" + modelName` (computed at access time).
   - Add `embeddingModel` to `IndexManifestEntry` and reject mismatches in `isCurrent`.
   The latter is more explicit and shows up in the JSON sidecar — preferred.
4. Wire a settings-change listener that fires the "index stale" notification with a Re-index action.
5. Generalize the Nomic validator (rename to `OllamaEmbeddingModelValidator`?). Keep `PULL_NOMIC` action type if other code already references it, or rename the enum value.

Out of scope: cloud-hosted embedders (OpenAI / Voyage / etc.) — Phase 4 if there's demand.
<!-- SECTION:NOTES:END -->
