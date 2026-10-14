---
type: C4 Component
title: Conversation history
status: stable
groma:
  id: conversation-history
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/conversation/ChatMessage.java
      symbol: ChatMessage
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/conversation/Conversation.java
      symbol: Conversation
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
      symbol: ConversationStorageService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/conversations/PendingConversationDeletionManager.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java
  group: Chat experience
  technology: SQLite (JDBC)
description: Saves and restores past conversations
---

Persists each conversation and its messages in a SQLite `conversations.db` under the IDE config directory, and provides the history panel used to reopen or delete them.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java](../../../../../../src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java) | [src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java](../../../../../../src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java) | Loads saved conversations | Java call |
