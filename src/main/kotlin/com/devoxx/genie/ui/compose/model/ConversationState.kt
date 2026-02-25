package com.devoxx.genie.ui.compose.model

import java.util.ResourceBundle

data class CustomPromptUi(
    val name: String,
    val prompt: String,
)

sealed class ConversationState {
    data class Welcome(
        val resourceBundle: ResourceBundle,
        val customPrompts: List<CustomPromptUi> = emptyList(),
    ) : ConversationState()

    data class Chat(
        val messages: List<MessageUiModel> = emptyList(),
        val isRestoringConversation: Boolean = false,
    ) : ConversationState()
}
