---
type: Groma Flow
title: Answer with RAG context
groma:
  id: answer-with-rag-context
---

With RAG enabled and agent mode off, prompt creation embeds the query and retrieves the closest project chunks from ChromaDB, injecting them as semantic context before the LLM call.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Developer](../actors/developer.md) | [Chat tool window and prompt input](../systems/devoxxgenieideaplugin/containers/plugin/components/chat-tool-window.md) | Submit a prompt with RAG enabled |
| [Chat tool window and prompt input](../systems/devoxxgenieideaplugin/containers/plugin/components/chat-tool-window.md) | [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | Start execution |
| [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | [RAG indexing and semantic search](../systems/devoxxgenieideaplugin/containers/plugin/components/rag-indexing-search.md) | Request semantic context for the prompt |
| [RAG indexing and semantic search](../systems/devoxxgenieideaplugin/containers/plugin/components/rag-indexing-search.md) | [ChromaDB container manager](../systems/devoxxgenieideaplugin/containers/plugin/components/chromadb-manager.md) | Query the project collection |
| [ChromaDB container manager](../systems/devoxxgenieideaplugin/containers/plugin/components/chromadb-manager.md) | [Local LLM runtimes](../externals/local-llm-runtimes.md) | Embed the query with nomic-embed-text |
| [ChromaDB container manager](../systems/devoxxgenieideaplugin/containers/plugin/components/chromadb-manager.md) | [ChromaDB](../externals/chromadb.md) | Search nearest chunks |
| [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | [Conversation view](../systems/devoxxgenieideaplugin/containers/plugin/components/conversation-view.md) | Stream the grounded answer |
