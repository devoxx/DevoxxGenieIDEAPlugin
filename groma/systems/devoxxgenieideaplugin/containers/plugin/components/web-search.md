---
type: C4 Component
title: Web search
status: stable
groma:
  id: web-search
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/WebSearchPromptStrategy.java
      symbol: WebSearchPromptStrategy
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersComponent.java
      symbol: WebSearchProvidersComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurable.java
      symbol: WebSearchProvidersConfigurable
  group: Prompt pipeline
  technology: langchain4j web search
description: Answers prompts grounded in live web results
---

Queries Google Custom Search or Tavily, builds a grounded prompt from the results and runs it as a dedicated prompt strategy. Includes the provider settings page.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionService.java](../../../../../../src/main/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionService.java) | [web-search-apis](../../../../../externals/web-search-apis.md) | Searches the web | HTTPS |
