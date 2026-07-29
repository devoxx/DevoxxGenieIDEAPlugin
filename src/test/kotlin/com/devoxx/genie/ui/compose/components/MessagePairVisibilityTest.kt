package com.devoxx.genie.ui.compose.components

import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.model.TerminalState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Issue #1241 steering UI: messages created by a steering split (frozen copies and
 * continuation areas) must not render an empty header-only AI frame — those empty
 * frames visually break the question/answer sequence. Regular messages keep their
 * AI bubble regardless (legacy behavior).
 */
class MessagePairVisibilityTest {

    private fun message(
        aiResponseMarkdown: String = "",
        isSteeringFrozen: Boolean = false,
        aiContentOffset: Int = 0,
        terminalState: TerminalState = TerminalState.COMPLETED,
        isLoadingIndicatorVisible: Boolean = false,
    ) = MessageUiModel(
        id = "msg-1",
        userPrompt = "prompt",
        aiResponseMarkdown = aiResponseMarkdown,
        isSteeringFrozen = isSteeringFrozen,
        aiContentOffset = aiContentOffset,
        terminalState = terminalState,
        isLoadingIndicatorVisible = isLoadingIndicatorVisible,
    )

    @Test
    fun `empty frozen copy hides its AI bubble`() {
        assertThat(shouldHideAiBubble(message(isSteeringFrozen = true))).isTrue()
    }

    @Test
    fun `frozen copy with content keeps its AI bubble`() {
        assertThat(shouldHideAiBubble(message(aiResponseMarkdown = "answer so far", isSteeringFrozen = true)))
            .isFalse()
    }

    @Test
    fun `empty continuation hides its AI bubble until content arrives`() {
        assertThat(shouldHideAiBubble(message(aiContentOffset = 25))).isTrue()
    }

    @Test
    fun `continuation with content keeps its AI bubble`() {
        assertThat(shouldHideAiBubble(message(aiResponseMarkdown = "continued", aiContentOffset = 25)))
            .isFalse()
    }

    @Test
    fun `empty continuation with terminal state keeps its AI bubble`() {
        // A stopped/errored run must still show its terminal card
        assertThat(shouldHideAiBubble(message(aiContentOffset = 25, terminalState = TerminalState.STOPPED)))
            .isFalse()
    }

    @Test
    fun `empty continuation with visible loading indicator keeps its AI bubble`() {
        assertThat(shouldHideAiBubble(message(aiContentOffset = 25, isLoadingIndicatorVisible = true)))
            .isFalse()
    }

    @Test
    fun `regular empty message keeps its AI bubble - legacy behavior unchanged`() {
        assertThat(shouldHideAiBubble(message())).isFalse()
    }
}
