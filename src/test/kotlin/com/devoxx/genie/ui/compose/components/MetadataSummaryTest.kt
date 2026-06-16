package com.devoxx.genie.ui.compose.components

import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.model.TokenUsageInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for the AI bubble metadata row formatting (AiBubble.kt):
 *  - token counts are abbreviated with a formatter (10502 -> "10.5K") instead of being
 *    printed raw, and
 *  - input vs output tokens are clearly labelled so "10502/476 tokens" can no longer be
 *    mistaken for a used/max ratio.
 */
class MetadataSummaryTest {

    // --- formatTokens ---

    @Test
    fun `small counts are printed verbatim`() {
        assertEquals("0", formatTokens(0))
        assertEquals("476", formatTokens(476))
        assertEquals("999", formatTokens(999))
    }

    @Test
    fun `thousands keep one decimal below 100K`() {
        assertEquals("1K", formatTokens(1_000))
        assertEquals("1.5K", formatTokens(1_500))
        assertEquals("10.5K", formatTokens(10_502))
    }

    @Test
    fun `large thousands round to integer`() {
        assertEquals("128K", formatTokens(128_000))
        assertEquals("131K", formatTokens(131_072))
    }

    @Test
    fun `millions and billions are abbreviated`() {
        assertEquals("1M", formatTokens(1_000_000))
        assertEquals("1.5M", formatTokens(1_500_000))
        assertEquals("2M", formatTokens(2_000_000))
        assertEquals("1B", formatTokens(1_000_000_000))
    }

    @Test
    fun `decimal separator is always a period regardless of locale`() {
        val previous = java.util.Locale.getDefault()
        try {
            // A locale that uses comma as the decimal separator must not leak into the output.
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("10.5K", formatTokens(10_502))
            assertEquals("1.5M", formatTokens(1_500_000))
        } finally {
            java.util.Locale.setDefault(previous)
        }
    }

    // --- formatMetadataSummary ---

    private fun message(
        executionTimeMs: Long = 0,
        inputTokens: Long = 0,
        outputTokens: Long = 0,
        cost: Double = 0.0,
        contextWindowMax: Long = 0,
    ) = MessageUiModel(
        id = "1",
        userPrompt = "hi",
        executionTimeMs = executionTimeMs,
        tokenUsage = TokenUsageInfo(
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            cost = cost,
            contextWindowMax = contextWindowMax,
        ),
    )

    @Test
    fun `token usage is labelled input and output`() {
        val summary = formatMetadataSummary(message(inputTokens = 10_502, outputTokens = 476))
        assertEquals("10.5K in / 476 out", summary)
    }

    @Test
    fun `time and tokens are joined with a tilde`() {
        val summary = formatMetadataSummary(message(executionTimeMs = 1_800, inputTokens = 10_502, outputTokens = 179))
        assertEquals("1.8s ~ 10.5K in / 179 out", summary)
    }

    @Test
    fun `context window is never shown in the bubble`() {
        // The overall context-window usage lives under the prompt; it must not be repeated
        // per-bubble even when the model's max window is known.
        val summary = formatMetadataSummary(
            message(
                executionTimeMs = 7_700,
                inputTokens = 10_502,
                outputTokens = 476,
                contextWindowMax = 131_072,
            ),
        )
        assertEquals("7.7s ~ 10.5K in / 476 out", summary)
    }

    @Test
    fun `cost is appended when present`() {
        val summary = formatMetadataSummary(
            message(executionTimeMs = 1_800, inputTokens = 100, outputTokens = 50, cost = 0.0023),
        )
        assertEquals("1.8s ~ 100 in / 50 out ~ \$0.0023", summary)
    }

    @Test
    fun `empty when nothing is known`() {
        assertEquals("", formatMetadataSummary(message()))
    }
}
