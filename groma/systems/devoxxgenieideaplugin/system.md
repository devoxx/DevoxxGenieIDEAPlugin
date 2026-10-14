---
type: C4 System
title: DevoxxGenie
status: stable
groma:
  id: devoxxgenieideaplugin
description: LLM code assistant for JetBrains IDEs, plus its documentation website
---

DevoxxGenie lets developers chat with local or cloud LLMs about their project from inside IntelliJ-platform IDEs. Beyond plain chat it offers an agent mode with IDE-aware tools, MCP tool servers, RAG over the project, web search, spec-driven development on Backlog.md tasks, delegation to external coding agents (CLI, ACP, Docker Agentic Platform), event automations, security scanning and inline completion.

The system has two deployables: the IntelliJ plugin itself, and the Docusaurus documentation website at genie.devoxx.com, which also serves the model catalogue and blog feed the plugin reads at runtime.
