---
id: TASK-198
title: Add conversation tabs for parallel model usage
status: Done
assignee: []
created_date: '2026-03-08 12:51'
updated_date: '2026-03-08 15:43'
labels:
  - feature
  - ui
  - architecture
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowFactory.java
  - src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowContent.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryService.java
  - src/main/java/com/devoxx/genie/service/FileListManager.java
  - src/main/java/com/devoxx/genie/ui/window/SpecBrowserToolWindowFactory.java
  - src/main/java/com/devoxx/genie/service/cli/CliConsoleManager.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add tabbed conversations to the DevoxxGenie tool window, allowing users to run multiple LLM models in parallel. Each tab operates independently with its own model selection, chat memory, file context, and conversation state.

## Current State

- Only one active conversation at a time per project
- Chat memory, file lists, and model selection are all scoped per-project (global singletons)
- Switching conversations requires the history popup (clear → load → display)
- No parallel execution possible

## Approach: IntelliJ ContentManager Tabs (Native)

Use IntelliJ's built-in `ContentManager` API for dynamic tabs. Each tab wraps its own `DevoxxGenieToolWindowContent` instance. This pattern is already used in `SpecBrowserToolWindowFactory` and `CliConsoleManager`.

## Implementation Phases

### Phase 1: Add tabId to ChatMessageContext
- Add `String tabId` field to `ChatMessageContext` (nullable, backward-compatible)
- Helper `getMemoryKey()` → composite key for routing

### Phase 2: Refactor ChatMemoryService for composite keys
- Support `projectHash-tabId` composite keys in `ChatMemoryService` and `ChatMemoryManager`
- Keep backward-compatible Project-based methods

### Phase 3: Refactor FileListManager for per-tab file lists
- Support composite keys in internal maps for per-tab file isolation

### Phase 4: Make DevoxxGenieToolWindowContent tab-aware
- Add `tabId` (UUID), store Content reference for title updates
- Pass tabId through to SubmitPanel, ActionButtonsPanel, PromptExecutionController

### Phase 5: Multi-tab factory
- Modify `DevoxxGenieToolWindowFactory` to create/manage multiple Content tabs
- Add "New Chat" title action, closeable tabs, ContentManagerListener for cleanup
- Max tab limit (8) for memory management

### Phase 6: Auto-naming tabs
- After first prompt: rename tab to `[Model]: [First ~30 chars of prompt]...`

### Phase 7: Tab persistence across IDE restarts
- Persist tab descriptors in DevoxxGenieStateService (tabId, conversationId, provider/model)
- Restore on startup from descriptors + SQLite conversations

### Phase 8: Edge cases
- ExternalPromptService routing to active tab
- Lazy Compose view initialization
- Conversation history scoping

## Critical Files

- `ui/window/DevoxxGenieToolWindowFactory.java` - Multi-tab creation and management
- `ui/window/DevoxxGenieToolWindowContent.java` - Tab-aware with tabId and auto-naming
- `service/prompt/memory/ChatMemoryService.java` - Composite key support
- `service/prompt/memory/ChatMemoryManager.java` - Route by memoryKey
- `service/FileListManager.java` - Per-tab file isolation
- `model/request/ChatMessageContext.java` - tabId field
- `service/DevoxxGenieStateService.java` - Tab persistence
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Users can create multiple conversation tabs via a + button in the tool window title bar
- [x] #2 Each tab has independent model/provider selection
- [x] #3 Tabs auto-rename to [Model]: [prompt summary] after first prompt
- [x] #4 Parallel execution works - streaming in one tab does not block another
- [x] #5 Each tab has isolated chat memory and file context
- [x] #6 Tabs can be closed with proper cleanup of memory and resources
- [x] #7 Tab state persists across IDE restarts
- [x] #8 Max 8 tabs enforced to manage memory
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Feature implementation reviewed again after fixes; no open findings remain in the tab execution, routing, file-context isolation, cleanup, or persistence paths.

Targeted verification passed for ActionButtonsPanelControllerTest, MessageCreationServiceTest, ChatMemoryManagerTest, NonStreamingPromptExecutionServiceTest, StreamingResponseHandlerTest, AbstractPromptExecutionStrategyTest, NonStreamingPromptStrategyTest, SpecTaskRunnerServiceTest, PromptContextFileListPanelTest, and LlmProviderPanelTest.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented native multi-conversation tabs in the DevoxxGenie tool window with per-tab isolation for provider/model selection, chat memory, file context, prompt execution, and conversation state. Added tab creation and close management with an 8-tab limit, auto-renaming after the first prompt, active-tab routing for external/spec prompt submission, per-tab cleanup on close, and persisted open tabs/provider-model state across IDE restarts. Follow-up review passes found no remaining feature-level issues in the implementation; targeted regression tests covering prompt execution, memory, file context, spec prompt routing, and provider state updates are passing.
<!-- SECTION:FINAL_SUMMARY:END -->
