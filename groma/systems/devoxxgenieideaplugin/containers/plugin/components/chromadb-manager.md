---
type: C4 Component
title: ChromaDB container manager
status: stable
groma:
  id: chromadb-manager
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/ChromaDBManager.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/ChromaDBService.java
      symbol: ChromaDBService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/ChromaDBStatusCallback.java
      symbol: ChromaDBStatusCallback
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/ChromaDockerService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
      symbol: ChromaEmbeddingService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/exception/ChromaDBException.java
      symbol: ChromaDBException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/exception/DockerException.java
      symbol: DockerException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/chromadb/model/ChromaCollection.java
      symbol: ChromaCollection
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/DockerUtil.java
      symbol: DockerUtil
  group: RAG
  technology: docker-java, Chroma HTTP API
description: Pulls, starts and talks to the local ChromaDB container
---

Uses Docker to pull and run the ChromaDB image and wraps the Chroma HTTP API for collections and embeddings.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java](../../../../../../src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java) | [local-llm-runtimes](../../../../../externals/local-llm-runtimes.md) | Embeds chunks | Ollama nomic-embed-text |
| [src/main/java/com/devoxx/genie/service/chromadb/ChromaDockerService.java](../../../../../../src/main/java/com/devoxx/genie/service/chromadb/ChromaDockerService.java) | [docker](../../../../../externals/docker.md) | Runs Chroma container | docker-java |
| [src/main/java/com/devoxx/genie/service/chromadb/ChromaDBService.java](../../../../../../src/main/java/com/devoxx/genie/service/chromadb/ChromaDBService.java) | [chromadb](../../../../../externals/chromadb.md) | Reads and writes vectors | Chroma HTTP API |
