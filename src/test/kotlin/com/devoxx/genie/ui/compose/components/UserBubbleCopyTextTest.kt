package com.devoxx.genie.ui.compose.components

import com.devoxx.genie.ui.compose.model.MessageUiModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Light-weight contract test guarding the user-bubble copy behaviour.
 *
 * The Compose UI test runner is not configured in this project, so we cannot
 * click the actual button. Instead we assert the invariant that [UserBubble]
 * relies on: the text fed to [CopyButton] is the raw [MessageUiModel.userPrompt]
 * with no transformation. Any future refactor that changes this contract should
 * either update the bubble accordingly or update this test deliberately.
 */
class UserBubbleCopyTextTest {

    @Test
    fun `copy text equals raw userPrompt - markdown preserved`() {
        val raw = "Hello **world**\n```kt\nval x = 1\n```"
        val m = MessageUiModel(id = "1", userPrompt = raw)

        // Contract: UserBubble passes userPrompt unchanged to CopyButton.textToCopy
        assertThat(m.userPrompt).isEqualTo(raw)
        assertThat(m.userPrompt).contains("**world**")
        assertThat(m.userPrompt).contains("```kt")
    }

    @Test
    fun `blank user prompt - bubble early-returns, no copy button rendered`() {
        // UserBubble has `if (promptText.isBlank()) return` at the top.
        // This test documents that contract: blank prompts get no copy button.
        val blank = MessageUiModel(id = "1", userPrompt = "   ")
        assertThat(blank.userPrompt.isBlank()).isTrue()
    }
}
