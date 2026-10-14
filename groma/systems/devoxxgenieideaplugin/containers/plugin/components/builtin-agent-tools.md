---
type: C4 Component
title: Built-in agent tools
status: stable
groma:
  id: builtin-agent-tools
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/agent/TestResult.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/AgentDiffPreviewFactory.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/BuildSystemDetector.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptions.java
      symbol: BuiltInToolDescriptions
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
      symbol: BuiltInToolProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/CompositeToolProvider.java
      symbol: CompositeToolProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/EditFileToolExecutor.java
      symbol: EditFileToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/FetchPageToolExecutor.java
      symbol: FetchPageToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/ListFilesToolExecutor.java
      symbol: ListFilesToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
      symbol: ParallelExploreToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/ReadFileToolExecutor.java
      symbol: ReadFileToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/ReadOnlyToolProvider.java
      symbol: ReadOnlyToolProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/RunCommandToolExecutor.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/RunTestsToolExecutor.java
      symbol: RunTestsToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/SearchFilesToolExecutor.java
      symbol: SearchFilesToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/SecurityScanToolExecutor.java
      symbol: SecurityScanToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/SecurityScanToolSpecification.java
      symbol: SecurityScanToolSpecification
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/SemanticSearchToolExecutor.java
      symbol: SemanticSearchToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/TestResultParser.java
      symbol: TestResultParser
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/ToolArgumentParser.java
      symbol: ToolArgumentParser
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutor.java
      symbol: WebSearchToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/WriteFileToolExecutor.java
      symbol: WriteFileToolExecutor
  group: Agent mode
description: File, search, command, test, web and semantic-search tools for agent mode
---

Tool executors and descriptions for read/write/edit file, list and search files, run command, run tests, fetch page, web search, semantic search, security scan and parallel explore.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java) | [src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java) | Registers PSI tools | Java call |
| [src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java) | [src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java) | Registers Backlog tools | Java call |
| [src/main/java/com/devoxx/genie/service/agent/tool/SemanticSearchToolExecutor.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/SemanticSearchToolExecutor.java) | [src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java](../../../../../../src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java) | Searches semantically | Java call |
| [src/main/java/com/devoxx/genie/service/agent/tool/SecurityScanToolExecutor.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/SecurityScanToolExecutor.java) | [src/main/java/com/devoxx/genie/service/security/SecurityScannerService.java](../../../../../../src/main/java/com/devoxx/genie/service/security/SecurityScannerService.java) | Runs security scans | Java call |
