---
type: C4 Component
title: RAG indexing and semantic search
status: stable
groma:
  id: rag-indexing-search
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/rag/RAGLogMessage.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/ChunkQualityFilter.java
      symbol: ChunkQualityFilter
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/IndexerConstants.java
      symbol: IndexerConstants
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
      symbol: IndexManifest
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifestEntry.java
      symbol: IndexManifestEntry
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifestService.java
      symbol: IndexManifestService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/manifest/InMemoryIndexManifest.java
      symbol: InMemoryIndexManifest
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/manifest/JsonFileIndexManifest.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/QueryExpansionFuser.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/RAGEventPublisher.java
      symbol: RAGEventPublisher
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/RAGLoggingMessage.java
      symbol: RAGLoggingMessage
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
      symbol: RagValidatorService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/SearchResult.java
      symbol: SearchResult
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
      symbol: SemanticSearchService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/watcher/RAGFileWatcher.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/rag/ActionButtonState.java
      symbol: ActionButtonState
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsConfigurable.java
      symbol: RAGSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/rag/table/ButtonEditor.java
      symbol: ButtonEditor
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/rag/table/ButtonRenderer.java
      symbol: ButtonRenderer
  group: RAG
  technology: langchain4j, ChromaDB, Ollama embeddings
description: Indexes project files into ChromaDB and retrieves relevant chunks
---

Splits and embeds project files with Ollama nomic-embed-text, stores them in a per-project Chroma collection, keeps an index manifest for incremental re-indexing and watches files for changes. Semantic search expands queries and fuses results; the hits feed either passive prompt context or the agent `semantic_search` tool. Includes the RAG settings page.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java](../../../../../../src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java) | [src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java](../../../../../../src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java) | Stores embeddings | Java call |
| [src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java](../../../../../../src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java) | [src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java](../../../../../../src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java) | Validates prerequisites | Java call |
| [src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java](../../../../../../src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java) | [src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java](../../../../../../src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java) | Searches embeddings | langchain4j EmbeddingStore |
