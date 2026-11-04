---
sidebar_position: 6
title: RAG Support - Retrieval-Augmented Generation
description: Learn how DevoxxGenie uses Retrieval-Augmented Generation (RAG) to improve AI assistance by automatically finding and incorporating relevant code from your project.
keywords: [rag, retrieval augmented generation, chromadb, ollama, embeddings, llama.cpp, lm studio, openai, code search, intellij, ai coding]
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

### 3. An embedding provider

An embedding model turns your code into vectors. By default DevoxxGenie uses the [`nomic-embed-text`](https://ollama.com/library/nomic-embed-text) model through Ollama:

```bash
ollama pull nomic-embed-text
```

You can also use another provider you've enabled in the LLM settings; see [Choosing an embedding provider](#choosing-an-embedding-provider). Make sure your provider is running before enabling RAG.

## Setup

1. Ensure Docker is running
2. Ensure your embedding provider is running. For the default setup, that's Ollama
3. Open **Settings** > **Tools** > **DevoxxGenie** > **RAG**
4. Configure the settings (see below)
5. Index your project files

DevoxxGenie checks that Docker, ChromaDB and your embedding provider are available, and shows the result under **Required RAG Services**.

## Configuration Options

| Setting | Description | Default |
|---------|-------------|---------|
| ChromaDB port | Port for the ChromaDB container | `8000` |
| Embedding provider | Where embeddings come from | Ollama |
| Embedding model | Model used to embed files and queries | `nomic-embed-text` |
| Query prefix | Text added before search queries when embedding | Depends on the model |
| Document prefix | Text added before indexed code when embedding | Depends on the model |
| Output dimension | Shorten embeddings to this size; `0` keeps the model's own size | `0` |
| Max results | Maximum number of relevant documents to retrieve | `10` |
| Min score | Minimum similarity score for including a result | `0.7` |

RAG is controlled by the **Enable RAG** setting; there is no separate per-conversation activation toggle.

### Choosing an embedding provider

The **Embedding provider** dropdown lists the providers that can create embeddings and are enabled in the LLM settings. RAG uses the URL and API key you've already configured there.

| Provider | Notes |
|----------|-------|
| **Ollama** | The default, and always available. Offers to download a missing model |
| **Custom OpenAI** | Any server with an OpenAI-compatible `/embeddings` endpoint |
| **OpenAI** | Uses your OpenAI API key |
| **LM Studio** | Load an embedding model in LM Studio first |
| **llama.cpp** | Start the server with embeddings turned on (see below) |

:::warning Cloud providers receive your code
With OpenAI, or any provider whose URL isn't on your own machine, your project's source code is sent to that provider during indexing, and your search queries when you search. The provider may charge for this, and re-indexing a large project sends everything again. DevoxxGenie shows a warning when you select such a provider.
:::

For example, to embed with llama.cpp on your own machine:

```bash
llama-server -m your-embedding-model.gguf --embeddings --port 8080
```

Then enable llama.cpp in the LLM settings, select it as the **Embedding provider**, and enter your model's name as the **Embedding model**.

### Choosing an embedding model

The **Embedding model** field suggests common models for the selected provider and accepts any name, including tagged Ollama names such as `nomic-embed-text:v1.5`. See [Ollama's embedding models](https://ollama.com/search?c=embedding) for what's available there.

With Ollama, if the model isn't installed when you start indexing, DevoxxGenie asks whether to download it and starts indexing when the download finishes. Other providers don't offer downloads, so install the model on that provider first.

Before changing your index, DevoxxGenie checks that the model can create embeddings. If you decline a download, the download fails, or the model can't create embeddings (for example, a chat model), indexing stops with a message and your existing index isn't changed.

### Query and document prefixes

Many embedding models work best when told whether a text is a search query or a document, using a short prefix such as `Query: ` and `Document: `. Prefixes only change what's sent to the model: they're never stored in the index or shown to the LLM.

DevoxxGenie fills in `search_query: ` and `search_document: ` for `nomic-embed-text`, as its documentation recommends. Other models start with empty prefixes, so check your model's documentation. Include any trailing space the model expects. Once you type your own prefix, it stays when you change model.

### Shortening embeddings

Some models (known as Matryoshka models) still work well when their vectors are cut short, which saves memory and speeds up search. Set **Output dimension** to use this: DevoxxGenie shortens the vectors itself and re-normalizes them, so it works on any provider. A size larger than the model produces is reported as an error.

This generally costs some search quality, so measure it on your own code. Not every model supports it.

### When you need to re-index

Vectors from different setups can't be compared, so **changing the provider, model, document prefix or output dimension requires re-indexing the project**. Changing only the query prefix doesn't.

When you apply such a change, DevoxxGenie shows a notification with a **Re-index now** button; nothing is re-indexed automatically. Until you re-index, search keeps using the settings the existing index was built with, so RAG keeps working. Re-indexing replaces the old index completely.

:::info Upgrading from an earlier version
Prefixes for `nomic-embed-text` are now on by default, so an existing index needs one re-index to use them. DevoxxGenie tells you once per project. Until you re-index, search keeps working as before. To keep the old behavior, clear both prefix fields.
:::

### Measured results

Measured on DevoxxGenie's own repository (1,458 files, 28 questions with a known answer file, `./gradlew ragBenchmark`). This is a small sample from one code base, so treat it as a guide:

| Setup | Answer file ranked first | in top 5 | in top 10 |
|-------|--------------------------|----------|-----------|
| `nomic-embed-text` without prefixes | 14% | 64% | 71% |
| `nomic-embed-text` with prefixes (default) | 25% | 54% | 82% |
| `nomic-embed-text` with prefixes, 256 dimensions | 21% | 57% | 61% |

Prefixes put the right file first more often and find it in the top 10 more often, but rank it in the top 5 less often. Cutting vectors to 256 dimensions lost quality here, most clearly in the top 10.

In an earlier run without prefixes, on a slightly smaller set of files, [`bge-m3`](https://ollama.com/library/bge-m3) (1.2 GB) ranked the answer first for 25% of questions, in the top 5 for 50% and in the top 10 for 86%, and took about 2.6 times as long to index as `nomic-embed-text`. It hasn't been measured against the prefixed default. To try it, pull it with Ollama and select it as the embedding model:

```bash
ollama pull bge-m3
```

The benchmark, and how to write questions for your own code base, is described in `src/test/resources/rag-benchmark/README.md` in the DevoxxGenie repository.

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
- Ensure your embedding model is pulled, e.g. `ollama pull nomic-embed-text` (the validation panel shows which model it checks)

### Other Embedding Provider Issues

- **Not reachable**: check that the server is running and that its URL in the LLM settings is correct
- **Disabled**: enable the provider in the LLM settings, or select another one
- **"Cannot create embeddings"**: check that the model name is right and is an embedding model, and that any API key is set. llama.cpp needs `--embeddings`
- **Dimension error**: lower **Output dimension**, or set it to `0`
