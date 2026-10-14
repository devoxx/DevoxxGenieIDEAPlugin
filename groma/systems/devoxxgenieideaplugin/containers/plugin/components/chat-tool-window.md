---
type: C4 Component
title: Chat tool window and prompt input
status: stable
groma:
  id: chat-tool-window
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/Persona.java
      symbol: Persona
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/agent/AgentToggleManager.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/border/AnimatedGlowingBorder.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/border/GlowingBorder.java
      symbol: GlowingBorder
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/button/AddFilesToContextButton.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/button/ButtonFactory.java
      symbol: ButtonFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/button/CustomButton.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/button/EditorFileButtonManager.java
      symbol: EditorFileButtonManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/ContextPopupMenu.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/EventSwitchSelected.java
      symbol: EventSwitchSelected
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/ExpandablePanel.java
      symbol: ExpandablePanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/FileEntryComponent.java
      symbol: FileEntryComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/FilteringComboBox.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/input/CommandAutoCompleteTextField.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/input/ImagePreviewHandler.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/input/PromptInputArea.java
      symbol: PromptInputArea
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/InputSwitch.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/RoundBorder.java
      symbol: RoundBorder
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/component/TokenUsageBar.java
      symbol: TokenUsageBar
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/dialog/CustomPromptDialog.java
      symbol: CustomPromptDialog
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/dialog/PersonaDialog.java
      symbol: PersonaDialog
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/AbstractDocumentListener.java
      symbol: AbstractDocumentListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/ChatMemorySizeListener.java
      symbol: ChatMemorySizeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/ConversationEventListener.java
      symbol: ConversationEventListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/ConversationSelectionListener.java
      symbol: ConversationSelectionListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/ConversationStarter.java
      symbol: ConversationStarter
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/CustomPromptChangeListener.java
      symbol: CustomPromptChangeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/FileReferencesListener.java
      symbol: FileReferencesListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/FileRemoveListener.java
      symbol: FileRemoveListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/GlowingListener.java
      symbol: GlowingListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/LLMSettingsChangeListener.java
      symbol: LLMSettingsChangeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/NewlineShortcutChangeListener.java
      symbol: NewlineShortcutChangeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/PersonaChangeListener.java
      symbol: PersonaChangeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/PromptInputFocusListener.java
      symbol: PromptInputFocusListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/PromptSubmissionListener.java
      symbol: PromptSubmissionListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/RAGStateListener.java
      symbol: RAGStateListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/SettingsChangeListener.java
      symbol: SettingsChangeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/ShortcutChangeListener.java
      symbol: ShortcutChangeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/listener/WebSearchStateListener.java
      symbol: WebSearchStateListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java
      symbol: ActionButtonsPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/BackgroundPanel.java
      symbol: BackgroundPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/FileListCellRenderer.java
      symbol: FileListCellRenderer
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/FileSelectionPanelFactory.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/FindResultsPanel.java
      symbol: FindResultsPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/LlmProviderPanel.java
      symbol: LlmProviderPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/PromptContextFileListPanel.java
      symbol: PromptContextFileListPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/PromptPanelRegistry.java
      symbol: PromptPanelRegistry
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/SearchOptionsPanel.java
      symbol: SearchOptionsPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/panel/SubmitPanel.java
      symbol: SubmitPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/renderer/ModelInfoRenderer.java
      symbol: ModelInfoRenderer
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/renderer/ModelProviderRenderer.java
      symbol: ModelProviderRenderer
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/window/ConversationTabRegistry.java
      symbol: ConversationTabRegistry
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowContent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowFactory.java
  group: Chat experience
  technology: Swing, IntelliJ UI components
description: 'The DevoxxGenie tool window: provider/model picker, prompt input, context files and action buttons'
---

Entry point of the UI. `DevoxxGenieToolWindowFactory` builds one tab per conversation with the LLM provider/model panel, the prompt input area (command autocomplete, image paste, shortcuts), the context file list, and the submit/stop/add-files/agent/RAG/web-search toggles. Listener classes react to settings and message-bus events to keep the panel in sync. Submitting hands the prompt to the Prompt execution controller; output goes to the Conversation view.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java](../../../../../../src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java) | [src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java](../../../../../../src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java) | Submits prompts | Java call |
