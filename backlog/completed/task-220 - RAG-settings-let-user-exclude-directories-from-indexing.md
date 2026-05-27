---
id: TASK-220
title: 'RAG settings: let user exclude directories from indexing'
status: Done
assignee: []
created_date: '2026-05-26 20:35'
updated_date: '2026-05-27 09:23'
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
- [x] #1 A new "Excluded directories" section is visible on Settings → RAG with an add/remove UI (or comma-separated input) backed by a new `ragExcludedDirectories` setting in `DevoxxGenieStateService`
- [x] #2 `RAGSettingsConfigurable.isModified/apply/reset` correctly round-trip the new setting
- [x] #3 `ProjectIndexerService.indexFiles` skips any file whose path contains a directory segment matching an entry in `ragExcludedDirectories` (in addition to existing project-scanner exclusion)
- [x] #4 `ProjectIndexerService.reindexFiles` (file-watcher path) honors the same exclusion list
- [x] #5 Existing RAG help text is updated to reflect the new RAG-specific exclusion list (no longer claiming the indexer *only* uses Scan & Copy Project settings)
- [x] #6 Unit tests cover: (a) state-service round-trip; (b) indexer filtering with at least one excluded entry; (c) entries that don't match are still indexed
- [x] #7 No regression when `ragExcludedDirectories` is empty — indexing behaves exactly as before
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Decisions

- **Dedicated list, empty default.** Adding a copy of the existing `excludedDirectories` would create a dual-maintenance pitfall when the user updates one and not the other. Empty default means zero behavior change unless the user opts in.
- **Segment-based match.** Mirrors `FileScanner.shouldExcludeDirectory` which uses `excludedDirectories.contains(file.getName())`. Substring matches (e.g. "generated" matching "generated-sources") are explicitly *not* allowed; pinned by a test.
- **No auto-reindex on save.** The help text says changes take effect on the next "Start Indexing" or file-watcher reindex. Avoids surprise long-running tasks.

## Implementation

- `DevoxxGenieStateService`: new field `private List<String> ragExcludedDirectories = new ArrayList<>()`. Lombok `@Getter`/`@Setter` cover the accessors.
- `ProjectIndexerService`: package-private static helper `isRagExcluded(Path, List<String>)` iterates path segments and returns true on first match. Wired into:
  - `indexFiles()` — after `scanProject` returns, filter `filesToProcess` with `removeIf(p -> isRagExcluded(p, ragExcluded))`. Log skip count.
  - `reindexFiles()` — if a tracked file matches the exclusion list, remove its existing chunks + mark removed in the manifest so stale matches don't linger.
- `RAGSettingsComponent`: added private static `RagExcludedDirectoriesPanel extends AddEditRemovePanel<String>` (same pattern as the project-scanner's panel in `CopyProjectSettingsComponent`). Wired into a new sub-row inside `addRAGSettingsSection`, with help text describing semantics + the "no auto-reindex" behavior. Updated `addInfoLabel` to mention both exclusion lists are honored.
- `RAGSettingsConfigurable`: round-tripped via `isModified` / `apply` / `reset`. Defensive `ArrayList` copies on both directions so the panel's mutable backing list and the persistent state never share a reference.

## Tests

- `ProjectIndexerServiceTest` (new) — 8 focused tests on the `isRagExcluded` helper: match, non-leaf segment, case-sensitivity, no-match, empty list, partial-substring NOT matched, multi-entry, and a pin on leaf-segment behavior.
- `DevoxxGenieStateServiceTest` — added empty-default + round-trip tests for `ragExcludedDirectories`.
- Full Gradle test suite: BUILD SUCCESSFUL (2m 43s).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java` — added `ragExcludedDirectories: List<String>` (empty default).
- `src/main/java/com/devoxx/genie/service/rag/ProjectIndexerService.java` — added `isRagExcluded(Path, List<String>)` helper + filtering in `indexFiles` and `reindexFiles`. The reindex path removes chunks for newly-excluded files so stale matches don't linger.
- `src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java` — added `RagExcludedDirectoriesPanel` (AddEditRemovePanel<String>), wired into the RAG section, updated info-label help text. The edit dialog now includes an optional **Browse...** button that opens IntelliJ's directory chooser (scoped to the project root); the chosen directory's name (last path segment) is inserted into the still-editable text field. Manual typing remains supported — Browse is purely a convenience.
- `src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsConfigurable.java` — round-trip in `isModified`, `apply`, `reset` with defensive `ArrayList` copies.
- Tests: new `ProjectIndexerServiceTest` (8 cases) + 2 new tests in `DevoxxGenieStateServiceTest` for empty default + round-trip.
- Full Gradle suite: BUILD SUCCESSFUL.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a RAG-specific directory exclusion list to Settings → RAG, layered on top of the existing "Scan & Copy Project" exclusions so users can keep project context broad while keeping RAG narrow.

**Implementation:**
- `DevoxxGenieStateService`: new `ragExcludedDirectories: List<String>` field, empty default (zero behavior change unless the user opts in).
- `ProjectIndexerService`: new `isRagExcluded(Path, List<String>, Path)` helper supporting three match modes — absolute-path prefix, project-relative prefix, and segment match for single-token entries (parity with the project-scanner's existing behavior). Wired into `indexFiles()` (filters scan result + sweeps already-indexed files now covered by exclusions) and `reindexFiles()` (drops chunks for files that fell under an exclusion since last index).
- `IndexManifest`: added `trackedPaths()` to enable the retroactive sweep without exposing internal storage.
- `RAGSettingsComponent`: new `RagExcludedDirectoriesPanel extends AddEditRemovePanel<String>` with an optional Browse… button scoped to the project root. Help text updated to describe both exclusion lists.
- `RAGSettingsConfigurable`: round-trips the new setting with defensive ArrayList copies.

**Tests:** New `ProjectIndexerServiceTest` covers the matcher (match/no-match/case-sensitive/empty/multi-entry/segment behavior); `DevoxxGenieStateServiceTest` gains empty-default + round-trip tests. Full Gradle suite green.</finalSummary>
</invoke>
<!-- SECTION:FINAL_SUMMARY:END -->
