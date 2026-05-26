---
id: TASK-220
title: 'RAG settings: let user exclude directories from indexing'
status: To Do
assignee: []
created_date: '2026-05-26 20:35'
labels:
  - rag
  - ui
  - settings
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsConfigurable.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java
  - >-
    src/main/java/com/devoxx/genie/service/projectscanner/ProjectScannerService.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

Today the RAG indexer (`ProjectIndexerService.indexFiles`) reuses the global "Scan & Copy Project" exclusion list (`DevoxxGenieStateService.excludedDirectories`, default: `build, .git, bin, out, target, node_modules, .idea`). The RAG settings page (`Settings → RAG`) help text already states this:

> "The indexer uses the 'Scan & Copy Project' settings to exclude specific directories, files, and extensions..."

There is no way for a user to exclude a directory from RAG indexing **specifically** without also hiding it from the project-context scanner. In practice users want RAG to skip heavier or noisier trees (e.g. `docs/generated/`, `dist/`, large test fixtures, vendor copies) without changing the project-scan behavior.

## Goal

Add a directory-exclusion list directly inside the RAG settings page (`Settings → RAG`) so users can manage which directories are excluded from RAG indexing without leaving the panel.

## Suggested Approach

1. **Setting**: Add `ragExcludedDirectories: List<String>` to `DevoxxGenieStateService` (defaults can mirror the existing `excludedDirectories`).
2. **UI**: In `RAGSettingsComponent`, add a new "Excluded directories" sub-section with:
   - A list/table widget (e.g. `JBList` with add/remove buttons, or a comma-separated text area for simplicity).
   - Help text explaining matching is by directory **name** anywhere under the project root, not by absolute path.
3. **Wiring**: `RAGSettingsConfigurable.isModified/apply/reset` must round-trip the new setting.
4. **Indexer hookup**: `ProjectIndexerService.indexFiles` (and the file watcher's `reindexFiles`) must filter out paths whose any segment matches an entry in `ragExcludedDirectories`. Apply this *in addition to* the existing project-scanner exclusion — not as a replacement, so users don't lose the existing defaults.
5. **Help-text correction**: Update the existing RAG help text since the indexer will no longer rely *only* on the project-scan settings.

## Open Questions (resolve in plan step)

- Single shared list vs. dedicated RAG list? Recommend **dedicated** so users can keep project context broad while keeping RAG narrow.
- Match by directory name (segment match) vs. glob? Recommend segment match for parity with the existing project-scanner behavior.
- Should toggling this setting offer to re-index automatically, or just notify the user that next index will reflect the change? Recommend the latter to avoid surprise long-running tasks.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new "Excluded directories" section is visible on Settings → RAG with an add/remove UI (or comma-separated input) backed by a new `ragExcludedDirectories` setting in `DevoxxGenieStateService`
- [ ] #2 `RAGSettingsConfigurable.isModified/apply/reset` correctly round-trip the new setting
- [ ] #3 `ProjectIndexerService.indexFiles` skips any file whose path contains a directory segment matching an entry in `ragExcludedDirectories` (in addition to existing project-scanner exclusion)
- [ ] #4 `ProjectIndexerService.reindexFiles` (file-watcher path) honors the same exclusion list
- [ ] #5 Existing RAG help text is updated to reflect the new RAG-specific exclusion list (no longer claiming the indexer *only* uses Scan & Copy Project settings)
- [ ] #6 Unit tests cover: (a) state-service round-trip; (b) indexer filtering with at least one excluded entry; (c) entries that don't match are still indexed
- [ ] #7 No regression when `ragExcludedDirectories` is empty — indexing behaves exactly as before
<!-- AC:END -->
