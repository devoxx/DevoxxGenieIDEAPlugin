package com.devoxx.genie.ui.compose.components

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ErrorCardTextTest {

    private val long = "x".repeat(ErrorCardText.COLLAPSED_LENGTH) + " and the end of the message"
    private val short = "Connection refused"

    @Test
    fun `a short error is shown whole and cannot be expanded`() {
        assertThat(ErrorCardText.isExpandable(short)).isFalse()
        assertThat(ErrorCardText.displayed(short, expanded = false)).isEqualTo(short)
        assertThat(ErrorCardText.displayed(short, expanded = true)).isEqualTo(short)
    }

    @Test
    fun `an error exactly at the limit is not shortened`() {
        val atLimit = "y".repeat(ErrorCardText.COLLAPSED_LENGTH)
        assertThat(ErrorCardText.isExpandable(atLimit)).isFalse()
        assertThat(ErrorCardText.displayed(atLimit, expanded = false)).isEqualTo(atLimit)
    }

    @Test
    fun `a long error is shortened until expanded`() {
        assertThat(ErrorCardText.isExpandable(long)).isTrue()

        val collapsed = ErrorCardText.displayed(long, expanded = false)
        assertThat(collapsed).hasSize(ErrorCardText.COLLAPSED_LENGTH + 1).endsWith("…")
        assertThat(long).startsWith(collapsed.dropLast(1))

        assertThat(ErrorCardText.displayed(long, expanded = true)).isEqualTo(long)
    }

    @Test
    fun `copy always takes the full error, whether or not the card is expanded`() {
        assertThat(ErrorCardText.copyText(long)).isEqualTo(long)
        assertThat(ErrorCardText.copyText(short)).isEqualTo(short)
    }
}
