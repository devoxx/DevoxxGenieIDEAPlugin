package com.devoxx.genie.ui.compose.viewmodel

import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.ConversationState
import dev.langchain4j.data.message.AiMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * A streamed reply that produced no visible partial text (e.g. a thinking model whose output is
 * all reasoning_content) completes through addChatMessage with the id of the prompt bubble already
 * shown. Appending a second item with that id crashed the chat's LazyColumn ("Key ... was already
 * used") and left the chat blank for the rest of the session.
 */
class ConversationViewModelDuplicateIdTest {

    @Test
    fun `adding a chat message for the live prompt fills its bubble instead of duplicating it`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("1794492951268").userPrompt("hi").build())

        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("1794492951268")
                .userPrompt("hi")
                .aiMessage(AiMessage.from("Angular 21."))
                .build()
        )

        val messages = (viewModel.state as ConversationState.Chat).messages
        assertThat(messages.map { it.id }).containsExactly("1794492951268")
        assertThat(messages.single().aiResponseMarkdown).isEqualTo("Angular 21.")
        assertThat(messages.single().userPrompt).isEqualTo("hi")
    }

    @Test
    fun `chat messages with distinct ids are still appended`() {
        val viewModel = ConversationViewModel()
        viewModel.addChatMessage(ChatMessageContext.builder().id("a").userPrompt("q1").aiMessage(AiMessage.from("r1")).build())
        viewModel.addChatMessage(ChatMessageContext.builder().id("b").userPrompt("q2").aiMessage(AiMessage.from("r2")).build())

        val messages = (viewModel.state as ConversationState.Chat).messages
        assertThat(messages.map { it.id }).containsExactly("a", "b")
    }
}
