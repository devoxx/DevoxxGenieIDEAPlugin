---
sidebar_position: 6
title: RAG Support - Retrieval-Augmented Generation
description: Learn how DevoxxGenie uses Retrieval-Augmented Generation (RAG) to improve AI assistance by automatically finding and incorporating relevant code from your project.
keywords: [rag, retrieval augmented generation, chromadb, ollama, code search, intellij, ai coding]
image: /img/devoxxgenie-social-card.jpg
slug: /features/rag
---

# RAG Support

Retrieval-Augmented Generation (RAG) enhances the LLM's ability to understand and interact with your codebase by automatically finding and incorporating relevant code from your project.

## What is RAG?

RAG combines retrieval of information with text generation:

1. **Retrieval**: When you ask a question, DevoxxGenie searches your codebase to find the most relevant files and code snippets
2. **Augmentation**: These relevant code snippets are added to the prompt context
3. **Generation**: The LLM then generates a response informed by this contextual information

This dramatically improves response quality for questions about your specific codebase.

## Prerequisites

RAG requires the following components to be installed and running:

### 1. Docker

Docker must be installed and running on your machine. RAG uses Docker to run the ChromaDB vector database.

- [Install Docker](https://docs.docker.com/get-docker/)

### 2. ChromaDB

DevoxxGenie uses [ChromaDB](https://www.trychroma.com/) (v0.6.2) as the vector database for storing code embeddings. The ChromaDB container is managed automatically via Docker.

### 3. Ollama with nomic-embed-text

An embedding model is required to generate vector representations of your code. DevoxxGenie uses the `nomic-embed-text` model via Ollama:

```bash
ollama pull nomic-embed-text
```

Make sure Ollama is running before enabling RAG.

## Setup

1. Ensure Docker is running
2. Ensure Ollama is running with `nomic-embed-text` pulled
3. Open **Settings** > **Tools** > **DevoxxGenie** > **RAG**
4. Configure the settings (see below)
5. Index your project files

DevoxxGenie will validate that Docker, ChromaDB, and Ollama are all available before enabling RAG. If any prerequisite is missing, you'll see a notification explaining what needs to be set up.

## Configuration Options

| Setting | Description | Default |
|---------|-------------|---------|
| ChromaDB port | Port for the ChromaDB container | `8000` |
| Max results | Maximum number of relevant documents to retrieve | `5` |
| Min score | Minimum similarity score for including a result | `0.7` |

## Using RAG

### Indexing Your Project

Before RAG can search your code, you need to index your project:

1. Use the **Index Files** action in the DevoxxGenie panel
2. DevoxxGenie will scan your project, generate embeddings, and store them in ChromaDB
3. Indexing runs in the background — you can continue working

### Searching with RAG

Once indexed, there are three ways RAG content reaches the LLM. Which one applies depends on whether Agent mode is on:

1. **Passive injection (Agent mode OFF)** — when RAG is enabled in settings, the top-K most relevant chunks for your prompt are automatically prepended to the message context as a `<SemanticContext>` block. You don't need to do anything; every prompt long enough to be worth retrieving for triggers a search.
2. **`semantic_search` agent tool (Agent mode ON)** — instead of injecting context up front, RAG is exposed as an agent tool the LLM can call on demand. The agent decides when to retrieve semantically vs. when to use lexical tools like `search_files` for known exact strings. Passive injection is suppressed in this mode so the LLM doesn't see the same content twice.
3. **`/find` command** — works in either mode. Use it to ask for retrieval explicitly:
   ```
   /find authentication flow
   ```

#### Why two modes?

Models bias strongly toward calling tools when both a tool and a passive context block are available — they would invoke `search_files` (regex grep) and ignore the higher-quality semantic context. Switching to a tool-based interface in agent mode lets the agent orchestrate: semantic search for conceptual queries, grep/glob for exact strings.

When Agent mode + RAG are both on, DevoxxGenie also injects a `<RAG_INSTRUCTION>` system-prompt fragment telling the LLM to prefer `semantic_search` for conceptual queries (e.g. "which slides discuss X", "where do we explain Y"). This is necessary because smaller models don't reliably honor "prefer this tool" hints buried inside tool descriptions.

#### Enabling / disabling the `semantic_search` tool

The tool is registered automatically whenever RAG is enabled and Agent mode is on. To override that — e.g. to force the agent to use lexical search only — uncheck `semantic_search` under **Settings → DevoxxGenie → Agent Mode → Built-in Tools**. The setting is independent of the master RAG switch, so you can keep passive injection active for non-agent prompts while excluding `semantic_search` from the agent's toolbox.

### Example Queries

These types of queries benefit most from RAG (in either mode):

- "How does the authentication flow work in this project?"
- "Explain the data model for users"
- "Generate a new service method that follows our existing patterns"
- "Which slides discuss the Model Context Protocol?" (agent mode → triggers `semantic_search`)
- "Where do we describe the indexing pipeline?" (agent mode → triggers `semantic_search`)

## Troubleshooting

### RAG Not Returning Relevant Context

- Ensure your project has been indexed
- Try rebuilding the index
- Check that your search query is specific enough

### Docker or ChromaDB Issues

- Verify Docker is running: `docker ps`
- Check if ChromaDB container is healthy
- Try restarting Docker and re-indexing

### Ollama Issues

- Verify Ollama is running: `ollama list`
- Ensure `nomic-embed-text` is pulled: `ollama pull nomic-embed-text`
