---
type: C4 Component
title: Legacy Swing markdown rendering
status: stable
groma:
  id: legacy-markdown-rendering
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/JEditorPaneUtils.java
      symbol: JEditorPaneUtils
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/StyleSheetsFactory.java
      symbol: StyleSheetsFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/processor/BlockProcessor.java
      symbol: BlockProcessor
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/processor/FencedCodeBlockProcessor.java
      symbol: FencedCodeBlockProcessor
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/processor/IndentedCodeBlockProcessor.java
      symbol: IndentedCodeBlockProcessor
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/processor/NodeProcessor.java
      symbol: NodeProcessor
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/renderer/CodeBlockNodeRenderer.java
  group: Chat experience
  technology: CommonMark, Swing
description: Pre-Compose Markdown-to-Swing renderers with no remaining callers
---

CommonMark node processors and stylesheet helpers that rendered responses into Swing editor panes before the chat output moved to Compose. No class outside this component references them today.
