---
id: TASK-216
title: RAG health & diagnostics panel in the Settings UI
status: To Do
assignee: []
created_date: '2026-05-26 18:00'
updated_date: '2026-05-26 18:00'
labels:
  - enhancement
  - RAG
  - UX
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsConfigurable.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifest.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/IndexManifestService.java
  - src/main/java/com/devoxx/genie/service/rag/manifest/JsonFileIndexManifest.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
  - src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Users currently have no way to see whether RAG is actually working for their project. The settings panel has the toggles and thresholds; the new "Show RAG Only" log shows per-query retrievals; but there's no single place to answer "is my index healthy, how big is it, when was it last updated, which files made it in?".

This task adds a small **RAG health/diagnostics** section to the existing RAG settings panel that surfaces the state of the index so users can self-diagnose retrieval problems before opening an issue.

**Investigation areas:**
- **Where it lives.** Add a "RAG Status" sub-section to `RAGSettingsComponent`, below the existing toggles and thresholds. Don't introduce a new tool window — one place to look for everything RAG.
- **What to show.**
  - Indexed file count (from the manifest).
  - Total chunk count (sum of `segmentCount` across manifest entries — already stored).
  - Last index time (max of `indexedAt`).
  - Embedding model in use, schema version, and warn if the manifest contains entries from older schemas (which would have been ignored by `isCurrent` anyway).
  - Manifest file path and size on disk.
  - ChromaDB collection name (so users can inspect with `chromadb` CLI tools if they want).
  - Validator status snapshot (Docker / Chroma / Ollama / embedding-model) — running the same checks as `RagValidatorService` and rendering a green/red row per check.
  - File-watcher status: ON/OFF, count of files reindexed in this session.
- **Actions.** Three buttons:
  - "Refresh" — re-run validators and recount the manifest.
  - "Open log panel" — opens the DevoxxGenie Logs tool window with the "Show RAG Only" filter preselected.
  - "Reset project index" — confirmation dialog, then wipes the manifest and the project's Chroma collection. Useful when the user changes embedding models or just wants a clean slate.
- **Refresh model.** Computing the stats requires a small manifest pass (cheap, in-memory). Refresh on panel open; manual refresh via the button. No automatic polling.
- **Headless-safe.** Validators and manifest reads must not block the EDT. Compute on a background thread, render to EDT via `invokeLater`.

This is the lowest-priority Phase 3 item — retrieval quality matters more than diagnostic UX — but it pays for itself the first time a user reports "RAG doesn't work" and we can ask them to screenshot this panel.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A "RAG Status" section is added to the existing RAG settings panel showing: indexed file count, total chunk count, last index time, embedding model + schema version, manifest path + on-disk size, ChromaDB collection name
- [ ] #2 A row per validator (Docker / ChromaDB / Ollama / embedding model) is rendered with green/red status, mirroring what `RagValidatorService` already computes — no duplicate validation logic, just a renderer
- [ ] #3 The file-watcher's status is shown: ON/OFF and the count of files reindexed in the current IDE session (small in-memory counter on `RAGFileWatcher`)
- [ ] #4 Three buttons work: "Refresh" re-runs validators and recounts the manifest; "Open log panel" opens DevoxxGenie Logs with the Show RAG Only filter preselected; "Reset project index" prompts for confirmation, wipes the manifest, drops the project's Chroma collection, and shows a notification when done
- [ ] #5 All computation runs off the EDT — opening the settings panel must never block the UI even if the manifest holds tens of thousands of entries
- [ ] #6 If RAG is disabled or no manifest exists yet, the section shows a friendly empty-state ("No index for this project yet — click Index Files to build one") rather than zeros and dashes
- [ ] #7 Unit tests cover the manifest-summary computation (file count, chunk count, last-indexed timestamp) on a synthetic manifest
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background from the Phase-1/2 RAG review: the review listed a "UI health surface" among Phase 3 items, scoped explicitly as a diagnostic aid not a redesign of the settings panel. This task implements that aid.

Suggested split:
- `RagHealthSnapshot` value object: holds everything to render. Computed by a `RagHealthService.snapshot(Project)` call on a background thread.
- `RAGSettingsComponent` gains a "RAG Status" `JPanel` with a small table for stats and a row of action buttons. Use existing IntelliJ UI components (`JBLabel`, `JBPanel`, etc.) to match the panel's look.
- Track the watcher's session reindex count via an `AtomicInteger` on `RAGFileWatcher`; expose a getter; reset on dispose.
- "Open log panel" should call `ToolWindowManager.getInstance(project).getToolWindow("DevoxxGenieLogs").show()` (or whatever the registered ID is) and somehow set the filter to `RAG_ONLY` — extend `AgentMcpLogPanel` to accept a default filter at construction or via a small static setter, and call it before `.show()`.

Out of scope: a real-time dashboard that auto-refreshes, charts/graphs, history of index changes. This is a snapshot view, not telemetry.

Low priority: ship this after task-212 (configurable embedding model) and the retrieval-quality tasks (213, 214, 215) — those move the needle for end users, this helps developers debug.
<!-- SECTION:NOTES:END -->
