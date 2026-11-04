package com.devoxx.genie.ui.compose.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize

/**
 * Scroll state of one conversation's [ChatScreen], held outside the composition.
 *
 * Switching DevoxxGenie tabs removes the chat's ComposePanel from the Swing hierarchy, which
 * disposes its composition and everything `remember`ed inside it. Without this holder the
 * returning tab would rebuild with the list at the top, every Markdown bubble at its tiny
 * first-frame height, and tail-following on — so it visibly animated down through the whole
 * transcript. The owning view model keeps one instance per conversation.
 */
class ChatScrollMemory {
    val listState = LazyListState()

    /** Last measured size of each finished AI bubble, keyed by message id. */
    val aiBubbleSizes = mutableStateMapOf<String, IntSize>()

    /** Whether the list follows the growing tail of the last message. */
    var autoFollow by mutableStateOf(true)
}
