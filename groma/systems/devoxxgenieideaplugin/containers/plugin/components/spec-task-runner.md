---
type: C4 Component
title: Spec task runner
status: stable
groma:
  id: spec-task-runner
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/CircularDependencyException.java
      symbol: CircularDependencyException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerListener.java
      symbol: SpecTaskRunnerListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/spec/TaskDependencySorter.java
      symbol: TaskDependencySorter
  group: Spec-driven development
description: Executes spec tasks one by one or in parallel dependency layers
---

Sorts tasks by dependencies and submits each as a prompt (LLM, CLI or ACP runner), tracking progress until each task is marked done.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java) | [src/main/java/com/devoxx/genie/service/spec/SpecService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecService.java) | Reads task specs | Java call |
| [src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java) | [src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java](../../../../../../src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java) | Runs tasks via CLI | Java call |
