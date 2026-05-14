---
id: TASK-211
title: Add persistent semantic conversation memory backed by ChromaDB
status: To Do
assignee: []
created_date: '2026-05-14 09:00'
updated_date: '2026-05-14 09:00'
labels:
  - enhancement
  - RAG
  - memory
  - architecture
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaDockerService.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaDBManager.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryService.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
  - src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - src/main/java/com/devoxx/genie/model/conversation/Conversation.java
  - src/main/java/com/devoxx/genie/service/MessageCreationService.java
  - src/main/java/com/devoxx/genie/service/ChatService.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/service/mcp/MCPExecutionService.java
  - 'https://github.com/EliasOulkadi/shokunin'
  - 'https://docs.trychroma.com/'
  - 'https://docs.langchain4j.dev/'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
DevoxxGenie currently has no semantic long-term memory. `ChatMemoryService` is just a LangChain4J `MessageWindowChatMemory` (last ~10 messages), and `ConversationStorageService` persists conversations as plain text in SQLite. There is no way to semantically recall decisions, context, or code patterns from past conversations — every new conversation starts cold.

This task adds a persistent, vector-backed **conversation memory** so the assistant can recall relevant context from earlier sessions (e.g. "what did we decide about auth", "what was the approach for the indexer refactor").

The design is inspired by the "memory" feature of the Shokunin project (https://github.com/EliasOulkadi/shokunin), which uses a small ChromaDB-backed store of curated session summaries with three operations: store, search, and session-summary. We are reusing the **pattern**, not the code (Shokunin is a Python MCP server using embedded ChromaDB).

**Key principle: reuse the existing infrastructure.** DevoxxGenie already runs a ChromaDB Docker container, has `ChromaEmbeddingService` + LangChain4J `ChromaEmbeddingStore` wiring, an Ollama `nomic-embed-text` embedding model, and a full MCP framework. This feature should plug into that stack, NOT introduce a parallel vector store or a new runtime dependency.

**Investigation areas:**
- **Separate collection, same container.** The existing code-RAG index uses a per-project collection (sanitized project name). Memory must use a DISTINCT collection (e.g. `devoxx-genie-memory-<projectHash>`) in the SAME ChromaDB instance so conversation summaries never pollute code search results and vice versa. Review `ChromaEmbeddingService` to see how collection names are derived and generalize it to support a second collection.
- **What gets stored.** Store curated, high-signal **summaries** of a conversation (key decisions, code patterns, outcomes) — NOT raw message transcripts. One entry per finished conversation (or per significant task). Keeps the store small and retrieval high-precision. Each entry needs metadata: `project`, `conversationId`, `tags` (JSON array string, since Chroma metadata values are scalars), `timestamp`, and `title`.
- **How summaries are produced.** Decide between (a) an automatic LLM-generated summary when a conversation ends / is saved, (b) explicit user action (`/remember`), or (c) both. Recommended: both — automatic on conversation completion, plus a manual `/remember [text]` command. Summary generation should reuse the currently selected LLM provider via the existing prompt execution path and must run off the EDT.
- **How memory is retrieved.** Add a `searchMemory(query, project, tags?)` operation doing vector similarity search against the memory collection, mirroring `SemanticSearchService` (configurable `minScore` / `maxResults`). At conversation start (or per prompt — decide and document), retrieve relevant past summaries and inject them into the system prompt or user message. Reuse the `<SemanticContext>`-style wrapping pattern in `MessageCreationService` but with a distinct tag (e.g. `<ConversationMemory>`) so it is visually and semantically separate from code RAG context.
- **MCP exposure (recommended).** DevoxxGenie has an MCP framework. Consider exposing `store_context` / `search_context` / `get_session_summary` as internal tools so the LLM itself can decide when to remember/recall, matching the Shokunin model. At minimum, build the service layer so MCP wiring can be added later without rework.
- **Settings & gating.** Memory depends on Docker + ChromaDB + Ollama, so it must be gated behind the existing RAG prerequisites (`RagValidatorService`). Add a dedicated "Conversation Memory" toggle in the RAG settings panel (`DevoxxGenieStateService` + `RAGSettingsComponent`), separate from the code-indexing RAG toggle, plus `minScore` / `maxResults` for memory retrieval.
- **Lifecycle & isolation.** Memory is per-project (project hash), consistent with how ChromaDB collections and SQLite conversations are already keyed. Confirm the memory collection persists across IDE restarts via the existing `ChromaDockerService` volume mount. Provide a way to clear/reset project memory.
- **Known tradeoff to document (not solve here).** Shokunin's headline advantage is a zero-dependency *embedded* ChromaDB. DevoxxGenie's Docker + Ollama requirement is heavyweight and fragile (the whole `RagValidatorService` chain exists for that reason). This task deliberately reuses the existing Docker stack for consistency; a future task could evaluate an embedded/serverless vector store. Capture this as a note, do not expand scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new `ConversationMemoryService` exposes `storeMemory`, `searchMemory`, and `getConversationSummary` operations backed by a dedicated ChromaDB collection (`devoxx-genie-memory-<projectHash>`), distinct from the code-RAG collection
- [ ] #2 The memory collection lives in the SAME ChromaDB Docker container and reuses the existing `ChromaEmbeddingService` / LangChain4J `ChromaEmbeddingStore` and Ollama `nomic-embed-text` embedding wiring — no new runtime dependency or parallel vector store is introduced
- [ ] #3 Stored entries are curated summaries (not raw transcripts) with metadata: project, conversationId, tags, timestamp, and title
- [ ] #4 A conversation summary is generated and stored automatically when a conversation completes/is saved, and the summary generation runs off the EDT using the currently selected LLM provider
- [ ] #5 A `/remember [text]` prompt command lets the user manually store a memory entry, and a `/recall [query]` command (or equivalent) searches and displays past memory
- [ ] #6 When memory is enabled, relevant past summaries are retrieved by vector similarity at conversation start and injected into the conversation context wrapped in a distinct tag (e.g. `<ConversationMemory>`), kept separate from code-RAG `<SemanticContext>` content
- [ ] #7 A "Conversation Memory" toggle plus memory `minScore` / `maxResults` settings are added to the RAG settings panel and persisted via `DevoxxGenieStateService`; the feature is gated behind the existing RAG/Docker/ChromaDB/Ollama prerequisites
- [ ] #8 Memory is isolated per project and persists across IDE restarts; a user-triggered "clear project memory" action is available
- [ ] #9 The service layer is structured so the three operations can later be exposed as MCP tools without rework
- [ ] #10 Unit tests cover store/search/summary operations (with ChromaDB/embedding interactions mocked) and the collection-name isolation between code-RAG and memory; the feature degrades gracefully (no errors, memory simply unavailable) when Docker/ChromaDB/Ollama are not running
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Investigation summary (2026-05-14): this task originated from investigating the Shokunin project's "memory" feature and assessing reuse for DevoxxGenie.

Shokunin's memory feature (reference, NOT to be copied verbatim — it is Python): a standalone MCP server (`.pack/memory/mcp-server.py`) using an embedded `chromadb.PersistentClient` at `~/.shokunin/memory/chroma_db/`, single global collection `shokunin_memory`. Embeddings use ChromaDB's built-in ONNX `all-MiniLM-L6-v2` (zero-config). Three MCP tools: `store_context(text, tags[], project, session_id)`, `search_context(query, project?, tags?)` (top-k similarity, optional project `where` filter, post-filter by tags), `get_session_summary(session_id)`. Capture is LLM-driven: the agent is instructed (via CLAUDE.md) to `search_context` at session start and `store_context` at session end. Stores curated summaries, not transcripts. Metadata: tags (JSON string), project, session_id, timestamp.

Current DevoxxGenie state (from codebase exploration):
- ChromaDB: `ChromaEmbeddingService` wires LangChain4J `ChromaEmbeddingStore` to a Docker container (`chromadb/chroma:0.6.2`, container `devoxx-genie-chromadb`) managed by `ChromaDockerService` / `ChromaDBManager`. Collection name = sanitized project name. Data persisted via volume mount under `~/.cache/JetBrains/.../DevoxxGenie/chromadb/data-<projectHash>/`. Embedding model = Ollama `nomic-embed-text` (hardcoded in `ChromaEmbeddingService.getEmbeddingModel()`).
- Code RAG: `ProjectIndexerService` indexes project files only (recursive 500-char chunks, metadata filePath/lastModified/indexedAt). `SemanticSearchService` does similarity search with configurable minScore (default 0.7) / maxResults (default 10). `RagValidatorService` chains DockerValidator -> ChromeDBValidator -> OllamaValidator -> NomicEmbedTextValidator.
- Chat memory: `ChatMemoryService` = `MessageWindowChatMemory` over `InMemoryChatMemoryStore`, last N messages (default 10). `ChatMemoryManager` orchestrates system-message construction. `ConversationStorageService` persists conversations to SQLite (`conversations.db`) as PLAIN TEXT — no embeddings, no semantic recall, no cross-conversation retrieval. `Conversation` model holds id/timestamp/title/provider/model/costs/messages.
- RAG context injection: `MessageCreationService` (~lines 161-242) runs semantic search on the user prompt when RAG is active and wraps results in `<SemanticContext>` tags.
- MCP: `MCPService` / `MCPExecutionService` already provide a tool framework that could host memory tools.

Conclusion: the *concept* fills a real gap and is worth implementing; the *implementation* should reuse DevoxxGenie's existing ChromaDB + LangChain4J + Ollama + MCP stack rather than porting Shokunin's Python/embedded approach.

Recommended implementation split:
1. Generalize `ChromaEmbeddingService` (or add a sibling) so it can produce/manage a second, independently-named collection for memory; key it by project hash like the existing collection.
2. Add `ConversationMemoryService` with `storeMemory` / `searchMemory` / `getConversationSummary`, mirroring `SemanticSearchService` for the retrieval side. Keep it MCP-tool-shaped (clear input/output objects).
3. Add automatic summary generation hooked into the conversation-completion path (where `ChatService` writes history after `AppTopics.CONVERSATION_TOPIC`), generating the summary via the selected LLM provider off the EDT.
4. Add `/remember` and `/recall` prompt commands (follow the existing custom-prompt-command pattern under `service/prompt/command/`).
5. Inject retrieved memory into the conversation context in `MessageCreationService` using a distinct `<ConversationMemory>` wrapper, separate from `<SemanticContext>`.
6. Add settings (toggle + minScore/maxResults) to `DevoxxGenieStateService` and the RAG settings panel; gate behind `RagValidatorService`.
7. Add a "clear project memory" action.
8. Tests: mock ChromaDB/embedding-store interactions; verify collection isolation, store/search/summary behavior, and graceful degradation when prerequisites are absent.

Per CLAUDE.md workflow rules: create a feature/fix branch before any code changes; write a reproducing/behavioral test first where practical; do not expand scope into the embedded-vector-store evaluation (capture that as a separate follow-up task).
<!-- SECTION:NOTES:END -->
