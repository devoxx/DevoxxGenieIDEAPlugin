package com.devoxx.genie.ui.compose.screen

import com.devoxx.genie.ui.compose.model.MessageUiModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChatScreenScrollButtonTest {

    private fun message(id: String, streaming: Boolean) =
        MessageUiModel(id = id, userPrompt = "hi", isStreaming = streaming)

    @Test
    fun `button shows when user scrolled up while the last message is streaming`() {
        val messages = listOf(message("m1", streaming = false), message("m2", streaming = true))
        assertThat(shouldShowScrollToBottom(autoFollow = false, messages = messages)).isTrue()
    }

    @Test
    fun `button hidden while auto-following even during streaming`() {
        val messages = listOf(message("m1", streaming = true))
        assertThat(shouldShowScrollToBottom(autoFollow = true, messages = messages)).isFalse()
    }

    @Test
    fun `button hidden when scrolled up over completed history with nothing streaming`() {
        val messages = listOf(message("m1", streaming = false), message("m2", streaming = false))
        assertThat(shouldShowScrollToBottom(autoFollow = false, messages = messages)).isFalse()
    }

    @Test
    fun `button hidden when there are no messages`() {
        assertThat(shouldShowScrollToBottom(autoFollow = false, messages = emptyList())).isFalse()
    }
}
