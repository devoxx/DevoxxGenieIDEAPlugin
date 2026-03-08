---
id: TASK-198.1
title: Fix chat persistence to conversation history for tabbed chats
status: To Do
assignee: []
created_date: '2026-03-08 15:46'
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
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After a tabbed chat prompt completes, the user/assistant exchange is persisted and appears in conversation history without requiring manual recovery steps.
- [ ] #2 New conversations and follow-up messages on an existing conversation are both saved correctly in the tabbed chat flow.
- [ ] #3 Conversation history remains stable across multiple open tabs: one tab's saved messages do not overwrite, suppress, or duplicate another tab's history entries.
- [ ] #4 Persisted conversation history is still available after closing and reopening the IDE.
- [ ] #5 Automated regression coverage verifies the tabbed conversation save flow and history visibility behavior.
<!-- AC:END -->
