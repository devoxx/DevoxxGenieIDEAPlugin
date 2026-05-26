---
id: TASK-215
title: AST-aware code chunking via the existing language analyzers
status: To Do
assignee: []
created_date: '2026-05-26 18:00'
updated_date: '2026-05-26 18:00'
labels:
  - enhancement
  - RAG
  - retrieval-quality
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/service/analyzer/ProjectAnalyzer.java
  - src/main/java/com/devoxx/genie/service/analyzer/languages/
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/564'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 2 added a per-extension splitter that picks line-based vs. paragraph-based splitting based on file extension. That's a substantial win over character-recursive splitting, but it still doesn't respect **semantic** code boundaries: a method can be sliced across two chunks, a class header can end up separated from its body, and Javadoc can land in a different chunk than the method it documents.

The plugin already has language-aware AST scanners under `service/analyzer/languages/` (Java, Kotlin, Python, JavaScript, Go, Rust, C++, PHP), coordinated by `ProjectAnalyzer`. This task uses those analyzers to chunk on **AST boundaries** — one chunk per method (or class for small classes, with method-level fallback when a class exceeds the token budget) — and adds metadata that makes retrieval much more precise.

**Investigation areas:**
- **Analyzer reuse.** The scanners currently extract context for prompt augmentation (parent classes, field references). Audit whether they expose a structured-enough view to produce chunk boundaries directly, or whether we need a thin adapter that walks the same data and emits `(symbolName, symbolKind, startLine, endLine, text)` tuples.
- **Chunk-per-method, with class fallback for tiny classes.** Default: every method becomes one chunk with its Javadoc/docstring attached. Classes shorter than `CHUNK_SIZE_TOKENS` become a single chunk. Top-level functions in Python/JS/Go follow the same rule.
- **Metadata for precision.** Each chunk should carry `symbolName`, `symbolKind` (`method` | `class` | `function` | `field`), `parentClass` (nullable), `startLine`, `endLine`, and the existing `FILE_PATH` / `LAST_MODIFIED`. Retrieval can use these for richer prompt rendering ("`AuthService.authenticate (lines 42-87)`") and future symbol-aware reranking.
- **Fallback for very long methods.** A 5000-line generated method shouldn't become a 5000-line chunk. If a method exceeds `CHUNK_SIZE_TOKENS * 2`, fall back to the line splitter for that method only and carry the same symbol metadata across the resulting sub-chunks.
- **Non-code files keep the Phase-2 routing.** Markdown still goes to paragraph splitter; everything else to the recursive default. Only the languages with AST scanners get this treatment.
- **Schema bump.** Adding new metadata fields and changing chunk semantics is a schema break for the manifest. Bump `CURRENT_EMBEDDING_SCHEMA_VERSION` to `v3` (or align with the model-versioning chosen in task-212) so existing v2 chunks are ignored and a re-index is prompted.
- **Performance.** AST parsing per file is heavier than line splitting. The Phase-2 parallel indexer (`INDEXING_PARALLELISM = 2`) should absorb most of it, but measure on a representative project; if regressions exceed ~30% indexing time, parallelism may need to bump.

This is the biggest retrieval-quality lever still on the table after Phase 1+2. Done well, it makes "where is `AuthService.authenticate` defined?" return exactly that method, not a 500-token slice that may or may not contain the signature.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 For languages with an existing analyzer (Java, Kotlin, Python, JavaScript, Go, Rust, C++, PHP), `ProjectIndexerService.splitterFor` produces one chunk per method (or per small class) with attached Javadoc/docstring; methods over 2× `CHUNK_SIZE_TOKENS` fall back to line-splitting while preserving symbol metadata
- [ ] #2 Each chunk carries `symbolName`, `symbolKind` (`method` / `class` / `function` / `field`), `parentClass` (nullable), `startLine`, `endLine` in its metadata, alongside the existing `FILE_PATH` / `LAST_MODIFIED` / `INDEXED_AT` / schema version
- [ ] #3 Non-source files (markdown, generic text, etc.) keep the Phase-2 splitter routing — only the listed language extensions use AST chunking
- [ ] #4 The embedding schema is bumped (e.g. `v3`) so existing v2 chunks are invalidated and the indexer prompts the user for a re-index — this is communicated in the UI, not silent
- [ ] #5 The "Show RAG Only" log entries (and the prompt rendering in `MessageCreationService`) include symbol-aware location info: `File: Foo.java (AuthService.authenticate, lines 42-87)` rather than just the file path
- [ ] #6 Indexing throughput regression on a representative project is documented; if it exceeds ~30% the parallelism default is reconsidered, with the chosen number recorded in the commit message
- [ ] #7 Unit tests cover: each supported language produces method-level chunks for a fixture file, oversized methods fall back to line splitting with retained symbol metadata, a class smaller than the chunk budget produces a single class-scoped chunk, and metadata fields are populated correctly
- [ ] #8 Files in unsupported languages or when an analyzer throws are handled gracefully: fall back to the Phase-2 per-extension splitter and log a `WARN` once per file
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background from the Phase-1/2 RAG review: AST chunking was called out as the biggest retrieval-quality lever still on the table after Phase 1+2 — bigger than reranking, bigger than hybrid retrieval. The Phase-2 per-extension splitter was an explicit cheap-first-pass that bought time for this proper version.

The existing analyzers under `service/analyzer/languages/` are already loaded for prompt-context extraction; reuse them rather than introducing a new tree-sitter or JavaParser dependency. If their public surface doesn't expose enough structure to drive chunking directly, a thin internal adapter is fine.

Suggested skeleton:
- `SymbolChunk` record: `(String symbolName, SymbolKind kind, String parentClass, int startLine, int endLine, String text)`.
- `AstChunker` interface: per-language impl; takes `Path` + file content, returns `List<SymbolChunk>`.
- `AstChunkerRegistry`: maps extension → chunker; falls back to the Phase-2 splitter for misses.
- In `ProjectIndexerService.splitterFor`, when an AST chunker exists for the extension, use it; otherwise the Phase-2 path.
- In `processPath`, propagate `SymbolChunk` metadata into the segment `Metadata` written to Chroma + manifest.

Open question to resolve in implementation: should the manifest track per-symbol entries (one row per method) or stay one row per file? Per-file is simpler and matches the current code; per-symbol unlocks "this method was deleted but the file still exists" precision. Recommended: stay per-file for v3, revisit later.

Out of scope: language-server-protocol-based chunking, symbol-aware reranking (could be a follow-up that consumes the metadata this task adds).
<!-- SECTION:NOTES:END -->
