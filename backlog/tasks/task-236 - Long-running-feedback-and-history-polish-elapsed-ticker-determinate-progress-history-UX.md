---
id: TASK-236
title: >-
  Long-running feedback & history polish: elapsed-time ticker, determinate
  progress, history UX
status: Done
assignee: []
created_date: '2026-06-10 12:00'
updated_date: '2026-06-11 06:56'
labels:
  - enhancement
  - UX
  - polish
dependencies:
  - TASK-233
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Bundle of smaller feedback gaps around long-running operations and the conversation-history popup.

1. **Elapsed-time ticker on running tools.** When the agent runs `run_command` / `run_tests`, nothing updates until the command finishes — a 90-second Gradle run looks frozen. Building on task-233's per-row RUNNING status, append a live elapsed-time suffix ("running… 12s") to rows whose status is RUNNING for >2s. One shared 1s Compose `LaunchedEffect` ticker per visible Activity section (not per row, and nothing ticking when no row is RUNNING).
2. **Determinate RAG-indexing progress.** `ProjectIndexerService` knows the candidate file list before embedding begins, but progress surfaces as indeterminate "Working…". Use a determinate `ProgressIndicator` (n/total files, current file name as text2) so users can judge whether indexing a large project is worth the wait. Same for the project scanner where the file count is known up front.
3. **Conversation history popup UX.** `ConversationHistoryPanel` is a plain JTable: no hover feedback, and the per-row delete button removes a conversation immediately with no confirmation and no undo. Add: row hover highlight (ListTableModel/renderer with hover row tracking), a confirmation for delete (or better, an undoable notification "Conversation deleted — Undo" that defers actual SQLite deletion a few seconds), and keyboard support (Enter opens selected row, Delete key deletes).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 An Activity row in RUNNING state for more than ~2s shows a live elapsed-time counter updating about once per second; the ticker stops when no row is running (no idle CPU churn)
- [x] #2 RAG indexing shows determinate progress (files processed / total, current file name) in the IntelliJ progress UI; cancellation via the progress indicator still works
- [x] #3 Project scanning shows determinate progress where the file count is known up front
- [x] #4 Conversation-history rows show a hover state; Enter opens the hovered/selected conversation; Delete key triggers deletion
- [x] #5 Deleting a conversation requires confirmation OR is undoable for a few seconds before the SQLite row is actually removed; accidental single-click can no longer destroy history silently
- [x] #6 All new background work stays off the EDT; progress text updates go through the ProgressIndicator API, not invokeLater loops
- [x] #7 Unit tests for the undo-deferred deletion logic (delete → undo restores, delete → timeout actually removes from ConversationStorageService)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Elapsed ticker depends on task-233's `ActivityEntryUiModel.status` field — schedule after it. Store `startedAt` on the entry at TOOL_REQUEST time; the ticker only forces recomposition of the suffix text, so derive the label rather than mutating the entry each second.
- Indexing progress: wrap the existing loop in `ProjectIndexerService.indexFiles()` with `ProgressManager`/`Task.Backgroundable`, `indicator.setFraction(i / (double) total)`, `indicator.setText2(file.name)`. Check current threading — if indexing already runs under an indicator, this is only setFraction/setText2 calls.
- For streaming intermediate stdout of run_command into the chat: explicitly out of scope (large payloads in Compose state, needs truncation design); the elapsed ticker is the 80/20.
- History undo: simplest robust pattern is a pending-deletion set + IntelliJ `Notification` with an "Undo" action; commit deletion on notification expiry. Avoid actually deleting then re-inserting (FK/ordering risks).
- Hover in JTable: track row under mouse via MouseMotionListener, repaint, renderer checks hovered row — standard IntelliJ pattern, see `JBTable` usages elsewhere in the codebase.
- Out of scope: redesigning the history popup into Compose, search within history, pagination.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented and merged via PR #1110 (commits 502831ae + review follow-up d0e28cfa).

1. Elapsed-time ticker — ActivitySection.kt + ConversationViewModel.kt / MessageUiModel.kt: per-row RUNNING/COMPLETED/ERROR status with one shared 1s ticker per visible Activity section appending a "running… Ns" suffix to rows running >2s; ticker stops when nothing is running. Tests: ActivitySectionElapsedTest.kt, ConversationViewModelTest.kt.

2/3. Determinate progress — ProjectIndexerService.java and ProjectScannerService.java run under a cancellable Task.Backgroundable with setFraction + setText2(file name). Follow-up d0e28cfa split scan+embed into a single monotonic 0.0→0.5 (scan) / 0.5→1.0 (embed) sweep via PhaseScalingIndicator so the bar no longer snaps back, plus a max-guard against parallel-worker progress rollback. All background work stays off the EDT through the ProgressIndicator API.

4/5/7. History popup UX — ConversationHistoryPanel.java: row hover highlight, Enter opens selected, Delete key deletes; deletion is undo-deferred via PendingConversationDeletionManager.java (commits to SQLite only after a grace period, with an Undo notification action). Tests: PendingConversationDeletionManagerTest.java (undo restores / timeout removes / race).
<!-- SECTION:FINAL_SUMMARY:END -->
