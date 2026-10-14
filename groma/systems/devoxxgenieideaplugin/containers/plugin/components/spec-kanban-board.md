---
type: C4 Component
title: Spec Kanban board
status: stable
groma:
  id: spec-kanban-board
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/spec/SpecKanbanPanel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/webview/handler/WebViewJavaScriptExecutor.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/webview/JCEFChecker.java
      symbol: JCEFChecker
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/webview/template/HtmlTemplate.java
      symbol: HtmlTemplate
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/webview/template/KanbanTemplate.java
      symbol: KanbanTemplate
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/webview/template/ResourceLoader.java
      symbol: ResourceLoader
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/webview/WebServer.java
      symbol: WebServer
    - scanner: javascript
      file: src/main/resources/webview/js/kanban.js
  group: Spec-driven development
  technology: JCEF, JavaScript
description: Drag-and-drop Kanban board for spec tasks
---

An embedded JCEF browser loads a Kanban page (HTML/CSS/JS from `resources/webview`) served by a local web server; JavaScript callbacks move tasks between statuses. This is the only remaining JCEF view.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/ui/panel/spec/SpecKanbanPanel.java](../../../../../../src/main/java/com/devoxx/genie/ui/panel/spec/SpecKanbanPanel.java) | [src/main/java/com/devoxx/genie/service/spec/SpecService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecService.java) | Moves tasks | Java call |
