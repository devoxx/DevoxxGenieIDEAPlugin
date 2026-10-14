---
type: C4 Actor
title: Developer
status: stable
groma:
  id: developer
---

A software developer using JetBrains IDEs (IntelliJ IDEA and other IntelliJ-platform IDEs). Installs DevoxxGenie from the JetBrains Marketplace and chats with an LLM about their project, runs agent mode, indexes code for RAG, manages Backlog.md spec tasks, and gets inline code completions. Also reads the documentation website at genie.devoxx.com.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [developer](developer.md) | [src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowFactory.java](../../src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowFactory.java) | Chats with LLMs | IDE tool window |
| [developer](developer.md) | [docusaurus/src/pages/index.js](../../docusaurus/src/pages/index.js) | Reads documentation | HTTPS browser |
| [developer](developer.md) | [src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java](../../src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java) | Runs spec tasks | IDE tool window |
