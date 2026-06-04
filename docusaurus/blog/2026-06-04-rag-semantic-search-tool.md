---
slug: rag-semantic-search-tool
title: "Why We Turned RAG Into a Tool"
authors: [stephanj]
tags: [rag, semantic search, agent mode, chromadb, ollama, intellij idea, open source]
date: 2026-06-04
description: When agent-mode LLMs started ignoring our carefully injected semantic context, we stopped wallpapering prompts and exposed RAG as a proper tool. Here's how we built semantic_search and why agentic retrieval needs to be orchestrated, not injected.
keywords: [devoxxgenie, rag, semantic search, agent mode, chromadb, ollama, nomic-embed-text, retrieval augmented generation, intellij plugin]
image: /img/rag-feature.png
---

# Why We Turned RAG Into a Tool

For months, DevoxxGenie's RAG pipeline worked beautifully — as long as you stayed in chat mode. Index your project into ChromaDB, ask a question, and the most relevant code chunks would automatically appear in the prompt as a `<SemanticContext>` block. It was invisible, automatic, and effective.

Then we shipped Agent Mode, and RAG fell off a cliff.

Users would ask conceptual questions like *"which slides discuss MCP?"* or *"where do we explain the indexing pipeline?"* and the agent would ignore the rich semantic context we had just injected. Instead, it reached for `search_files` — a regex grep — and returned nonsense. The semantic context had become wallpaper: present, but unseen.

<!-- truncate -->

![RAG Feature Overview](/img/rag-feature.png)

## The tool bias problem

Agent-mode LLMs are trained to prefer tools over passive context. When both a `<SemanticContext>` block and a `search_files` tool are available, the model almost always chooses the tool. It's not being lazy; it's doing exactly what the agent loop incentivises. The problem is that regex grep is terrible for conceptual queries. *"Authentication flow"* doesn't match `auth.*flow` in any file — but it embeds close to the actual auth code.

We tried making the tool descriptions more persuasive. We tried bigger models. Smaller models still defaulted to grep. The only reliable fix was to stop fighting the bias and **lean into it**: if the model wants a tool, give it a tool.

## From injection to orchestration

The `semantic_search` tool (task-221, refined in task-222) does exactly what the passive injection did — embed the query, search ChromaDB, return the top-K chunks — but exposes it through the agent tool loop instead of prepending it to the prompt.

The key design decision was **mutual exclusion**: when Agent mode is on, passive injection is completely suppressed. The LLM sees the index only through the tool. This avoids three problems at once:

1. **Token waste** — no duplicate context in the prompt
2. **Contradictory sources** — the LLM can't get confused by injected chunks that differ from tool-retrieved ones
3. **Tool bias** — the model has no choice but to use the tool if it wants semantic retrieval

Of course, the agent still has `search_files` for exact-string lookups, `list_files` for browsing, and the PSI tools for symbol navigation. The point isn't to replace those. It's to give the agent an *orchestration layer*: semantic search for meaning, grep for literals, PSI for symbols.

## Query expansion and the meta-query trap

Conceptual queries have a second problem: they embed like conversational boilerplate. Ask *"where do we discuss authentication?"* and the embedding lands near chit-chat, not near `AuthenticationService.java`.

Our answer was optional **query expansion** via `ExpandingQueryTransformer`. The query is paraphrased into multiple variants, each searched independently, and the results fused with **Reciprocal Rank Fusion** (RRF, k=60). A single meta-query becomes a small retrieval ensemble. It's overkill for "find the User class" and essential for "where do we explain the indexing pipeline?"

## The nudge that smaller models need

Even with the tool registered, we noticed smaller models still sometimes defaulted to `search_files` for conceptual queries. Tool descriptions alone weren't enough. So when Agent mode + RAG are both enabled, DevoxxGenie now injects a dedicated `<RAG_INSTRUCTION>` system-prompt fragment:

> Prefer `semantic_search` for conceptual queries (e.g. "which slides discuss X", "where do we explain Y").

This lives outside the tool schema, in the system prompt itself, where smaller models are more likely to honour it. It's a small patch, but it meaningfully improved tool selection on models like Qwen 2.5 and Gemma.

## Error handling as a first-class concern

One subtle requirement: the tool must fail gracefully. If ChromaDB is unreachable, throwing an exception breaks the agent loop. Instead, `SemanticSearchToolExecutor` returns a descriptive error string:

```
Error: ChromaDB is not available. Docker container may not be running.
```

The agent reads this, understands the index is down, and falls back to `search_files` or PSI tools. No modal dialogs, no stack traces in the chat window. Because `semantic_search` is classified as a **read-only tool**, it's also auto-approved — no approval friction on every call.

## The user-control layer

Not everyone wants the agent to have semantic search. We kept the control granular: `semantic_search` appears in **Settings → Agent Mode → Built-in Tools** as an individual checkbox, independent of the master RAG switch. You can keep passive injection active for chat mode while excluding the tool from the agent's toolbox, or vice versa.

## Architecture in three layers

The implementation is thin by design — it reuses the existing RAG stack rather than building a parallel one:

| Layer | Component | Role |
|-------|-----------|------|
| **Storage** | `ProjectIndexerService` + ChromaDB | Language-aware chunking, content-hash manifest, batched embeddings |
| **Retrieval** | `SemanticSearchService` | Embedding, optional query expansion, RRF fusion, score filtering |
| **Agent glue** | `SemanticSearchToolExecutor` | Formats results for LLM consumption, truncates snippets, returns safe errors |

Registration happens in `BuiltInToolProvider`: the tool is added only when `ragEnabled` is true. Suppression of passive injection lives in `MessageCreationService.shouldInjectPassiveRagContext()`. The `<RAG_INSTRUCTION>` fragment is injected by `ChatMemoryManager`. Each concern is separated, so the feature can be disabled or extended without touching the core RAG pipeline.

## What changed, what didn't

The RAG pipeline itself is unchanged. It still indexes via Ollama's `nomic-embed-text`, still stores vectors in ChromaDB v0.6.2, still filters low-content chunks at index time, still debounces re-indexing on save. What changed is the **interface surface**: from prompt injection to tool contract.

If you're already using RAG in chat mode, nothing breaks. If you turn on Agent mode, the same index becomes queryable on demand. And if you want the gory setup details — Docker, Ollama, indexing, configuration — the [RAG docs](/docs/features/rag) have you covered.

**Install:** [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24169-devoxxgenie) · [GitHub](https://github.com/devoxx/DevoxxGenieIDEAPlugin)
