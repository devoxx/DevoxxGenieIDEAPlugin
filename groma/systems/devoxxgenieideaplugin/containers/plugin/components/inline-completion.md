---
type: C4 Component
title: Inline code completion
status: stable
groma:
  id: inline-completion
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/CompletionCache.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/CompletionPostProcessor.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/EditorContextExtractor.java
      symbol: EditorContextExtractor
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/FimProvider.java
      symbol: FimProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/FimRequest.java
      symbol: FimRequest
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/FimResponse.java
      symbol: FimResponse
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/InlineCompletionService.java
      symbol: InlineCompletionService
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/LMStudioFimProvider.java
      symbol: LMStudioFimProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/completion/OllamaFimProvider.java
      symbol: OllamaFimProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/completion/CompletionSettingsComponent.java
      symbol: CompletionSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/completion/CompletionSettingsConfigurable.java
      symbol: CompletionSettingsConfigurable
  technology: IntelliJ inline completion API
description: Fill-in-the-middle completions in the editor
---

Implements the IntelliJ inline completion provider (Kotlin entry point, not scanned) through `InlineCompletionService`, which extracts editor context, calls Ollama or LM Studio FIM endpoints, post-processes and caches suggestions. Includes its settings page.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/completion/InlineCompletionService.java](../../../../../../src/main/java/com/devoxx/genie/completion/InlineCompletionService.java) | [src/main/java/com/devoxx/genie/completion/OllamaFimProvider.java](../../../../../../src/main/java/com/devoxx/genie/completion/OllamaFimProvider.java) | Requests completions | Java call |
| [src/main/java/com/devoxx/genie/completion/OllamaFimProvider.java](../../../../../../src/main/java/com/devoxx/genie/completion/OllamaFimProvider.java) | [local-llm-runtimes](../../../../../externals/local-llm-runtimes.md) | Requests FIM completions | HTTP |
