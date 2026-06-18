package com.devoxx.genie.ui.compose.model

import java.util.ResourceBundle

data class CustomPromptUi(
    val name: String,
    val prompt: String,
)

/**
 * Welcome-screen view of a single langchain4j skill discovered by
 * {@code SkillRegistry}. Already filtered against {@code disabledSkillNames}
 * — i.e. the welcome screen only renders skills that are currently active.
 */
data class SkillUi(
    val name: String,
    val description: String,
    val source: String,
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
        val skills: List<SkillUi> = emptyList(),
        val blogPosts: List<BlogPostUi> = emptyList(),
        /** Whether at least one MCP server is configured. Drives the "Add MCP" setup nudge. */
        val hasMcpServers: Boolean = false,
    ) : ConversationState()

    data class Chat(
        val messages: List<MessageUiModel> = emptyList(),
        val isRestoringConversation: Boolean = false,
    ) : ConversationState()
}
