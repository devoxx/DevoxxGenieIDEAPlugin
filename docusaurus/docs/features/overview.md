---
sidebar_position: 1
---

# Features Overview

DevoxxGenie offers a comprehensive set of features designed to enhance your development workflow with AI assistance. This page provides an overview of the key features available in the plugin.

## Core Features

### Multi-Provider LLM Support

Connect to a wide range of LLM providers:

- **Local Providers**: Ollama, LMStudio, GPT4All, Llama.cpp, Exo, Jan, and custom OpenAI-compatible providers
- **Cloud Providers**: OpenAI, Anthropic (Claude), Mistral, Groq, Google (Gemini), DeepInfra, DeepSeek, OpenRouter, Azure OpenAI, and Amazon Bedrock

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

- **Git Diff/Merge**: Review and accept LLM-generated code changes with a Git diff interface
- **Web Search**: Augment LLM knowledge with web search results from Google or Tavily
- **Token Cost Calculator**: Calculate token usage and cost before sending prompts
- **MCP Support**: Model Context Protocol servers for extended capabilities

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
| Git Diff | ✅ | ✅ | ✅ | ✅ | ✅ |
| Web Search | ✅ | ✅ | ✅ | ✅ | ✅ |
| Multimodal | Varies* | GPT-4V+ | Claude 3+ | Gemini Pro+ | Varies |

*Depends on the model you're using locally (e.g., LLaVA supports images)

## Experimental Features

DevoxxGenie also includes experimental features that are being developed and refined:

- **Test Driven Generation (TDG)**: Write tests and generate implementation code
- **GraphRAG**: Enhanced RAG capabilities using knowledge graphs (coming soon)

## Feature Details

For detailed information about specific features, check out the dedicated pages:

- [Chat Interface](chat-interface.md)
- [RAG Support](rag.md)
- [MCP Support](mcp.md)
- [Web Search](web-search.md)
- [Git Diff](git-diff.md)
- [Drag & Drop Images](dnd-images.md)
- [Project Scanner](project-scanner.md)
- [Chat Memory](chat-memory.md)
