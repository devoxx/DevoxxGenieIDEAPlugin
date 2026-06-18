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

    // --- entrance animation gating (guards the scroll snap-to-top regression) ---

    @Test
    fun `entrance plays the first time a live message is seen`() {
        val seen = mutableSetOf<String>()
        assertThat(
            shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = true, isRestoring = false)
        ).isTrue()
    }

    @Test
    fun `entrance does not replay when a recycled item re-enters composition`() {
        val seen = mutableSetOf<String>()
        // First composition: animates once.
        shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = true, isRestoring = false)
        // Scrolled away and back: the item re-composes and must NOT animate again,
        // otherwise AnimatedVisibility re-enters the layout path and the LazyColumn
        // loses its scroll anchor (jump-to-top bug).
        assertThat(
            shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = true, isRestoring = false)
        ).isFalse()
    }

    @Test
    fun `entrance never plays for messages pre-seeded from restored history`() {
        val seen = mutableSetOf("m1", "m2")
        assertThat(
            shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = true, isRestoring = false)
        ).isFalse()
        assertThat(
            shouldPlayEntrance(seen, messageId = "m2", animationsEnabled = true, isRestoring = false)
        ).isFalse()
    }

    @Test
    fun `entrance does not play while restoring`() {
        val seen = mutableSetOf<String>()
        assertThat(
            shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = true, isRestoring = true)
        ).isFalse()
        // Id is still marked seen, so a later live recomposition won't suddenly animate.
        assertThat(
            shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = true, isRestoring = false)
        ).isFalse()
    }

    @Test
    fun `entrance does not play when animations are disabled`() {
        val seen = mutableSetOf<String>()
        assertThat(
            shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = false, isRestoring = false)
        ).isFalse()
        // Still marked seen: re-enabling animations later must not retroactively animate it.
        assertThat(
            shouldPlayEntrance(seen, messageId = "m1", animationsEnabled = true, isRestoring = false)
        ).isFalse()
    }
}
