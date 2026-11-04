---
id: TASK-265
title: >-
  Support OpenAI-compatible embedding endpoints with query/document prefixes and
  dimension truncation for RAG
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-03T13:24:00Z'
updated_date: '2026-11-04T14:30:00Z'
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
modified_files:
  - docusaurus/docs/features/rag.md
  - src/main/java/com/devoxx/genie/service/PostStartupActivity.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingFingerprint.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingModelInstaller.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/manifest/InMemoryIndexManifest.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifestEntry.java
  - src/main/java/com/devoxx/genie/service/rag/validator/ValidatorType.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/rag/EmbeddingModelDownloadPrompt.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsConfigurable.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RagIndexStaleNotifier.java
  - >-
    src/test/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingServiceModelTest.java
  - src/test/java/com/devoxx/genie/service/rag/ChunkingCharacterizationTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/ProjectIndexerModelChangeTest.java
  - src/test/java/com/devoxx/genie/service/rag/SemanticSearchServiceTest.java
  - src/test/java/com/devoxx/genie/service/rag/cli/RagBenchmarkCommand.java
  - src/test/java/com/devoxx/genie/service/rag/cli/RagCli.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/embedding/EmbeddingFingerprintTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/embedding/EmbeddingModelInstallerTest.java
  - src/test/java/com/devoxx/genie/service/rag/manifest/IndexManifestTest.java
  - src/test/java/com/devoxx/genie/ui/settings/RagEmbeddingModelSettingTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/RagIndexStaleNotifierTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/StartIndexingAvailabilityTest.java
  - src/test/resources/rag-benchmark/README.md
  - docusaurus/blog/2026-11-04-rag-embedding-providers.md
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingModelFactory.java
  - src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingPrefixes.java
  - src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingProviderSettings.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/validator/EmbeddingProviderValidator.java
  - src/test/java/com/devoxx/genie/service/rag/RagValidatorChainTest.java
  - src/test/java/com/devoxx/genie/service/rag/embedding/EmbeddingSetupTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/validator/EmbeddingProviderValidatorTest.java
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
- [x] #1 RAG settings offer an Embedding provider dropdown above the model field, listing the LLM providers that are enabled and support embeddings: Ollama (default, existing behaviour), Custom OpenAI, OpenAI, LM Studio and llama.cpp
- [x] #2 The selected provider's existing URL and credential from the LLM settings are used; RAG adds no URL or API key fields of its own, and indexing and semantic search both use that provider
- [x] #3 Users can configure a query prefix and a document prefix; the query prefix is applied to every search query embedding and the document prefix to every indexed chunk embedding (batch and per-segment fallback paths alike)
- [x] #4 Prefixes never appear in stored segment text, in the injected <SemanticContext> block, or in semantic_search tool output
- [x] #5 When nomic-embed-text is the selected model, the prefix fields default to its documented `search_query: ` / `search_document: ` prefixes; other models default to empty
- [x] #6 An optional output dimension can be set; embeddings are truncated to that size and re-normalised to unit length for both indexing and querying, independent of whether the backend honours a dimensions parameter
- [x] #7 A configured dimension larger than the model's native dimension is reported as an error rather than silently ignored or padded
- [x] #8 The embedding fingerprint from TASK-212 covers provider, model, output dimension and document prefix: changing any of them marks the index stale and offers re-indexing; changing only the query prefix does not
- [x] #9 The Ollama and embedding-model validators apply only when Ollama is selected; every other provider gets a provider-appropriate reachability check, and a selected provider that has been disabled in the LLM settings is reported
- [x] #10 RagCli can target an OpenAI-compatible endpoint via environment variables (URL, model, key, prefixes, dimension)
- [x] #11 Unit tests cover: prefix application on query vs document paths, prefixes absent from stored text, truncation + re-normalisation, fingerprint change detection per setting, and provider selection in the validator chain
- [x] #12 The docusaurus RAG page documents the provider choice with a llama.cpp example, prefixes, and truncation
- [x] #13 Existing installs that upgrade with nomic-embed-text also switch to the `search_query: ` / `search_document: ` defaults; their existing index is detected as stale and the user is told once, with a one-click "Re-index now" action. No re-index starts automatically, and the notification is not repeated after the user acts on or dismisses it
- [x] #14 Until the user re-indexes, semantic search for an upgraded install keeps working against the existing index (using the settings that built it) instead of failing or returning empty results
- [x] #15 The TASK-269 benchmark can also target an OpenAI-compatible endpoint, and the plugin's embedding path reuses the prefix/truncation component built in TASK-269 rather than a second implementation
- [x] #16 Selecting a cloud provider shows a warning that project source code is sent to that provider during indexing and search
- [x] #17 For a provider without model download support, a missing or unusable model produces a clear error and leaves the existing index untouched
- [ ] #18 A blog post announces the new embedding features, with screenshots of the new RAG settings panel
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Fingerprint first (provider, model, dimension, document prefix; old ids still parse; LEGACY vs new default). One embedding path through ProfiledEmbedder for indexing, search and the pre-wipe test embed. EmbeddingModelFactory builds OpenAI-style clients from each provider's existing URL and key. Settings UI: provider, warning, prefixes, dimension. Validators chosen per provider. One-time startup notice per project. CLI and benchmark through environment variables. Docs and a draft blog post.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-03 decision (maintainer): nomic prefix rollout uses option 1, prefixes on by default for everyone including existing installs, with a one-time re-index prompt. Rejected alternatives: grandfathering existing installs (two groups of users with different quality), query-prefix-only for old indexes (off-spec for nomic, may hurt retrieval), background rebuild into a second collection (too complex for now; conflicts with the 'no silent multi-minute embed job' rule from TASK-212), and mixing old/new prefixed vectors in one collection (scores can't be compared). The retrieval benchmark task in this milestone can quantify the gain for the upgrade notification text.

2026-11-04 decision (maintainer): reuse the providers already configured in the LLM settings instead of new RAG-specific endpoint fields. First group: Ollama, Custom OpenAI, OpenAI, LM Studio, llama.cpp. Mistral, Gemini, Azure and Bedrock need their own clients and come later. Model downloads for providers other than Ollama are a future optional enhancement, not part of this task.

Implementation notes (2026-11-04). The query prefix is stored per manifest entry rather than in the fingerprint: changing it mustn't mark the index stale, but a stale index must keep using the prefix it was built with. Ollama is always selectable, because RAG has always embedded through it even with Ollama hidden from the chat provider list. The reachability check counts any HTTP response and doesn't send the API key, to avoid a keychain dialog on the EDT. llama.cpp gets /v1 added, since its default URL has none.

Benchmark, 28 questions, 1458 files. nomic without prefixes recall@1 0.143, @5 0.643, @10 0.714, MRR 0.351. With prefixes 0.250, 0.536, 0.821, 0.407: recall@5 drops, and docs and blog say so. Same config through the OpenAI path (Ollama's /v1) gave identical scores, which verifies that path against a real server. Prefixes plus 256 dimensions 0.214, 0.571, 0.607, 0.371. Rough check of the minimum score: 15 of 28 answer files reach 0.7 with prefixes, 6 of 28 without.

Verification: full suite 3986 tests, 22 skipped, 0 failures; verifyPlugin Compatible with IU-253 and IU-261; docs build passes. Not verified: the settings UI and dialogs have not been exercised in a running IDE, and LM Studio, llama.cpp and OpenAI have not been run against real servers. #18 is open: the blog post is a draft waiting for screenshots.

Final verification: full suite 3987 tests, 22 skipped, 0 failures; verifyPlugin Compatible with IU-253 and IU-261. All SemanticSearchService.search callers run off the EDT, so reading the API key there is safe.

Known gap, not fixed: with a prefix-only change, the file watcher records the new query prefix on touched files while other entries keep the old one. Search is fine while the fingerprint matches, but if the model then changes without a re-index, queryPrefix(project) returns whichever entry comes first. Fix by storing the prefix once per manifest.
<!-- SECTION:NOTES:END -->
