package com.devoxx.genie.ui.compose.util

import com.devoxx.genie.ui.compose.model.MessageUiModel

/**
 * Serialises a chat conversation to a Markdown transcript suitable for copying
 * to the clipboard. Pure Kotlin (no Compose dependencies) so it can be unit-tested
 * without a Compose runtime.
 */
object ChatTranscriptFormatter {

    /**
     * Returns a Markdown transcript of [messages]:
     *  - each message becomes a `### User` / `### Assistant` block
     *  - the assistant header includes the model name in parentheses when present
     *  - messages are separated by `---`
     *  - messages with a blank user prompt are skipped (they contribute neither
     *    block nor separator)
     *  - prompts and responses are right-trimmed to avoid runaway blank lines
     *  - the result ends with a single trailing newline; an empty input list
     *    produces an empty string
     */
    fun toMarkdown(messages: List<MessageUiModel>): String {
        if (messages.isEmpty()) return ""

        val included = messages.filter { it.userPrompt.isNotBlank() }
        if (included.isEmpty()) return ""

        val sb = StringBuilder()
        included.forEachIndexed { idx, m ->
            sb.append("### User\n\n")
            sb.append(m.userPrompt.trimEnd()).append("\n\n")

            if (m.aiResponseMarkdown.isNotBlank()) {
                val header = if (m.modelName.isNotBlank()) {
                    "### Assistant (${m.modelName})"
                } else {
                    "### Assistant"
                }
                sb.append(header).append("\n\n")
                sb.append(m.aiResponseMarkdown.trimEnd()).append("\n\n")
            }

            if (idx < included.lastIndex) {
                sb.append("---\n\n")
            }
        }

        return sb.toString().trimEnd() + "\n"
    }
}
