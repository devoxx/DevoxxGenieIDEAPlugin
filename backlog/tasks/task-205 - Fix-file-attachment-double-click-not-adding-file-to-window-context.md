---
id: TASK-205
title: Fix @ file attachment double-click not adding file to window context
status: In Progress
assignee: []
created_date: '2026-03-10 16:56'
updated_date: '2026-03-10 17:39'
labels:
  - bug
  - ui
  - file-context
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/FileListManager.java
  - src/main/java/com/devoxx/genie/ui/panel/PromptContextFileListPanel.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When using the `@` file attachment feature and double-clicking on a file in the file picker, the selected file is no longer added to the prompt/window context. Double-clicking a file should add it to the context file list (PromptContextFileListPanel) so it gets included in the prompt sent to the LLM.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Double-clicking a file in the @ file picker adds it to the window context (PromptContextFileListPanel)
- [ ] #2 The added file appears in the context file list and is included in subsequent prompts
- [ ] #3 No regression in other file attachment methods (drag-and-drop, manual add)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Root Cause

`FileSelectionPanelFactory.addSelectedFile()` calls `FileListManager.addFile(project, selectedFile)` **without passing `tabId`**. The `PromptContextFileListPanel` registers its observer with a specific `tabId`, so the file gets added to a null-tab bucket that nobody observes — the UI is never notified.

All other file addition paths (`AddFileAction`, `AddSnippetAction`, `AddDirectoryAction`, drag-and-drop) correctly use `ConversationTabRegistry.getInstance().getActiveTabId(project)`.

## Fix

1. **`FileSelectionPanelFactory.addSelectedFile()`** — get active tab ID from `ConversationTabRegistry` and pass it to both `contains()` and `addFile()`
2. **`FileSelectionPanelFactoryTest`** — mock `ConversationTabRegistry` and update verify calls to use tab-aware method signatures
<!-- SECTION:PLAN:END -->
