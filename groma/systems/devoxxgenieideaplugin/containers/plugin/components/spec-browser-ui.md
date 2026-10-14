---
type: C4 Component
title: Spec browser
status: stable
groma:
  id: spec-browser-ui
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/spec/SpecPreviewPanel.java
      symbol: SpecPreviewPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/spec/SpecStatisticsPanel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/spec/SpecTaskRunnerProgressPanel.java
      symbol: SpecTaskRunnerProgressPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/spec/SpecTreeCellRenderer.java
      symbol: SpecTreeCellRenderer
  group: Spec-driven development
  technology: Swing
description: Tree, preview and statistics panels for spec tasks
---

Swing panels to browse tasks by status, preview a task, see statistics and follow the task runner.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java](../../../../../../src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java) | [src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java) | Starts task runs | Java call |
