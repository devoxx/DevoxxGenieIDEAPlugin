---
sidebar_position: 1
title: Features Overview - DevoxxGenie Documentation
description: An overview of all features available in DevoxxGenie, including core features, LLM provider support, and advanced capabilities for IntelliJ IDEA.
keywords: [devoxxgenie, intellij plugin, ai features, llm, code assistance, rag, mcp, skills]
image: /img/devoxxgenie-social-card.jpg
---

# Features Overview - DevoxxGenie Documentation

DevoxxGenie offers a comprehensive set of features designed to enhance your development workflow with AI assistance. This page provides an overview of the key features available in the plugin.

## Core Features

### Spec-driven Development (SDD)

Define tasks as structured markdown specs with acceptance criteria, and let the LLM agent implement them autonomously:

- **Backlog.md Integration**: Tasks are stored as version-controlled markdown files with YAML frontmatter
- **Task List & Kanban Board**: Browse tasks in a tree view or manage them visually on a drag-and-drop Kanban board with archive-by-drag support
- **Implement with Agent**: One-click task implementation — the agent reads the spec, makes code changes, and checks off acceptance criteria as it works
- **17 Built-in Backlog Tools**: Create, edit, search, and complete tasks, documents, and milestones programmatically during agent execution

### Multi-Provider LLM Support

Connect to a wide range of LLM providers:

- **Local Providers**: Ollama, LMStudio, GPT4All, Llama.cpp, Jan, and custom OpenAI-compatible providers
- **Cloud Providers**: OpenAI, Anthropic (Claude), Mistral, Groq, Google (Gemini), Grok (xAI), DeepInfra, DeepSeek, Kimi, GLM (Zhipu AI), OpenRouter, Azure OpenAI, and Amazon Bedrock

### Chat Interface

- **Streaming responses**: See each token as it's received from the LLM in real-time
- **Code highlighting**: Proper syntax highlighting for code blocks in responses
- **Chat memory**: Configurable conversation history to maintain context
- **Context files**: Add files and code snippets to the chat context

### Code Completion

- **Inline Completion**: AI-powered code suggestions as you type using Fill-in-the-Middle (FIM) models via Ollama or LM Studio
- **Context-aware**: Uses code before and after your cursor for intelligent suggestions
- **Ghost text**: Suggestions appear as gray text inline with your code
- **Partial acceptance**: Accept full suggestions, single words, or just the current line

### Advanced Context Features

- **RAG Support**: Retrieval-Augmented Generation for automatically finding and incorporating relevant code from your project
- **Project Scanner**: Add source code (full project or by package) to prompt context
- **DEVOXXGENIE.md**: Generate and customize a project description file that gets included in the system prompt
- **Abstract Syntax Tree (AST) context**: Automatically include parent class and class/field references

### Developer Tools

- **Inline Completion**: AI-powered code completion using Fill-in-the-Middle (FIM) models via Ollama or LM Studio, providing context-aware suggestions as you type
- **Skills**: Built-in and custom slash commands (`/test`, `/explain`, `/review`, `/find`, etc.) with `$ARGUMENT` placeholder support
- **MCP Support**: Model Context Protocol servers for extended agent-like capabilities, with a built-in Marketplace for discovering servers
- **Agent Mode** *(v0.9.4+)*: Enable agent mode for autonomous codebase exploration using read-only tools. Parallel sub-agents allow concurrent investigation of multiple aspects, each configurable with a different LLM from any provider
- **Web Search**: Augment LLM knowledge with web search results from Google or Tavily
- **Token Cost Calculator**: Calculate token usage and cost before sending prompts

### Multimodal Capabilities

- **Drag & Drop Images**: Add images to your prompts when using multimodal LLMs
- **Multiple File Support**: Attach different types of files to your prompts

## Feature Matrix by LLM Provider

| Feature | Local LLMs | OpenAI | Anthropic | Google | Other Cloud |
|---------|------------|--------|-----------|--------|-------------|
| Chat Interface | ✅ | ✅ | ✅ | ✅ | ✅ |
| Streaming | ✅ | ✅ | ✅ | ✅ | Varies |
| Spec Driven Dev | ✅ | ✅ | ✅ | ✅ | ✅ |
| Inline Completion | ✅* | ❌ | ❌ | ❌ | ❌ |
| RAG Support | ✅ | ✅ | ✅ | ✅ | ✅ |
| Project Context | ✅ | ✅ | ✅ | ✅ | ✅ |
| MCP Support | ✅ | ✅ | ✅ | ✅ | ✅ |
| Skills | ✅ | ✅ | ✅ | ✅ | ✅ |
| Web Search | ✅ | ✅ | ✅ | ✅ | ✅ |
| Multimodal | Varies** | GPT-4V+ | Claude 3+ | Gemini Pro+ | Varies |

\* Ollama or LM Studio with FIM-capable models (e.g., starcoder2, qwen2.5-coder)

\*\*Depends on the model you're using locally (e.g., LLaVA supports images)

## Experimental Features

DevoxxGenie also includes experimental features that are being developed and refined:

- **Test Driven Generation (TDG)**: Write tests and generate implementation code

## Feature Details

For detailed information about specific features, check out the dedicated pages:

- [Spec-driven Development](spec-driven-development.md)
- [Chat Interface](chat-interface.md)
- [MCP Support](mcp_expanded.md)
- [Agent Mode](agent-mode.md)
- [Skills](skills.md)
- [Web Search](web-search.md)
- [RAG Support](rag.md)
- [Drag & Drop Images](dnd-images.md)
- [Project Scanner](project-scanner.md)
- [Chat Memory](chat-memory.md)
- [Inline Completion](inline-completion.md)
