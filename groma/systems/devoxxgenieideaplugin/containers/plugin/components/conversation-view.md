---
type: C4 Component
title: Conversation view
status: stable
groma:
  id: conversation-view
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/AbstractChatFontSizeAction.java
      symbol: AbstractChatFontSizeAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/DecreaseChatFontSizeAction.java
      symbol: DecreaseChatFontSizeAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/action/IncreaseChatFontSizeAction.java
      symbol: IncreaseChatFontSizeAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcher.java
      symbol: ActivityMessageDispatcher
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationHistoryManager.java
      symbol: ConversationHistoryManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationManager.java
      symbol: ConversationManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
      symbol: ConversationPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationUIController.java
      symbol: ConversationUIController
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/conversation/MessageRenderer.java
      symbol: MessageRenderer
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/PromptOutputPanel.java
      symbol: PromptOutputPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/appearance/AppearanceRefreshHandler.java
      symbol: AppearanceRefreshHandler
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/appearance/AppearanceSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/appearance/AppearanceSettingsConfigurable.java
      symbol: AppearanceSettingsConfigurable
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/appearance/AppearanceSettingsEvents.java
      symbol: AppearanceSettingsEvents
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/ChatFontSizeService.java
      symbol: ChatFontSizeService
  group: Chat experience
  technology: Swing host, Compose for Desktop
description: Hosts the chat transcript and routes streaming output into it
---

Java side of the chat output: `PromptOutputPanel` and `ConversationPanel` own a conversation's lifecycle and hand messages, streamed tokens, activity and file references to the Kotlin `ComposeConversationViewController`, which renders them with Compose for Desktop (Kotlin sources are not scanned, so they appear only here). Also owns chat font size actions and appearance settings, and loads conversations restored from history.
