package com.devoxx.genie.ui.compose.components

/**
 * What the chat's error card shows of a failed prompt's error. The card receives the full error text: long
 * provider errors are shortened until the user expands them, and Copy always takes the full text.
 */
internal object ErrorCardText {

    /** Characters shown before the card offers to expand the error. */
    const val COLLAPSED_LENGTH = 300

    /** Whether the error is long enough that the card shortens it and offers "Show full error". */
    fun isExpandable(fullText: String): Boolean = fullText.length > COLLAPSED_LENGTH

    /** The text the card shows: the full error when expanded or short enough, otherwise its start and an ellipsis. */
    fun displayed(fullText: String, expanded: Boolean): String =
        if (expanded || !isExpandable(fullText)) fullText else fullText.take(COLLAPSED_LENGTH) + "…"

    /** The text the Copy button puts on the clipboard: always the full error. */
    fun copyText(fullText: String): String = fullText
}
