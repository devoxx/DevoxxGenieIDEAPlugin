package com.devoxx.genie.ui.compose.viewmodel

import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.ConversationState
import dev.langchain4j.data.message.AiMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConversationViewModelThinkingTest {

    @Test
    fun `update ai content splits thinking marker into dedicated model field`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.updateAiMessageContent(
            ChatMessageContext.builder()
                .id("msg-1")
                .aiMessage(AiMessage.from("""
                    <!-- devoxx-genie-thinking-start -->
                    I should inspect this first.
                    <!-- devoxx-genie-thinking-end -->

                    The answer is 42.
                """.trimIndent()))
                .build()
        )

        val msg = (viewModel.state as ConversationState.Chat).messages.single()
        assertThat(msg.thinkingMarkdown).isEqualTo("I should inspect this first.")
        assertThat(msg.aiResponseMarkdown).isEqualTo("The answer is 42.")
    }

    @Test
    fun `restored chat message splits thinking marker into dedicated model field`() {
        val viewModel = ConversationViewModel()

        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hi")
                .aiMessage(AiMessage.from("""
                    <!-- devoxx-genie-thinking-start -->
                    Think separately.
                    <!-- devoxx-genie-thinking-end -->

                    Final answer.
                """.trimIndent()))
                .build()
        )

        val msg = (viewModel.state as ConversationState.Chat).messages.single()
        assertThat(msg.thinkingMarkdown).isEqualTo("Think separately.")
        assertThat(msg.aiResponseMarkdown).isEqualTo("Final answer.")
    }
}
