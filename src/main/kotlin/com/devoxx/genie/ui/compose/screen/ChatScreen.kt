package com.devoxx.genie.ui.compose.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.components.ConversationToolbar
import com.devoxx.genie.ui.compose.components.MessagePair
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.theme.DevoxxBlue
import com.devoxx.genie.ui.compose.util.IdeAnimations
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    messages: List<MessageUiModel>,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRetryClick: (String) -> Unit = {},
    onOpenAgentSettings: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    isRestoring: Boolean = false,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var previousMessageCount by remember { mutableStateOf(messages.size) }

    val animationsEnabled = remember { IdeAnimations.enabled() }

    // Message ids whose entrance animation has already played (or been skipped).
    // Pre-seeded with the messages present on first composition so a conversation
    // restored from history does not replay entrances for its whole transcript.
    // Because LazyColumn items use stable message-id keys, streaming updates to an
    // existing bubble recompose the same item and never re-trigger the entrance;
    // items scrolled out and back in are also skipped via this set.
    val seenMessageIds = remember { messages.mapTo(HashSet()) { it.id } }

    // Whether the list should follow the growing tail of the last message. Disabled the
    // moment the user scrolls up (see nestedScrollConnection), re-enabled when the user
    // returns to the bottom or a new message is submitted.
    var autoFollow by remember { mutableStateOf(true) }

    // True when the very end of the conversation is visible. The bottom anchor item is
    // the last list item, so "at bottom" means the last item's bottom edge is inside the
    // viewport (small tolerance for rounding).
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            last == null || (
                last.index == info.totalItemsCount - 1 &&
                    last.offset + last.size <= info.viewportEndOffset + 8
                )
        }
    }

    // Reaching the bottom by any means (manual scroll, button, programmatic) resumes following.
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) autoFollow = true
    }

    // Only user gestures (drag / mouse wheel / fling) pass through the nested-scroll chain;
    // programmatic animateScrollToItem calls do not. Any upward user scroll stops following
    // so streaming updates can't yank the reader back down.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) autoFollow = false
                return Offset.Zero
            }
        }
    }

    // Always scroll when a new message is added (user just submitted a prompt)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && messages.size > previousMessageCount) {
            autoFollow = true
            listState.animateScrollToItem(messages.lastIndex)
        }
        previousMessageCount = messages.size
    }

    // Follow the tail while streaming. Scrolling to the 1px bottom anchor (index ==
    // messages.size) shows the END of the growing last message; scrolling to the message
    // item itself would pin the viewport at its top once it outgrows the screen.
    LaunchedEffect(messages.lastOrNull()?.aiResponseMarkdown) {
        if (autoFollow && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ConversationToolbar(messages = messages)
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .padding(vertical = 4.dp),
            ) {
                itemsIndexed(
                    items = messages,
                    key = { _, message -> message.id },
                ) { _, message ->
                    // Play the entrance exactly once: the first time this id is ever
                    // composed, and only for messages inserted while the chat is live.
                    val playEntrance = remember(message.id) {
                        seenMessageIds.add(message.id) && animationsEnabled && !isRestoring
                    }
                    MessageEntrance(messageId = message.id, playEntrance = playEntrance) {
                        MessagePair(
                            message = message,
                            onFileClick = onFileClick,
                            onRetryClick = onRetryClick,
                            onOpenAgentSettings = onOpenAgentSettings,
                            onOpenLogs = onOpenLogs,
                        )
                    }
                }
                // Bottom anchor used as the scroll target for tail-following.
                item(key = "bottom-anchor") {
                    Spacer(Modifier.height(1.dp))
                }
            }

            ScrollToBottomButton(
                visible = shouldShowScrollToBottom(autoFollow, messages),
                onClick = {
                    autoFollow = true
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.size)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

/**
 * Entrance animation for a newly inserted message bubble: fade-in plus a slight upward
 * slide (≤150ms). Neither effect changes the measured item size, so tail-following and
 * scroll-to-bottom positions are not disturbed. When [playEntrance] is false (already
 * seen, restoring from history, or animations disabled) the content shows immediately.
 */
@Composable
private fun MessageEntrance(
    messageId: String,
    playEntrance: Boolean,
    content: @Composable () -> Unit,
) {
    val entranceState = remember(messageId) {
        MutableTransitionState(initialState = !playEntrance).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = entranceState,
        enter = fadeIn(tween(IdeAnimations.MESSAGE_ENTRANCE_MS)) +
            slideInVertically(tween(IdeAnimations.MESSAGE_ENTRANCE_MS)) { it / 6 },
        exit = ExitTransition.None,
    ) {
        content()
    }
}

/**
 * The scroll-to-bottom affordance is scoped to an in-flight stream: it appears only when
 * the user has scrolled away while the last message is still growing. Once streaming
 * ends, normal scrolling takes over and no floating button lingers over chat history.
 */
internal fun shouldShowScrollToBottom(autoFollow: Boolean, messages: List<MessageUiModel>): Boolean =
    !autoFollow && messages.lastOrNull()?.isStreaming == true

@Composable
private fun ScrollToBottomButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DevoxxBlue)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "↓",
                style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}
