---
sidebar_position: 1
title: Features Overview - DevoxxGenie Documentation
description: An overview of all features available in DevoxxGenie, including core features, LLM provider support, and advanced capabilities for IntelliJ IDEA.
keywords: [devoxxgenie, intellij plugin, ai features, llm, code assistance, rag, mcp, skills]
image: /img/devoxxgenie-social-card.jpg
---

# Features Overview

DevoxxGenie offers a comprehensive set of features designed to enhance your development workflow with AI assistance. This page provides an overview of the key features available in the plugin.

## Core Features

### Multi-Provider LLM Support

Connect to a wide range of LLM providers:

- **Local Providers**: Ollama, LMStudio, GPT4All, Llama.cpp, Jan, and custom OpenAI-compatible providers
- **Cloud Providers**: OpenAI, Anthropic (Claude), Mistral, Groq, Google (Gemini), Grok (xAI), DeepInfra, DeepSeek, Kimi, GLM (Zhipu AI), OpenRouter, Azure OpenAI, and Amazon Bedrock

### Chat Interface

- **Streaming responses**: See each token as it's received from the LLM in real-time
- **Code highlighting**: Proper syntax highlighting for code blocks in responses
- **Chat memory**: Configurable conversation history to maintain context
- **Context files**: Add files and code snippets to the chat context

### Advanced Context Features

- **RAG Support**: Retrieval-Augmented Generation for automatically finding and incorporating relevant code from your project
- **Project Scanner**: Add source code (full project or by package) to prompt context
- **DEVOXXGENIE.md**: Generate and customize a project description file that gets included in the system prompt
- **Abstract Syntax Tree (AST) context**: Automatically include parent class and class/field references

### Developer Tools

- **Skills**: Built-in and custom slash commands (`/test`, `/explain`, `/review`, `/find`, etc.) with `$ARGUMENT` placeholder support
- **MCP Support**: Model Context Protocol servers for extended agent-like capabilities, with a built-in Marketplace for discovering servers
- **Sub-Agents** *(v0.9.4+)*: Parallel codebase exploration using multiple read-only sub-agents that can concurrently investigate different parts of your project, each configurable with a different LLM from any provider
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
| RAG Support | ✅ | ✅ | ✅ | ✅ | ✅ |
| Project Context | ✅ | ✅ | ✅ | ✅ | ✅ |
| MCP Support | ✅ | ✅ | ✅ | ✅ | ✅ |
| Skills | ✅ | ✅ | ✅ | ✅ | ✅ |
| Web Search | ✅ | ✅ | ✅ | ✅ | ✅ |
| Multimodal | Varies* | GPT-4V+ | Claude 3+ | Gemini Pro+ | Varies |

*Depends on the model you're using locally (e.g., LLaVA supports images)

## Experimental Features

DevoxxGenie also includes experimental features that are being developed and refined:

- **Test Driven Generation (TDG)**: Write tests and generate implementation code

## Feature Details

For detailed information about specific features, check out the dedicated pages:

- [Chat Interface](chat-interface.md)
- [MCP Support](mcp_expanded.md)
- [Sub-Agents](sub-agents.md)
- [Skills](skills.md)
- [Web Search](web-search.md)
- [RAG Support](rag.md)
- [Drag & Drop Images](dnd-images.md)
- [Project Scanner](project-scanner.md)
- [Chat Memory](chat-memory.md)
