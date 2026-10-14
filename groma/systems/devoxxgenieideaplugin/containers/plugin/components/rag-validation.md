---
type: C4 Component
title: RAG prerequisite checks
status: stable
groma:
  id: rag-validation
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/ChromeDBValidator.java
      symbol: ChromeDBValidator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/DockerValidator.java
      symbol: DockerValidator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/NomicEmbedTextValidator.java
      symbol: NomicEmbedTextValidator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/OllamaValidator.java
      symbol: OllamaValidator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/ValidationActionType.java
      symbol: ValidationActionType
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/ValidationResult.java
      symbol: ValidationResult
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/Validator.java
      symbol: Validator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/ValidatorStatus.java
      symbol: ValidatorStatus
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/rag/validator/ValidatorType.java
      symbol: ValidatorType
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ValidatorsPanel.java
      symbol: ValidatorsPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ValidatorStatusPanel.java
      symbol: ValidatorStatusPanel
  group: RAG
description: Verifies Docker, ChromaDB, Ollama and the embedding model before RAG is enabled
---

Validators with status and fix-up actions, shown in the RAG settings and status panels.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/rag/validator/ChromeDBValidator.java](../../../../../../src/main/java/com/devoxx/genie/service/rag/validator/ChromeDBValidator.java) | [src/main/java/com/devoxx/genie/service/chromadb/ChromaDockerService.java](../../../../../../src/main/java/com/devoxx/genie/service/chromadb/ChromaDockerService.java) | Checks Chroma container | Java call |
