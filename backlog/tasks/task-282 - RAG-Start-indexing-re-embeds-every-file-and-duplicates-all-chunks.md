---
id: TASK-282
title: 'RAG: Start indexing re-embeds every file and duplicates all chunks'
status: In Progress
assignee: []
created_date: '2026-11-26T13:43:00Z'
updated_date: '2026-11-26T14:06:00Z'
labels:
  - rag
  - bug
  - indexing
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
modified_files:
  - >-
    src/test/java/com/devoxx/genie/service/rag/ProjectIndexerIncrementalTest.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - docusaurus/docs/features/rag.md
priority: high
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Settings → RAG → Start indexing calls `ProjectIndexerService.indexFiles(project, true, …)`. The `forceReindex = true` flag skips the manifest's content-hash check, so every run re-embeds all files (about 1,650 in this project), even when nothing changed.

Worse, a full index never removes a file's existing chunks before adding new ones: `indexFile` → `processPath` → `storeSegments` adds the chunks under random ids. Only the file watcher's `reindexFiles` calls `removeChunksFromStore` first. So each full run adds another copy of every chunk.

Measured on 2026-11-26 in this project's Chroma collection: 50,393 chunks stored, but only 22,373 distinct (file, chunk) pairs, so 28,020 extra copies (56% of the index). 10,673 chunks are stored 3× and 6,329 2×, matching the full runs on 4 Nov, 18 Nov and 26 Nov. Search results then repeat the same chunk, wasting RAG context.

Also, a full index doesn't remove chunks of files that were deleted or newly excluded since the last run, unless the file watcher saw the deletion.

Fix: make Start indexing incremental (hash check on), always replace a file's chunks instead of appending, prune entries for files that are gone or excluded, and add an explicit "Rebuild index" that wipes the collection and manifest first. That rebuild is also the one-time cleanup for existing duplicates.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Running Start indexing twice without file changes re-embeds no files the second time and leaves the chunk count unchanged
- [x] #2 Re-indexing a changed file, on any path (full index, rebuild, file watcher), replaces that file's chunks instead of adding a second copy
- [x] #3 A full index removes the chunks and manifest entries of files that no longer exist or are now excluded
- [x] #4 RAG settings offer a separate Rebuild index action that clears the collection and manifest, then indexes everything; Start indexing stays incremental
- [x] #5 The progress/status text says how many files were indexed, skipped as unchanged and removed
- [x] #6 Unit tests cover: second run is a no-op, changed file has no duplicate chunks, deleted/excluded file is pruned, rebuild clears first; written failing first
- [ ] #7 Verified manually: after one Rebuild index, the project's collection has no duplicate (file, chunk) pairs, and a following Start indexing finishes quickly with 0 re-embedded files
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Reproducing tests first (ProjectIndexerService with a fake/in-memory EmbeddingStore + manifest): second full run is a no-op; a changed file has no duplicate chunks; a deleted/excluded file is pruned; rebuild clears first.
2. indexFile: when a file is (re)indexed, remove its existing chunks (removeChunksFromStore) before storing new ones, so no path can duplicate.
3. indexFiles: Start indexing passes forceReindex=false (incremental); after the scan, prune manifest-tracked files that are no longer in the scanned set (deleted or excluded).
4. Rebuild index: new entry point that wipes the collection (removeAll()) and the manifest, then indexes everything; RAG settings get a Rebuild index button next to Start indexing.
5. Report indexed / unchanged / removed counts in the progress label.
6. Run RAG tests + full suite; manual check: Rebuild once, then Start indexing re-embeds 0 files and the collection has no duplicates.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation 2026-11-26:
- ProjectIndexerService.indexFile now returns a FileOutcome and replaces a tracked file's chunks (removeChunksFromStore) before storing new ones, on every path. The watcher's reindexFiles no longer removes them separately.
- indexFiles(project, rebuild, …): Start Indexing passes rebuild=false, so the content-hash check applies; rebuild=true calls clearIndex() (removeAll + manifest.clear) first. Files are never indexed with force anymore.
- pruneFilesNotIn(files) after the scan and RAG exclusions removes deleted, excluded or ignored files; it replaces the task-220 exclusion sweep. An empty scan prunes nothing.
- indexFiles returns IndexRunSummary (indexed / unchanged / removed / failed); the settings notification shows it.
- RAG settings: new Rebuild Index button (with confirmation) next to Start Indexing; tooltips explain the difference. RagIndexStaleNotifier keeps passing true, which now means rebuild.
- The cancellation flag is no longer reset in indexFiles' finally block, so the settings panel can report a cancelled run; each run resets it on entry.
- Tests: ProjectIndexerIncrementalTest (6 tests; 4 failed before the fix). RAG + settings + chromadb tests: 330 pass.
- Docs: rag.md, Indexing Your Project and troubleshooting.

Review fix 2026-11-26: indexFile now embeds first and replaces after (embedSegments + addAll). Old chunks are only removed once the new embeddings exist. If nothing could be embedded (e.g. the embedding server is down), the file keeps its chunks and manifest entry (FAILED), so the watcher still retries it later. A file that became blank loses its chunks and is untracked. Pruning is also skipped when the scan was cancelled through the IDE progress indicator. Two more tests (8 in ProjectIndexerIncrementalTest); RAG test groups: 332 pass. Full suite not run yet.

Full ./gradlew test --rerun (JDK 21) passes on the branch rebased onto master (2026-11-26). AC #7 (manual rebuild + duplicate check) still open.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1451
<!-- SECTION:NOTES:END -->
