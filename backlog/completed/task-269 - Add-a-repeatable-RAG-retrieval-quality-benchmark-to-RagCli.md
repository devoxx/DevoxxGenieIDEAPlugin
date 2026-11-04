---
id: TASK-269
title: Add a repeatable RAG retrieval-quality benchmark to RagCli
status: Done
assignee:
  - '@claude'
created_date: '2026-11-03T13:34:00Z'
updated_date: '2026-11-03T16:45:00Z'
labels:
  - RAG
  - testing
  - tooling
milestone: m-0
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/523
  - src/main/java/com/devoxx/genie/service/rag/cli/RagCli.java
modified_files:
  - src/test/resources/rag-chunking/fixtures/SampleIndexer.java
  - src/test/resources/rag-chunking/fixtures/SampleBubble.kt
  - src/test/resources/rag-chunking/fixtures/sample-agent-mode.md
  - src/test/resources/rag-chunking/fixtures/sample-workflow.yml
  - src/test/resources/rag-chunking/fixtures/sample.properties
  - src/test/java/com/devoxx/genie/service/rag/ChunkingCharacterizationTest.java
  - src/test/resources/rag-chunking/golden/SampleIndexer.java.json
  - src/test/resources/rag-chunking/golden/SampleBubble.kt.json
  - src/test/resources/rag-chunking/golden/sample-agent-mode.md.json
  - src/test/resources/rag-chunking/golden/sample-workflow.yml.json
  - src/test/resources/rag-chunking/golden/sample.properties.json
  - src/test/java/com/devoxx/genie/service/rag/chunking/RagChunkerTest.java
  - src/main/java/com/devoxx/genie/service/rag/chunking/RagChunker.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/test/java/com/devoxx/genie/service/rag/SemanticSearchServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/embedding/ProfiledEmbedderTest.java
  - src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingProfile.java
  - src/main/java/com/devoxx/genie/service/rag/embedding/ProfiledEmbedder.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/benchmark/RetrievalMetricsTest.java
  - src/main/java/com/devoxx/genie/service/rag/benchmark/RetrievalMetrics.java
  - src/test/resources/rag-benchmark/devoxxgenie-questions.json
  - src/main/java/com/devoxx/genie/model/ProjectScanDefaults.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/benchmark/BenchmarkDatasetTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/benchmark/BenchmarkCorpusTest.java
  - src/main/java/com/devoxx/genie/service/rag/benchmark/BenchmarkDataset.java
  - src/main/java/com/devoxx/genie/service/rag/benchmark/BenchmarkCorpus.java
  - src/test/java/com/devoxx/genie/service/rag/benchmark/RagBenchmarkTest.java
  - src/main/java/com/devoxx/genie/service/rag/benchmark/RagBenchmark.java
  - src/test/java/com/devoxx/genie/service/rag/cli/RagBenchmarkCommandTest.java
  - src/main/java/com/devoxx/genie/service/rag/cli/RagBenchmarkCommand.java
  - src/main/java/com/devoxx/genie/service/rag/cli/RagCli.java
  - src/main/java/com/devoxx/genie/service/rag/cli/RagBenchmarkMain.java
  - build.gradle.kts
  - src/test/resources/rag-benchmark/README.md
priority: medium
type: task
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The RAG configurable-embeddings epic (milestone m-0) changes the things that decide retrieval quality: embedding model, provider, query/document prefixes, and Matryoshka dimension truncation. The project currently has no way to measure retrieval quality. `RagCli` only offers ad-hoc `query` and `list` commands. So questions like "do nomic's `search_query:`/`search_document:` prefixes help on code?", "is jina-embeddings-v5-text-nano better than nomic-embed-text here?" or "how much does truncating to 256 dimensions cost?" can only be guessed.

The maintainer has already decided to turn nomic's prefixes on by default for all users (TASK-265), which forces a one-time re-index on upgrade. This benchmark gives a number for that change (useful in the upgrade notification and release notes) and an objective check for future model, prefix and dimension choices.

The benchmark runs outside the IDE like the existing `RagCli` (Gradle task, env-var configuration) against a real embedding endpoint (Ollama), keeping vectors in an in-memory store (maintainer decision, 2026-11-03). It must not touch ChromaDB or the user's existing project collections.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A versioned benchmark dataset of at least 20 natural-language questions about this repository is checked in, each paired with the file path(s) that a correct retrieval should return
- [x] #2 A RagCli benchmark command indexes the target project into its own throwaway in-memory store, runs every question, and reports recall@1, recall@5, recall@10 and MRR, plus a per-question pass/fail list
- [x] #3 The benchmark chunks and filters files with exactly the same code the plugin's indexer uses: the splitter selection and chunk quality filtering are moved out of the IntelliJ-dependent indexer service into an IDE-independent component used by both, with no duplicated chunking logic
- [x] #4 The plugin's indexing behaviour is unchanged by that move: the same files produce the same chunks as before, covered by a test
- [x] #5 For Ollama, the embedding model, base URL, query prefix, document prefix and output dimension are configurable per benchmark run, so two configurations can be compared on the same dataset (OpenAI-compatible endpoints are added later by TASK-265)
- [x] #6 Applying query/document prefixes and truncating + re-normalising to an output dimension is implemented in a component that the plugin's embedding path can reuse in TASK-265, not as benchmark-only code
- [x] #7 The benchmark never connects to ChromaDB and never reads, writes or deletes the plugin's real project collections; its vectors live only in memory for the duration of the run
- [x] #8 Results can be written in a machine-readable format (e.g. JSON) so runs can be compared over time
- [x] #9 A first comparison of nomic-embed-text without prefixes vs. with `search_query: ` / `search_document: ` on this repository is recorded in the task's final summary
- [x] #10 Unit tests cover the metric calculations (recall@k, MRR) on fixed synthetic rankings, and prefix application plus truncation/re-normalisation
- [x] #11 Usage is documented (how to run it, how to read the output, how to add questions)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Research findings (2026-11-03)
- Chunking lives in `ProjectIndexerService` (IntelliJ @Service): `splitterFor(Path)` (paragraph splitter for md/mdx/markdown, line splitter for code extensions, recursive 500/50 tokens otherwise) and `processPath` (split → `ChunkQualityFilter.isLowContent` → metadata). `ChunkQualityFilter` is already IDE-independent.
- File selection uses `ProjectScannerService` (VFS, .gitignore, settings), which can't run outside the IDE. Default scan extensions live in `DevoxxGenieStateService.includedFileExtensions`; excluded dirs in `excludedDirectories`.
- Retrieval (`SemanticSearchService.singleQuerySearch`): embed query → store search with minScore 0.7, maxResults 10, one result per chunk.
- `RagCli` builds its own Ollama + Chroma clients; its Chroma store doesn't set `ChromaApiVersion.V2` (the plugin does).
- Local env: Ollama has nomic-embed-text and bge-m3; ChromaDB image present but not running.

## Plan
1. **Pin current chunking (test first).** Characterisation test: for fixed sample files (Java, Kotlin, Markdown, YAML, a table-heavy Markdown file), record the chunks the current `ProjectIndexerService` produces. Must pass before and after the move (AC #4).
2. **Extract `RagChunker`** (new, `service/rag/chunking`, no IntelliJ imports): `List<TextSegment> chunk(Path, String content)` = splitter selection + quality filter. `ProjectIndexerService.processPath` delegates to it and only adds metadata. Remove the duplicate logic from the indexer (AC #3).
3. **`EmbeddingProfile` + `ProfiledEmbedder`** (new, `service/rag/embedding`): record (queryPrefix, documentPrefix, outputDimension) and a wrapper over a langchain4j `EmbeddingModel` with `embedQuery(String)` / `embedDocuments(List<TextSegment>)`. Prefixes are applied only to the text sent to the model, never to the returned segments. Optional truncation to the first k dimensions plus L2 re-normalisation, with an error if k > native dimension. Not wired into the plugin yet; TASK-265 does that (AC #6).
4. **Metrics** (`service/rag/benchmark/RetrievalMetrics`): file-level ranking (chunks deduplicated to first occurrence per file); hit@k = any expected file in top k; recall@1/5/10 = mean hit@k over questions; MRR = mean 1/rank of first expected file (0 if absent). No minScore filtering, so thresholds don't hide rank differences. Unit tests on synthetic rankings (AC #10).
5. **Dataset**: `src/test/resources/rag-benchmark/devoxxgenie-questions.json` with at least 25 questions over this repo (code and docs), each with expected repo-relative path(s). A unit test checks every expected path exists, so the dataset fails loudly when files move (AC #1).
6. **Benchmark runner** (`RagBenchmark` + `ragBenchmark` subcommand in `RagCli`, plus Gradle task `ragBenchmark`): corpus = `git ls-files` filtered by the plugin's default included extensions and excluded dirs (defaults exposed as constants from `DevoxxGenieStateService` so they aren't copied), chunk with `RagChunker`, embed documents with `ProfiledEmbedder` in batches of 64, store in a **throwaway store** (see decision below), run every question, print a per-question table plus summary, optionally write JSON (`--out`) with config, corpus stats, timings and metrics (AC #2, #5, #7, #8). Config via env vars/flags: Ollama URL, model, query prefix, document prefix, dimension, top-k.
7. **Docs**: `src/test/resources/rag-benchmark/README.md` covering how to run it, how to read the output, and how to add questions (AC #11).
8. **First comparison**: run nomic-embed-text without prefixes vs. with `search_query: ` / `search_document: `, and record the results in the final summary (AC #9).

## Open decision
Throwaway store: in-memory langchain4j `InMemoryEmbeddingStore` (cosine; needs only Ollama; can't touch real collections) vs. a temporary ChromaDB collection (needs Docker/Chroma; matches production storage). Recommendation: in-memory by default.

## Decision resolved (2026-11-03)
Throwaway store: in-memory `InMemoryEmbeddingStore` (maintainer's choice). A temporary ChromaDB collection is out of scope for now.

## Deviations from the plan
- Defaults are shared via a new plain `ProjectScanDefaults` class rather than constants on `DevoxxGenieStateService`, because the state service can't be loaded outside the IDE.
- The benchmark command lives in `RagBenchmarkCommand` (unit-testable option parsing); `RagCli benchmark` dispatches to it, and Gradle task `ragBenchmark` uses `RagBenchmarkMain`.
- README lives next to the dataset (`src/test/resources/rag-benchmark/README.md`).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-03: This is the starting point of milestone m-0. It runs first so every later change (TASK-212 model choice, TASK-265 prefixes/dimensions/provider) can be measured against a baseline. Scope is Ollama-only on purpose so it has no dependency on TASK-265; TASK-265 adds the OpenAI-compatible provider to the benchmark and reuses the prefix/truncation component built here.

2026-11-03 decision (maintainer): the benchmark uses an in-memory langchain4j InMemoryEmbeddingStore for now, not a temporary ChromaDB collection. ACs #2 and #7 reworded accordingly. It measures retrieval quality (model/prefix/dimension), not the Chroma storage layer.

Steps 1-2 done. ChunkingCharacterizationTest pins the indexer's chunks for 5 fixture files (copies of real repo files under src/test/resources/rag-chunking/fixtures); goldens were generated from the indexer before the extraction, and regenerate only with UPDATE_RAG_GOLDEN=true. RagChunker (service/rag/chunking, no IntelliJ imports) now owns splitter selection + ChunkQualityFilter; ProjectIndexerService.processPath delegates and only adds metadata. splitterIsChosenByFileExtension moved from SemanticSearchServiceTest to RagChunkerTest. Minor: extension lower-casing now uses Locale.ROOT (was default locale), and the 'Dropped N low-content chunk(s)' debug log was removed. Fixture copies of repo files must be excluded from the benchmark corpus so they don't compete with the originals.

Steps 3-8 done. EmbeddingProfile/ProfiledEmbedder (service/rag/embedding); gotcha: the IntelliJ build instruments @NotNull with runtime null checks, so the record's null-to-empty normalisation only works without @NotNull on its components. ProjectScanDefaults (model/) now holds the scanner's default extensions/excluded dirs, because DevoxxGenieStateService can't load outside the IDE (the IntelliJ platform is not on the JavaExec runtime classpath); the state service seeds its settings from it. RetrievalMetrics, BenchmarkDataset (28 questions), BenchmarkCorpus, RagBenchmark (InMemoryEmbeddingStore, minScore 0, top 50 chunks collapsed to files), RagBenchmarkCommand (`RagCli benchmark`, plus Gradle task `ragBenchmark` via RagBenchmarkMain, because --args replaces preset args). Report output uses Locale.ROOT (the first run printed decimal commas).

First comparison, 2026-11-03, commit 410f280c + this branch's uncommitted work, nomic-embed-text:latest on local Ollama, 1414 files / 18,657 chunks, both runs on an identical corpus. No prefixes: recall@1 0.143, recall@5 0.643, recall@10 0.714, MRR 0.359, 3 complete misses. Nomic prefixes (search_query: / search_document: ): recall@1 0.250, recall@5 0.643, recall@10 0.821, MRR 0.425, 1 complete miss. Per question: 13 better, 8 worse, 7 unchanged. Biggest gains: rag-reindex-on-save 11->2, sub-agent-isolation 13->3, register-llm-provider 32->13, two misses now found (rank 5 and 11). Biggest regression: analytics-privacy 1->7. Caveat: 28 questions, so recall@1 +0.107 is 3 questions; the direction is consistent (MRR +18%) but this is not a large sample. Indexing ~140 s per run.

Review fixes: --prefix-preset now beats prefix env vars (flags > env, as documented), with a test that failed first; RagBenchmark reuses IndexerConstants.FILE_PATH. Verification: full suite 3877 tests, 22 skipped, 0 failures; verifyPlugin Compatible against IU-253.31033.145 and IU-261.22158.277; ragBenchmark run end-to-end twice against local Ollama.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## What changed
A repeatable RAG retrieval-quality benchmark, `./gradlew ragBenchmark` (also `RagCli benchmark`). It indexes this repository into an in-memory store with the plugin's own chunker, asks 28 checked-in questions, and reports recall@1/5/10 and MRR per file, with a per-question pass/fail list and optional JSON output. It never touches ChromaDB.

Building it required pulling three pieces out of IDE-bound code. Each is reusable by later epic tasks:
- **`RagChunker`** (`service/rag/chunking`): splitter selection + `ChunkQualityFilter`, now used by both `ProjectIndexerService` and the benchmark. Behaviour is pinned by `ChunkingCharacterizationTest`: its golden chunks for 5 real-file fixtures were captured from the indexer *before* the extraction and still match.
- **`EmbeddingProfile` + `ProfiledEmbedder`** (`service/rag/embedding`): query/document prefixes that apply only to text sent to the model, plus truncate-and-re-normalise to an output dimension (error if larger than native). Ready for TASK-265 to wire into the plugin.
- **`ProjectScanDefaults`** (`model/`): the scanner's default extensions/excluded dirs, now the single source for `DevoxxGenieStateService` and the benchmark.

## First comparison (AC #9)
nomic-embed-text:latest on local Ollama, 2026-11-03. Corpus: the 1414 tracked files at commit 410f280c with this branch's edits to tracked files (new untracked files weren't indexed), 18,657 chunks; identical for both runs.

| | recall@1 | recall@5 | recall@10 | MRR | complete misses |
|---|---|---|---|---|---|
| no prefixes (today's plugin) | 0.143 | 0.643 | 0.714 | 0.359 | 3 |
| `search_query: ` / `search_document: ` | 0.250 | 0.643 | 0.821 | 0.425 | 1 |

Per question: 13 better, 8 worse, 7 unchanged. Largest gains: rag-reindex-on-save 11→2, sub-agent-isolation 13→3, register-llm-provider 32→13. Largest regression: analytics-privacy 1→7. This supports the option-1 decision (prefixes on by default, TASK-265), but the sample is small: recall@1 +0.107 is 3 questions.

## Verification
- Full test suite: 3877 tests, 22 skipped, 0 failures.
- `verifyPlugin`: Compatible against IU-253.31033.145 and IU-261.22158.277.
- Both benchmark configurations run end to end.

## Risks / follow-ups
- Numbers depend on the working tree (`git ls-files`), so they will shift once this branch is committed; compare runs on the same commit.
- The corpus approximates the IDE scanner (defaults + git-tracked files) and ignores user scan settings.
- Small behaviour notes in the indexer: extension lower-casing now uses `Locale.ROOT`, and a debug log line for dropped low-content chunks was removed.
- Not done here: OpenAI-compatible endpoints in the benchmark (TASK-265, AC #15).

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1373
<!-- SECTION:FINAL_SUMMARY:END -->
