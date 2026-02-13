---
sidebar_position: 1
title: Introduction to DevoxxGenie - AI Code Assistant for IntelliJ IDEA
description: DevoxxGenie is a Java-based LLM Code Assistant plugin for IntelliJ IDEA that integrates with both local and cloud-based LLM providers to enhance your development workflow.
keywords: [devoxxgenie, intellij plugin, ai coding assistant, llm, java, code generation, cloud llm, local llm]
image: /img/devoxxgenie-social-card.jpg
slug: /getting-started/introduction
---

# Introduction to DevoxxGenie

![DevoxxGenie Logo](../../static/img/genie.svg)

## What is DevoxxGenie?

DevoxxGenie is a fully Java-based LLM Code Assistant plugin for IntelliJ IDEA, designed to integrate with both local LLM providers and cloud-based LLM services.

With DevoxxGenie, developers can leverage the power of artificial intelligence to improve code quality, solve problems faster, and learn new concepts, all within their familiar IDE environment.

## Key Features

DevoxxGenie provides a rich set of features to enhance your development workflow:

- **[Multiple LLM Providers](../llm-providers/overview.md)**: Connect to local LLMs like Ollama, LMStudio, and GPT4All, as well as cloud-based providers like OpenAI, Anthropic, Google, Grok, Mistral, Groq, Kimi, GLM, and more.
- **[MCP Support](../features/mcp_expanded.md)**: Model Context Protocol servers for agent-like capabilities, with a built-in Marketplace for discovering and installing servers.
- **[Agent Mode](../features/agent-mode.md)** *(v0.9.4+)*: Enable agent mode for autonomous codebase exploration using read-only tools. Parallel sub-agents allow concurrent investigation of multiple aspects, each configurable with a different LLM from any provider.
- **[Spec Driven Development](../features/spec-driven-development.md)** *(v0.9.7+)*: Define tasks in Backlog.md, browse them in the Spec Browser with Task List and Kanban Board views, then let the Agent implement them autonomously.
- **[CLI Runners](../features/cli-runners.md)** *(v0.9.9+)*: Execute prompts and spec tasks via external CLI tools (Claude Code, GitHub Copilot, Codex, Gemini CLI, Kimi) directly from the chat interface or the Spec Browser.
- **[ACP Runners](../features/acp-runners.md)** *(v0.9.10+)*: Communicate with external agents via the Agent Communication Protocol (JSON-RPC 2.0) with structured streaming, conversation history, and capability negotiation.
- **[Inline Code Completion](../features/inline-completion.md)** *(v0.9.6+)*: AI-powered code suggestions as you type using Fill-in-the-Middle (FIM) models via Ollama or LM Studio.
- **[Skills](../features/skills.md)**: Built-in and custom slash commands (`/test`, `/explain`, `/review`, `/find`, etc.) for common development tasks.
- **Git Diff/Merge**: Show Git Diff/Merge dialog to accept LLM suggestions.
- **[Chat History](../features/chat-interface.md)**: Your chats are stored locally, allowing you to easily restore them in the future.
- **[Project Scanner](../features/project-scanner.md)**: Add source code (full project or by package) to prompt context when using compatible LLM providers.
- **[Token Cost Calculator](../configuration/token-cost.md)**: Calculate the cost when using Cloud LLM providers.
- **[Web Search](../features/web-search.md)**: Search the web for a given query using Google or Tavily.
- **[Streaming responses](../features/chat-interface.md)**: See each token as it's received from the LLM in real-time.
- **[DnD Images](../features/dnd-images.md)**: Drag and drop images with multimodal LLM support.
- **[Appearance Customization](../configuration/appearance.md)**: Customize chat interface colors, spacing, and font sizes.
- **100% Java**: Built entirely in Java using the Langchain4J library.

## Why Use DevoxxGenie?

- **Multiple LLM options**: Choose between running models locally for privacy or using cloud providers for more powerful models.
- **Seamless IDE Integration**: Work with AI assistance without leaving your development environment.
- **Enhanced Context Awareness**: DevoxxGenie can understand your project structure and incorporate relevant code context.
- **Focused on Java Development**: Built by Java developers for Java developers.
- **Open Source**: Community-driven development and transparent about its capabilities and limitations.

## Getting Started

Ready to get started with DevoxxGenie? Check out our [Installation Guide](installation.md) and [Quick Start guides](quick-start-local.md) to begin using DevoxxGenie in your development workflow.
