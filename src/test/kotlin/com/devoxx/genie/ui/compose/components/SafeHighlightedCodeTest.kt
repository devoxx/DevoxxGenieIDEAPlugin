package com.devoxx.genie.ui.compose.components

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.PhraseLocation
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Reproduces TASK-258: the conversation panel dies with
 * `IllegalArgumentException: Reversed range is not supported` when a code block
 * contains a multiline-comment terminator before the first comment opener.
 *
 * Upstream `dev.snipme:highlights` pairs the Nth comment-start index with the Nth
 * comment-end index positionally, so an unmatched terminator yields a highlight
 * whose end precedes its start. The markdown renderer hands that straight to
 * `AnnotatedString.Builder.addStyle`, which rejects it — from a background
 * coroutine, taking the whole Compose frame clock with it.
 */
class SafeHighlightedCodeTest {

    /** A snippet that starts mid-comment, as an LLM streaming a Javadoc excerpt or a diff hunk produces. */
    private val codeWithUnmatchedCommentTerminator = """
        */
        public void doWork() { /* inline */ }
    """.trimIndent()

    private fun highlightsFor(code: String) =
        Highlights.Builder()
            .theme(SyntaxThemes.default(darkMode = true))
            .code(code)
            .language(SyntaxLanguage.JAVA)
            .build()
            .getHighlights()

    /**
     * Characterization test pinning the upstream defect. If a future dependency bump
     * fixes `MultilineCommentLocator`, this test fails and the sanitising wrapper can
     * be reconsidered.
     */
    @Test
    fun `upstream highlighter emits a reversed range for an unmatched comment terminator`() {
        val highlights = highlightsFor(codeWithUnmatchedCommentTerminator)

        assertThat(highlights)
            .describedAs("expected at least one highlight with end < start")
            .anyMatch { it.location.end < it.location.start }
    }

    /** The exact crash: what the renderer does with those highlights today. */
    @Test
    fun `applying upstream highlights unguarded throws the reported exception`() {
        val code = codeWithUnmatchedCommentTerminator
        val highlights = highlightsFor(code)

        assertThatThrownBy {
            buildAnnotatedString {
                append(code)
                highlights.forEach {
                    addStyle(SpanStyle(), it.location.start, it.location.end)
                }
            }
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Reversed range is not supported")
    }

    @Test
    fun `safe builder renders code containing an unmatched comment terminator`() {
        val code = codeWithUnmatchedCommentTerminator

        assertThatCode {
            val result = buildSafeHighlightedAnnotatedString(code, highlightsFor(code))
            assertThat(result.text).isEqualTo(code)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `safe builder drops reversed ranges`() {
        val code = "abcdef"

        val result = buildSafeHighlightedAnnotatedString(
            code,
            listOf(ColorHighlight(PhraseLocation(start = 4, end = 2), rgb = 0xFF0000)),
        )

        assertThat(result.text).isEqualTo(code)
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `safe builder drops ranges that fall outside the code`() {
        val code = "abcdef"

        val result = buildSafeHighlightedAnnotatedString(
            code,
            listOf(
                ColorHighlight(PhraseLocation(start = 10, end = 20), rgb = 0xFF0000),
                BoldHighlight(PhraseLocation(start = -3, end = -1)),
            ),
        )

        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `safe builder clamps a range that runs past the end of the code`() {
        val code = "abcdef"

        val result = buildSafeHighlightedAnnotatedString(
            code,
            listOf(ColorHighlight(PhraseLocation(start = 4, end = 99), rgb = 0xFF0000)),
        )

        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles.first().start).isEqualTo(4)
        assertThat(result.spanStyles.first().end).isEqualTo(code.length)
    }

    @Test
    fun `safe builder applies well-formed ranges`() {
        val code = "abcdef"

        val result = buildSafeHighlightedAnnotatedString(
            code,
            listOf(
                ColorHighlight(PhraseLocation(start = 0, end = 3), rgb = 0x00FF00),
                BoldHighlight(PhraseLocation(start = 3, end = 6)),
            ),
        )

        assertThat(result.text).isEqualTo(code)
        assertThat(result.spanStyles).hasSize(2)
    }

    @Test
    fun `safe builder returns unstyled code when highlighting blows up`() {
        val code = "abcdef"

        val result = buildSafeHighlightedAnnotatedString(code) { error("highlighter exploded") }

        assertThat(result.text).isEqualTo(code)
        assertThat(result.spanStyles).isEmpty()
    }
}
