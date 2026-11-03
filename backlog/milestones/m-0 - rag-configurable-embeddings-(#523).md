---
id: m-0
title: "RAG: configurable embeddings (#523)"
---

## Description

Epic for GitHub #523: let users choose their RAG embedding model and provider (Ollama or any OpenAI-compatible endpoint such as llama.cpp), with query/document prefixes for asymmetric models and Matryoshka dimension truncation. Changing these settings must never silently mix incompatible vectors: the index is fingerprinted and re-indexing is offered. A Settings "Test" button verifies the unsaved configuration end-to-end before Apply. Also includes the RAG settings validation fixes needed for this to work reliably (no network calls on the EDT, no duplicate action listeners). Order: TASK-212 → TASK-265 → TASK-266; TASK-267 and TASK-268 are independent.
