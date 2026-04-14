package com.devoxx.genie.ui.compose.model

import java.util.ResourceBundle

data class CustomPromptUi(
    val name: String,
    val prompt: String,
)

data class BlogPostUi(
    val title: String,
    val description: String,
    val date: String,
    val url: String,
)

sealed class ConversationState {
    data class Welcome(
        val resourceBundle: ResourceBundle,
        val customPrompts: List<CustomPromptUi> = emptyList(),
        val blogPosts: List<BlogPostUi> = emptyList(),
    ) : ConversationState()

    data class Chat(
        val messages: List<MessageUiModel> = emptyList(),
        val isRestoringConversation: Boolean = false,
    ) : ConversationState()
}
