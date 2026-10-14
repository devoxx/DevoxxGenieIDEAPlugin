---
type: C4 Component
title: Project context collection
status: stable
groma:
  id: project-context
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/AddDirectoryAction.java
      symbol: AddDirectoryAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/AddFileAction.java
      symbol: AddFileAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/AddSnippetAction.java
      symbol: AddSnippetAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/CalcTokensForDirectoryAction.java
      symbol: CalcTokensForDirectoryAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/ExcludeDirectoryAction.java
      symbol: ExcludeDirectoryAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/ScanContentResult.java
      symbol: ScanContentResult
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/FileListManager.java
      symbol: FileListManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/FileListObserver.java
      symbol: FileListObserver
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/ProjectContentService.java
      symbol: ProjectContentService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/projectscanner/ContentExtractor.java
      symbol: ContentExtractor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/projectscanner/DirectoryScannerService.java
      symbol: DirectoryScannerService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/projectscanner/FileScanner.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/projectscanner/ProjectScannerService.java
      symbol: ProjectScannerService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/projectscanner/TokenCalculator.java
      symbol: TokenCalculator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/TokenCalculationService.java
      symbol: TokenCalculationService
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/copyproject/CopyProjectSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/copyproject/CopyProjectSettingsConfigurable.java
      symbol: CopyProjectSettingsConfigurable
  group: Prompt pipeline
description: Gathers files, directories and snippets into the prompt context and counts their tokens
---

Editor and project-view actions add files, directories or selections; `FileListManager` tracks the context files per conversation; the project scanner walks directories honouring exclusions and .gitignore, extracts content and estimates tokens and cost.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/action/AddFileAction.java](../../../../../../src/main/java/com/devoxx/genie/action/AddFileAction.java) | [src/main/java/com/devoxx/genie/service/FileListManager.java](../../../../../../src/main/java/com/devoxx/genie/service/FileListManager.java) | Adds context files | Java call |
