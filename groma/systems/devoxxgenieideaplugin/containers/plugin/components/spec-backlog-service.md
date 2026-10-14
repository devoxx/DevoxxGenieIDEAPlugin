---
type: C4 Component
title: Spec and Backlog service
status: stable
groma:
  id: spec-backlog-service
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/spec/AcceptanceCriterion.java
      symbol: AcceptanceCriterion
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/spec/BacklogConfig.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/spec/BacklogDocument.java
      symbol: BacklogDocument
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/spec/DefinitionOfDoneItem.java
      symbol: DefinitionOfDoneItem
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/spec/TaskSpec.java
      symbol: TaskSpec
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/ExternalTaskService.java
      symbol: ExternalTaskService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/FuzzySearchHelper.java
      symbol: FuzzySearchHelper
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java
      symbol: SpecContextBuilder
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterGenerator.java
      symbol: SpecFrontmatterGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java
      symbol: SpecFrontmatterParser
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/SpecService.java
  group: Spec-driven development
  technology: Markdown + YAML frontmatter
description: Reads and writes Backlog.md tasks and documents in the project
---

Parses task Markdown with frontmatter, keeps an in-memory cache, builds task context for prompts and creates or updates tasks, documents and milestones. Also the entry point other plugins use to create tasks.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/ExternalTaskService.java](../../../../../../src/main/java/com/devoxx/genie/service/ExternalTaskService.java) | [src/main/java/com/devoxx/genie/service/spec/SpecService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecService.java) | Creates tasks | Java call |
