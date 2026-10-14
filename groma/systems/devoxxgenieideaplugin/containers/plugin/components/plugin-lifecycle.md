---
type: C4 Component
title: Plugin lifecycle and external API
status: stable
groma:
  id: plugin-lifecycle
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/ExternalPromptService.java
      symbol: ExternalPromptService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/PostStartupActivity.java
      symbol: PostStartupActivity
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/WindowsSkikoRenderApiInitializer.java
      symbol: WindowsSkikoRenderApiInitializer
  group: Settings and platform
description: Startup work and the prompt API for other plugins
---

Per-project post-startup activity that initialises chat memory and the prompt thread pool, registers theme and event-automation listeners, starts the RAG file watcher and shows the analytics consent notice; a Windows Skia render API initializer for Compose, and `ExternalPromptService`, which lets other plugins place prompts in the chat input.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/PostStartupActivity.java](../../../../../../src/main/java/com/devoxx/genie/service/PostStartupActivity.java) | [src/main/java/com/devoxx/genie/service/rag/watcher/RAGFileWatcher.java](../../../../../../src/main/java/com/devoxx/genie/service/rag/watcher/RAGFileWatcher.java) | Starts RAG watcher | Java call |
| [src/main/java/com/devoxx/genie/service/PostStartupActivity.java](../../../../../../src/main/java/com/devoxx/genie/service/PostStartupActivity.java) | [src/main/java/com/devoxx/genie/service/analytics/AnalyticsConsentNotifier.java](../../../../../../src/main/java/com/devoxx/genie/service/analytics/AnalyticsConsentNotifier.java) | Shows consent notice | Java call |
