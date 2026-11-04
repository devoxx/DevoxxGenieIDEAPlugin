package com.devoxx.genie.ui.compose.viewmodel

import androidx.compose.ui.unit.IntSize
import com.devoxx.genie.model.request.ChatMessageContext
import dev.langchain4j.data.message.AiMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Switching DevoxxGenie tabs removes the chat's ComposePanel from the Swing hierarchy, which
 * disposes its composition and every `remember` inside ChatScreen. The scroll position, the
 * measured AI bubble sizes and the follow-the-tail flag must therefore live in the view model,
 * or the returning tab rebuilds from the top and animates down through the whole transcript.
 */
class ConversationViewModelChatScrollTest {

    private fun ConversationViewModel.addExchange(id: String) = addChatMessage(
        ChatMessageContext.builder()
            .id(id)
            .userPrompt("prompt $id")
            .aiMessage(AiMessage.from("answer $id"))
            .build()
    )

    @Test
    fun `scroll memory survives a tab switch`() {
        val viewModel = ConversationViewModel()
        viewModel.addExchange("m1")

        val beforeSwitch = viewModel.chatScroll
        beforeSwitch.aiBubbleSizes["m1"] = IntSize(800, 1200)
        beforeSwitch.autoFollow = false

        // The returning tab composes ChatScreen afresh and reads the memory again.
        val afterSwitch = viewModel.chatScroll

        assertThat(afterSwitch).isSameAs(beforeSwitch)
        assertThat(afterSwitch.aiBubbleSizes).containsEntry("m1", IntSize(800, 1200))
        assertThat(afterSwitch.autoFollow).isFalse()
    }

    @Test
    fun `scroll memory survives new messages in the same conversation`() {
        val viewModel = ConversationViewModel()
        viewModel.addExchange("m1")
        val memory = viewModel.chatScroll

        viewModel.addExchange("m2")

        assertThat(viewModel.chatScroll).isSameAs(memory)
    }

    @Test
    fun `clearing the conversation starts a fresh scroll memory`() {
        val viewModel = ConversationViewModel()
        viewModel.addExchange("m1")
        val old = viewModel.chatScroll
        old.aiBubbleSizes["m1"] = IntSize(800, 1200)
        old.autoFollow = false

        viewModel.clearConversation()

        assertThat(viewModel.chatScroll).isNotSameAs(old)
        assertThat(viewModel.chatScroll.aiBubbleSizes).isEmpty()
        assertThat(viewModel.chatScroll.autoFollow).isTrue()
    }

    @Test
    fun `restoring a conversation starts a fresh scroll memory`() {
        val viewModel = ConversationViewModel()
        viewModel.addExchange("m1")
        val old = viewModel.chatScroll

        viewModel.setRestoringConversation(true)
        viewModel.clearConversation()

        assertThat(viewModel.chatScroll).isNotSameAs(old)
    }
}
