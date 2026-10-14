---
type: C4 Component
title: Backlog agent tools
status: stable
groma:
  id: backlog-agent-tools
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/BacklogDocumentToolExecutor.java
      symbol: BacklogDocumentToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/BacklogMilestoneToolExecutor.java
      symbol: BacklogMilestoneToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java
      symbol: BacklogTaskToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/BacklogToolSpecifications.java
      symbol: BacklogToolSpecifications
  group: Agent mode
description: Agent tools to manage Backlog.md tasks, documents and milestones
---

Tool specifications and executors that let the LLM create, edit, search and complete Backlog.md items through the spec service.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java) | [src/main/java/com/devoxx/genie/service/spec/SpecService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecService.java) | Manages Backlog tasks | Java call |
