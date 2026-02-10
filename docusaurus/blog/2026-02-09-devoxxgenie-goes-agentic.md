---
slug: devoxxgenie-goes-agentic
title: The DevoxxGenie Plugin Goes Agentic
authors: [stephanj]
tags: [agent mode, MCP, inline completion, agentic AI, IntelliJ IDEA, LLM, Java, open source]
date: 2026-02-09
description: DevoxxGenie evolves from passive AI assistance to agentic AI — with Agent Mode, parallel sub-agents, MCP Marketplace, and privacy-focused inline code completion.
keywords: [devoxxgenie, agent mode, mcp, inline completion, agentic programming, intellij plugin, parallel sub-agents, langchain4j]
image: /img/devoxxgenie-social-card.jpg
---

# The DevoxxGenie Plugin Goes Agentic

DevoxxGenie has come a long way from its origins as a simple LLM chat plugin for IntelliJ IDEA. With the latest releases, the plugin has made a fundamental shift — from passive AI assistance to **agentic AI capabilities**. This reflects a paradigm shift in how developers interact with AI: we're moving beyond generating code snippets toward autonomous agents that can explore, reason about, and modify your codebase.

<!-- truncate -->

## Agent Mode with Parallel Sub-Agents

The centerpiece of this evolution is **Agent Mode**. When enabled, the LLM doesn't just answer questions — it autonomously explores your codebase through seven built-in tools:

- **`read_file`** — inspect project files
- **`write_file`** — create new files
- **`edit_file`** — modify existing code
- **`list_files`** — browse directories
- **`search_files`** — find patterns via regex
- **`run_command`** — execute terminal commands
- **`parallel_explore`** — spawn concurrent sub-agents

Read operations auto-approve for efficiency, while write operations require explicit user confirmation via a diff preview. You stay in control.

### Parallel Sub-Agents

What makes this especially powerful is **parallel sub-agents**. Multiple read-only AI assistants can investigate different parts of your project simultaneously. Each sub-agent operates with:

- Isolated memory (no cross-contamination)
- Independent tool budgets
- Potentially different LLM providers

The main agent synthesizes their findings into a comprehensive response. This opens up interesting cost optimization strategies — use smaller, cheaper models like Gemini Flash or GLM 4.7 for sub-agents, while keeping a powerful coordinator like Claude Opus or GPT-4 as the main agent.

![Agent Mode Settings](/img/agent-mode-top.jpg)

## MCP Marketplace

DevoxxGenie now integrates the **Model Context Protocol (MCP)**, allowing agents to access external services — databases, APIs, documentation, cloud infrastructure — through standardized tool interfaces.

The built-in **MCP Marketplace** connects to an official MCP server registry, making it easy to discover and install servers with just a few clicks. No more manual configuration of JSON files or hunting for compatible servers.

![MCP Marketplace](/img/MCPMarketplace.jpg)

## Inline Code Completion

For developers who want AI assistance without the overhead of a full conversation, DevoxxGenie now offers **real-time inline code completion** using local Fill-in-the-Middle (FIM) models.

Key characteristics:

- **Runs entirely locally** via Ollama or LM Studio — your code never leaves your machine
- **Smart post-processing**: suffix overlap detection, leading newline stripping
- **Caching**: recent completions are cached for instant retrieval
- **Configurable**: debounce delay (100–2000ms), max tokens (16–256), timeout (1–30s)

Supported models include StarCoder2 3B, Qwen 2.5 Coder, and DeepSeek-Coder — all running locally for complete privacy.

## Built for Developer Control

DevoxxGenie is built entirely in Java using [Langchain4J](https://github.com/langchain4j/langchain4j) and supports both local providers (Ollama, LM Studio, GPT4All, Llama.cpp) and cloud services (OpenAI, Anthropic, Google Gemini, Mistral, DeepSeek, and more). With over 42,000 downloads on the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24169-devoxxgenie), it's become a go-to tool for Java developers who want flexibility in their AI tooling.

The plugin prioritizes developer control across every dimension:

- **Model selection** — use any provider, switch freely
- **Approval workflows** — confirm writes before they happen
- **Spending** — track token costs in real-time
- **External service access** — MCP tools are explicitly configured
- **Code privacy** — local models for sensitive codebases

## What's Next

This is just the beginning. With [Spec Driven Development](/docs/features/spec-driven-development) (SDD) now available in v0.9.7, you can define tasks as structured specs and let the agent implement them autonomously — complete with acceptance criteria tracking and a visual Kanban board.

We're not just chatting with AI anymore. We're collaborating with it.

**Try it out:**
- [Install from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24169-devoxxgenie)
- [Full Documentation](https://genie.devoxx.com)
- [GitHub Repository](https://github.com/devoxx/DevoxxGenieIDEAPlugin)
