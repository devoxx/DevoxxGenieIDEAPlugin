---
id: TASK-212
title: Make the RAG embedding model configurable
status: Done
assignee:
  - '@claude'
created_date: 2026-05-26 18:00
updated_date: '2026-11-04T17:07:00Z'
labels:
  - enhancement
  - RAG
  - settings
milestone: m-0
dependencies:
  - TASK-269
references:
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/rag/IndexerConstants.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/validator/NomicEmbedTextValidator.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - https://ollama.com/library/nomic-embed-text
  - https://huggingface.co/nomic-ai/nomic-embed-code
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/523
modified_files:
  - >-
    src/test/java/com/devoxx/genie/service/rag/embedding/EmbeddingFingerprintTest.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingFingerprint.java
  - src/test/java/com/devoxx/genie/service/rag/manifest/IndexManifestTest.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifestEntry.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/manifest/InMemoryIndexManifest.java
  - >-
    src/test/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingServiceModelTest.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/rag/IndexerConstants.java
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - src/main/java/com/devoxx/genie/service/rag/RAGEventPublisher.java
  - src/test/java/com/devoxx/genie/service/rag/SemanticSearchServiceTest.java
  - src/test/java/com/devoxx/genie/service/rag/ChunkingCharacterizationTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/ProjectIndexerModelChangeTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/validator/NomicEmbedTextValidatorTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/validator/EmbeddingModelValidatorTest.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/validator/NomicEmbedTextValidator.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/validator/EmbeddingModelValidator.java
  - src/main/java/com/devoxx/genie/service/rag/validator/ValidatorType.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/validator/ValidationActionType.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
  - src/main/java/com/devoxx/genie/ui/panel/ValidatorStatusPanel.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/RagIndexStaleNotifierTest.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RagIndexStaleNotifier.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsConfigurable.java
  - src/test/java/com/devoxx/genie/ui/settings/RagEmbeddingModelSettingTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/EmbeddingModelComboBoxTest.java
  - src/main/java/com/devoxx/genie/service/rag/cli/RagCli.java
  - src/main/java/com/devoxx/genie/service/rag/cli/RagBenchmarkCommand.java
  - docusaurus/docs/features/rag.md
  - >-
    src/test/java/com/devoxx/genie/chatmodel/local/ollama/OllamaModelServicePullTest.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaModelService.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/embedding/EmbeddingModelInstallerTest.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingModelInstaller.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/rag/EmbeddingModelDownloadPrompt.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/StartIndexingAvailabilityTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/RagSearchResultsPanelTest.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RagSearchResultsPanel.java
  - src/main/java/com/devoxx/genie/ui/panel/ValidatorsPanel.java
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
- [x] #1 A "RAG embedding model" setting (Ollama model name) is exposed in the RAG settings panel and persisted via `DevoxxGenieStateService`; default is `nomic-embed-text` so existing installs see no change
- [x] #2 `ChromaEmbeddingService.getEmbeddingModel()` reads the configured model name (no more hardcoded constant); the cached model is rebuilt when the model name changes, mirroring the existing URL-change behaviour
- [x] #3 The embedding schema is bound to the model so that mixing models is impossible: either `CURRENT_EMBEDDING_SCHEMA_VERSION` includes the model name, or every manifest entry stores the model name and `isCurrent` rejects entries written by a different model
- [x] #4 Changing the model in settings notifies the user that the existing index is stale and offers a one-click "Re-index now" action; no automatic re-index happens silently
- [x] #5 The Ollama model validator is generalized to verify whatever model the user configured (not only the `nomic-embed-text` prefix) and its action/messaging reflects the configured name
- [x] #6 Unit tests cover: settings round-trip, the model-change-invalidates-manifest behaviour, and the validator working for a non-`nomic` model name
- [x] #7 The docusaurus RAG page mentions at least one measured alternative to `nomic-embed-text`, with its benchmark results, and how to pull it via Ollama
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Findings (2026-11-03)
- The model name is hardcoded in 4 places: `ChromaEmbeddingService.EMBEDDING_MODEL_NAME`, `RagCli.DEFAULT_EMBED_MODEL`, `RagBenchmarkCommand.DEFAULT_MODEL`, and the pull call in `RAGSettingsHandler.pullNomicModel`. `RAGEventPublisher` logs the constant.
- `ChromaEmbeddingService.getEmbeddingModel()` already caches by (URL, model name); it only needs to read the name from state.
- One Chroma collection per project (named after the project). A Chroma collection's vector dimension is fixed by its first add. "Start Indexing" calls `indexFiles(force=true)`, which never removes existing chunks. So after a model change, a re-index would either fail (different dimension) or mix two models' vectors (same dimension, e.g. nomic → nomic-embed-text:v1.5).
- `ValidatorStatusPanel` shows the lower-cased `ValidatorType` name as the row label.

## Plan
1. **One default.** Add an IDE-free `RagEmbeddingDefaults.MODEL_NAME = "nomic-embed-text"`. State default, `RagCli`, `RagBenchmarkCommand` and the validator all use it.
2. **Setting.** Add `DevoxxGenieStateService.ragEmbeddingModelName`, with blank meaning the default. In `RAGSettingsComponent`, add an editable combo with suggestions (nomic-embed-text, bge-m3, …) that accepts any name; this avoids the trap where `setSelectedItem` drops unknown items. Update `RAGSettingsConfigurable` `isModified`/`apply`/`reset`. No network call on the EDT.
3. **Model.** `ChromaEmbeddingService.getEmbeddingModel()` reads the configured name; the cache is rebuilt when the name changes. `RAGEventPublisher` logs the model that was actually used.
4. **Fingerprint.** Add IDE-free `EmbeddingFingerprint` (`ollama:<model>` for now; TASK-265 adds provider, dimension and document prefix). Add a nullable `embeddingFingerprint` field to `IndexManifestEntry`. `schemaVersion` stays `v2`, as the storage-format version. `InMemoryIndexManifest` takes a `Supplier<String>` for the current fingerprint, wired by `IndexManifestService`. `isCurrent` requires both the schema and the fingerprint to match. Legacy entries with no fingerprint count as the default model, so existing installs don't re-index (AC #1).
5. **Rebuild on change.** A full `indexFiles` whose configured fingerprint differs from the fingerprint the project's index was built with first drops the project's Chroma collection and clears the manifest, then indexes everything. This covers both "Start Indexing" and the notification action.
6. **Between change and re-index (decision).** See notes.
7. **Notification.** In `RAGSettingsConfigurable.apply()`, when the model changed and the project has an index: a balloon saying the index was built with X and needs a re-index for Y, with a "Re-index now" action (Task.Backgroundable). Nothing starts automatically.
8. **Validator.** Rename `NomicEmbedTextValidator` → `EmbeddingModelValidator`, `ValidatorType.NOMIC` → `EMBEDDING_MODEL` with a display label, and `PULL_NOMIC` → `PULL_EMBEDDING_MODEL`. It checks the configured model: an exact name or `name:<tag>`, with `name` matching `name:latest`. The pull action and its messages use the configured name.
9. **Tests.** Settings round-trip; fingerprint mismatch invalidates the manifest, and a legacy null fingerprint counts as default; full index after a model change drops the collection first; the validator with a non-nomic model (bge-m3); `getEmbeddingModel` rebuilds on a name change.
10. **Docs.** Add an embedding-model section to `docusaurus/docs/features/rag.md` recommending a measured alternative (bge-m3 benchmark running), with `ollama pull` and the re-index note.

Benchmark note: the benchmark builds its own Ollama client, so it can't detect a plugin-side regression. Unit tests cover "the default config is unchanged".

## Deviations from the plan
- Step 1: the single default lives on `EmbeddingFingerprint.DEFAULT_MODEL_NAME` instead of a separate `RagEmbeddingDefaults` class, since the fingerprint already needs it for legacy entries.
- Step 4: `isCurrent`/`markIndexed` take the fingerprint as an explicit argument instead of the manifest reading it from a `Supplier`. Which fingerprint is correct depends on the caller: the configured one for a full index, the index's own for the watcher and search (decision in step 6).
- Review fix: `prepareIndexFor` runs only after the project base path checks in `indexFiles`, so an aborted run can't empty the index without rebuilding it.
- Pre-existing duplicate-chunks bug on same-model re-index split out as TASK-270.
<!-- SECTION:PLAN:END -->

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

2026-11-03: GitHub #523 brings OpenAI-compatible embedding endpoints into scope as TASK-265, which depends on this task. Design the model-binding fingerprint here so it can be extended to cover provider, output dimension and document prefix, not just the Ollama model name.

2026-11-03: Now depends on TASK-269 (retrieval benchmark). Use the benchmark to pick and justify any recommended code-tuned model in the docs, and to check that making the model configurable doesn't change retrieval for the default nomic-embed-text setup.

2026-11-03 decision (maintainer): between a model change and the re-index, search and incremental re-indexing keep using the model the index was built with (the index fingerprint). Only a full index uses the configured model. This is the same rule TASK-265 needs for its prefix rollout ("search keeps working until re-index").

Implementation (2026-11-03):
- `EmbeddingFingerprint` (service/rag/embedding, IDE-free): `ollama:<model>`, `:latest` normalised away, null/blank = legacy default `ollama:nomic-embed-text`; holds the single default constant used by state, RagCli and RagBenchmarkCommand.
- Manifest: `IndexManifestEntry.embeddingFingerprint` (nullable; pre-212 JSON loads as nomic, so no forced re-index on upgrade). `isCurrent(file, fingerprint)` / `markIndexed(file, n, fingerprint)` take it explicitly instead of a hidden supplier: the right fingerprint depends on the caller (full index = configured, watcher/search = index's). Added `clear()` and `indexFingerprint()`. `schemaVersion` stays `v2` (storage format).
- `ProjectIndexerService.prepareIndexFor`: a full `indexFiles` whose configured fingerprint differs from the index's calls `EmbeddingStore.removeAll()` (langchain4j Chroma deletes + recreates the collection, which also resets Chroma's fixed vector dimension), clears the manifest, then indexes. If removeAll fails it aborts instead of mixing models. `reindexFiles`/`indexFile` use the index fingerprint. Chunks also carry an informational `embeddingFingerprint` metadata key.
- `ChromaEmbeddingService`: `configuredFingerprint()`, `indexFingerprint(project)`, `getEmbeddingModel(fingerprint)` (clients cached per URL+fingerprint, so old and new model can be live together); `EMBEDDING_MODEL_NAME` constant removed. `SemanticSearchService` embeds queries with the index's model; `RAGEventPublisher` logs it.
- Validator renamed `NomicEmbedTextValidator` → `EmbeddingModelValidator` (`ValidatorType.EMBEDDING_MODEL` with label "embedding model", `PULL_EMBEDDING_MODEL`). Exact name match with `:latest` normalisation; behaviour change: `nomic-embed-text-v1.5`-style prefix matches no longer pass. Action now resets to OK on revalidation (was sticky). Pull uses the configured model.
- Settings: editable `ComboBox` with suggestions (nomic-embed-text, bge-m3, mxbai-embed-large, embeddinggemma), any name accepted; isModified/apply/reset compare normalised names. `RagIndexStaleNotifier` checks the manifest off the EDT and shows a balloon with "Re-index now" (Task.Backgroundable) only when the index was built with another model; switching back to the index's model shows nothing.

Benchmark, same corpus (1438 files, branch base 601cd373): nomic-embed-text recall@1 0.143 / @5 0.643 / @10 0.714 / MRR 0.351, index 150 s; bge-m3 0.250 / 0.500 / 0.857 / 0.380, index 392 s. Mixed result; bge-m3 is not code-tuned. Docs present it as a measured alternative, not a code-tuned recommendation. AC #7 left open for the maintainer (see below).

Verification: full suite 3918 tests, 22 skipped, 0 failures; verifyPlugin Compatible against IU-253.31033.145 and IU-261.22158.277.

Pre-existing, not fixed here: "Start Indexing" calls indexFiles(force=true) without removing a file's old chunks first, so re-indexing with the same model duplicates chunks. Worth its own task.

2026-11-04, after trying the build. Selecting embeddinggemma when it wasn't installed wiped the project's 22,427-segment index: every chunk failed to embed, and the manifest still recorded 18,626 segments. Added at the maintainer's request: before indexing, a missing model prompts for an Ollama download (EmbeddingModelInstaller, EmbeddingModelDownloadPrompt), and Start Indexing stays visible when the model is the only thing missing. prepareIndexFor test-embeds first, so a model that can't embed (for example a chat model) leaves the index intact. pullModel now fails on Ollama's HTTP 200 error lines, and its progress text no longer depends on locale.

2026-11-04, RAG settings panel. Required RAG Services now sits above Test semantic search; removed the carried-over GridBag weight and the fixed validation height that caused the gap. Test results are now a match list plus a preview (RagSearchResultsPanel). Removed nested scroll panes (validators) and replaced plain JScrollPane with JBScrollPane. Dropdown suggestions extended from ollama.com's embedding list; laya left out because its description reads as a decision model.

Known gap, not fixed: storeSegments still logs and skips chunks that fail mid-run, and the manifest records the chunk count rather than what was stored. The test embed catches a model that can't embed at all, not failures partway through a run. RAG settings validation still runs on the EDT and freezes the panel after opening or Apply; that's TASK-267.

2026-11-04: Maintainer approved rewording AC #7 from a recommended code-tuned alternative to a measured alternative, and dropping "(default to a code-tuned model)" from the title. The default stays nomic-embed-text; bge-m3 is the measured alternative and is not code-tuned. No code-tuned model has been benchmarked yet.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The RAG embedding model is now a setting: an editable dropdown of Ollama embedding models in the RAG settings, defaulting to `nomic-embed-text`.

- Each index is bound to the model that built it through an embedding fingerprint on the manifest. Search and the file watcher keep using the index's model; only a full index uses the configured one, and it test-embeds before wiping the old index.
- Changing the model offers a Re-index now notification and never re-indexes silently.
- A missing model prompts for an Ollama download before indexing, and `pullModel` now fails on Ollama's HTTP 200 error lines.
- The validator checks whichever model is configured.
- RAG settings panel: Required RAG Services sits above Test semantic search, the vertical gap and nested scroll panes are gone, and test results are a match list with a preview.

Benchmark on this repository (1,438 files, 28 questions, ranked first / top 5 / top 10): `nomic-embed-text` 14% / 64% / 71%, `bge-m3` 25% / 50% / 86%. `bge-m3` isn't code-tuned, so AC #7 was reworded to a measured alternative with the maintainer's approval. No code-tuned model has been benchmarked.

Verified: 3945 tests, 0 failures, 22 skipped. Known gaps: chunks that fail partway through a run are still counted as indexed (fixed in TASK-265), and settings validation still runs on the EDT (TASK-267).
<!-- SECTION:FINAL_SUMMARY:END -->
