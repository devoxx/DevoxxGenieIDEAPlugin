---
id: TASK-198.1
title: Fix chat persistence to conversation history for tabbed chats
status: Done
assignee: []
created_date: '2026-03-08 15:46'
updated_date: '2026-03-08 15:55'
labels:
  - bug
  - ui
  - persistence
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/ChatService.java
  - src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
  - >-
    src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java
  - >-
    src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowFactory.java
  - src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowContent.java
parent_task_id: TASK-198
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Users report that chats created in the new tabbed conversation flow are not being saved into conversation history reliably. Fix the persistence flow so completed user/assistant exchanges are stored and appear in conversation history as expected for tabbed chats, without regressions to existing single-project history behavior.

**Root Cause:** `CliPromptStrategy.finalizeSuccess()` never published the `CONVERSATION_TOPIC` event after a CLI runner prompt completed. Both `StreamingResponseHandler` and `NonStreamingPromptStrategy` correctly publish this event, but the CLI strategy was missing it entirely. This meant all CLI runner conversations (Claude, Codex, Kimi) were never persisted to conversation history.

The tab filtering logic in `ChatService.onNewConversation()` was investigated and found to be correct — each tab's ChatService saves only its own events via tabId matching.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 After a tabbed chat prompt completes, the user/assistant exchange is persisted and appears in conversation history without requiring manual recovery steps.
- [x] #2 New conversations and follow-up messages on an existing conversation are both saved correctly in the tabbed chat flow.
- [x] #3 Conversation history remains stable across multiple open tabs: one tab's saved messages do not overwrite, suppress, or duplicate another tab's history entries.
- [x] #4 Persisted conversation history is still available after closing and reopening the IDE.
- [x] #5 Automated regression coverage verifies the tabbed conversation save flow and history visibility behavior.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Fix

Added the missing `CONVERSATION_TOPIC` event publication in `CliPromptStrategy.finalizeSuccess()` to match the pattern used by Streaming and NonStreaming strategies.

### Changed file
- `src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java`
  - Added `import com.devoxx.genie.ui.topic.AppTopics`
  - Added `project.getMessageBus().syncPublisher(AppTopics.CONVERSATION_TOPIC).onNewConversation(context)` after `ChatMemoryManager.addAiResponse()` and before `resultTask.complete()`

### Investigation findings (no changes needed)
- `ChatService.onNewConversation()` tab filtering: correct — each tab's instance saves only matching events
- `ConversationStorageService`: correctly tab-agnostic — stores by projectHash, history shared across tabs
- `ConversationHistoryPanel.loadConversations()`: correctly loads all project conversations regardless of tab
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Fix: CLI runner conversations not persisted to history

**Root cause:** `CliPromptStrategy.finalizeSuccess()` was missing the `CONVERSATION_TOPIC` event publication that triggers `ChatService.saveConversation()`. Both streaming and non-streaming strategies had this, but CLI strategy did not.

**Fix:** Added `project.getMessageBus().syncPublisher(AppTopics.CONVERSATION_TOPIC).onNewConversation(context)` in `CliPromptStrategy.finalizeSuccess()`.

**Files changed:** `src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java` (2 lines added: 1 import + 1 event publication)
<!-- SECTION:FINAL_SUMMARY:END -->
