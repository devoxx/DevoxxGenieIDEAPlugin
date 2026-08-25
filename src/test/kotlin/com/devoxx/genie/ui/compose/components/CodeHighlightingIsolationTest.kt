package com.devoxx.genie.ui.compose.components

import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * TASK-259: two code blocks in one message must never highlight against each other's text.
 *
 * `Highlights.Builder` is a data class with `var` fields — `code(...)` mutates it in place and
 * `build()` snapshots whatever the fields hold at that instant. Sharing one builder across the
 * code blocks of a message therefore lets concurrent highlighting on `Dispatchers.Default`
 * interleave, so a block can be styled with offsets computed from a different block's text.
 */
class CodeHighlightingIsolationTest {

    private val theme = SyntaxThemes.default(darkMode = true)

    private val codeA = """
        public String greet() { return "hello there"; }
    """.trimIndent()

    private val codeB = """
        // counts things
        public int count(int[] xs) { return xs.length + 1234; }
    """.trimIndent()

    /**
     * Pins the mechanism behind the race, deterministically and without threads: this is exactly
     * the interleaving two concurrent blocks hit when they share one builder.
     */
    @Test
    fun `a shared builder hands one block the highlights of another`() {
        val shared = Highlights.Builder().theme(theme)

        shared.code(codeA)                              // block A prepares its highlighting
        shared.code(codeB)                              // block B prepares before A gets to build
        val whatBlockAReceives = shared.build().getHighlights()

        assertThat(whatBlockAReceives).isEqualTo(computeHighlights(codeB, "java", theme))
        assertThat(whatBlockAReceives).isNotEqualTo(computeHighlights(codeA, "java", theme))
    }

    @Test
    fun `computed highlights fall within the code they were computed from`() {
        val highlights = computeHighlights(codeA, "java", theme)

        assertThat(highlights).isNotEmpty
        assertThat(highlights).allMatch { it.location.end <= codeA.length }
    }

    @Test
    fun `concurrent highlighting keeps each block matched to its own text`() {
        val expectedA = computeHighlights(codeA, "java", theme)
        val expectedB = computeHighlights(codeB, "java", theme)
        assertThat(expectedA).isNotEqualTo(expectedB)

        val iterations = 200
        val pool = Executors.newFixedThreadPool(8)
        try {
            val work = (0 until iterations).flatMap {
                listOf(
                    Callable { codeA to computeHighlights(codeA, "java", theme) },
                    Callable { codeB to computeHighlights(codeB, "java", theme) },
                )
            }

            val results = pool.invokeAll(work).map { it.get(30, TimeUnit.SECONDS) }

            assertThat(results).hasSize(iterations * 2)
            results.forEach { (code, highlights) ->
                assertThat(highlights).isEqualTo(if (code == codeA) expectedA else expectedB)
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `highlighting an unknown language still produces highlights for its own text`() {
        val highlights = computeHighlights(codeA, "no-such-language", theme)

        assertThat(highlights).allMatch { it.location.end <= codeA.length }
    }
}
