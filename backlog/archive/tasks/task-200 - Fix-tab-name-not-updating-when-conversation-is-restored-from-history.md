---
id: TASK-200
title: Fix tab name not updating when conversation is restored from history
status: Done
assignee: []
created_date: '2026-03-08 16:48'
updated_date: '2026-03-08 17:01'
labels:
  - bug
  - ui
  - conversation-tabs
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/ComposeConversationViewController.kt
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a conversation is restored from the conversation history, the tab name remains "New Chat" instead of updating to reflect the restored conversation's name/title. The tab should display the conversation's actual name after restoration.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 When a conversation is restored from history, the tab name updates to the conversation's stored name
- [x] #2 The tab name should not remain as 'New Chat' after restoration
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added `updateTabDisplayName()` method to `ConversationManager.java` that looks up the tab's `Content` via `ConversationTabRegistry` using the `tabId`, then sets its display name to `"modelName: title"` (truncated to 40 chars). Called from `onConversationSelected()` after updating the conversation label. File changed: `src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationManager.java`.
<!-- SECTION:FINAL_SUMMARY:END -->
