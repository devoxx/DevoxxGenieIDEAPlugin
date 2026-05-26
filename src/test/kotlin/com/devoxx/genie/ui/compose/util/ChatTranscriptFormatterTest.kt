package com.devoxx.genie.ui.compose.util

import com.devoxx.genie.ui.compose.model.MessageUiModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChatTranscriptFormatterTest {

    private fun msg(
        id: String,
        user: String,
        ai: String = "",
        model: String = "",
    ): MessageUiModel = MessageUiModel(
        id = id,
        userPrompt = user,
        aiResponseMarkdown = ai,
        modelName = model,
    )

    @Test
    fun `empty list returns empty string`() {
        assertThat(ChatTranscriptFormatter.toMarkdown(emptyList())).isEmpty()
    }

    @Test
    fun `single user message without response is included`() {
        val out = ChatTranscriptFormatter.toMarkdown(
            listOf(msg("1", "Hello there"))
        )
        assertThat(out).contains("### User").contains("Hello there")
        assertThat(out).doesNotContain("### Assistant")
    }

    @Test
    fun `user and assistant message produce both headers`() {
        val out = ChatTranscriptFormatter.toMarkdown(
            listOf(msg("1", "What is 2+2?", ai = "It is 4."))
        )
        // Order: User header → prompt → Assistant header → response
        val userIdx = out.indexOf("### User")
        val promptIdx = out.indexOf("What is 2+2?")
        val assistantIdx = out.indexOf("### Assistant")
        val replyIdx = out.indexOf("It is 4.")
        assertThat(userIdx).isGreaterThanOrEqualTo(0)
        assertThat(promptIdx).isGreaterThan(userIdx)
        assertThat(assistantIdx).isGreaterThan(promptIdx)
        assertThat(replyIdx).isGreaterThan(assistantIdx)
    }

    @Test
    fun `assistant header includes model name when present`() {
        val out = ChatTranscriptFormatter.toMarkdown(
            listOf(msg("1", "Hi", ai = "Hello", model = "gpt-4o-mini"))
        )
        assertThat(out).contains("### Assistant (gpt-4o-mini)")
    }

    @Test
    fun `assistant header is plain when model name blank`() {
        val out = ChatTranscriptFormatter.toMarkdown(
            listOf(msg("1", "Hi", ai = "Hello", model = ""))
        )
        assertThat(out).contains("### Assistant\n")
        assertThat(out).doesNotContain("### Assistant (")
    }

    @Test
    fun `multiple messages separated by horizontal rule`() {
        val messages = listOf(
            msg("1", "first", ai = "reply1"),
            msg("2", "second", ai = "reply2"),
            msg("3", "third", ai = "reply3"),
        )
        val out = ChatTranscriptFormatter.toMarkdown(messages)
        // n - 1 separators between n included messages
        val sepCount = "---".toRegex().findAll(out).count()
        assertThat(sepCount).isEqualTo(messages.size - 1)
        // No trailing separator after the last message
        assertThat(out.trimEnd()).doesNotEndWith("---")
    }

    @Test
    fun `blank user prompt is skipped`() {
        val out = ChatTranscriptFormatter.toMarkdown(
            listOf(
                msg("1", "real prompt", ai = "real reply"),
                msg("2", "   ", ai = "ignored reply"),
            )
        )
        assertThat(out).contains("real prompt")
        assertThat(out).doesNotContain("ignored reply")
        // No separator for the skipped one → no "---" at all (only one included msg)
        assertThat(out).doesNotContain("---")
    }

    @Test
    fun `preserves markdown formatting in prompt`() {
        val prompt = "Look at this:\n```java\nSystem.out.println(\"hi\");\n```"
        val out = ChatTranscriptFormatter.toMarkdown(listOf(msg("1", prompt)))
        assertThat(out).contains("```java")
        assertThat(out).contains("System.out.println(\"hi\");")
        assertThat(out).contains("```")
    }

    @Test
    fun `output ends with single trailing newline`() {
        val out = ChatTranscriptFormatter.toMarkdown(
            listOf(msg("1", "hi", ai = "hello"))
        )
        assertThat(out).endsWith("\n")
        assertThat(out).doesNotEndWith("\n\n")
    }

    @Test
    fun `prompts and responses trimmed of trailing whitespace`() {
        val out = ChatTranscriptFormatter.toMarkdown(
            listOf(
                msg("1", "hi\n\n\n", ai = "reply\n  \n"),
                msg("2", "again", ai = "again-reply"),
            )
        )
        // No four consecutive newlines (which would happen without trimming)
        assertThat(out).doesNotContain("\n\n\n\n")
        // Both messages still present
        assertThat(out).contains("hi")
        assertThat(out).contains("reply")
        assertThat(out).contains("again")
    }
}
