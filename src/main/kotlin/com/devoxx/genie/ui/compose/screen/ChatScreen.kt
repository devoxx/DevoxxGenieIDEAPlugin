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
import androidx.compose.ui.unit.Dp
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

    // Last measured height of each finished AI bubble, keyed by message id. A bubble's
    // Markdown content reports a tiny height on its first frame and grows once parsed; when
    // a recycled item re-enters at the top of the viewport that one-frame growth makes
    // LazyColumn re-pin the item's top edge, snapping the viewport to the start of the
    // message. Replaying the cached height as a minimum on re-entry keeps the first frame
    // at full size so the anchor is preserved. Survives recomposition (not config changes).
    val aiBubbleHeights = remember { mutableStateMapOf<String, Dp>() }

    // Whether the list should follow the growing tail of the last message. Disabled the
    // moment the user scrolls up (see nestedScrollConnection), re-enabled when the user
    // returns to the bottom or a new message is submitted.
    var autoFollow by remember { mutableStateOf(true) }

    // True when the very end of the conversation is visible. The bottom anchor is the
    // last list item, so "at bottom" means that anchor is laid out and its bottom edge is
    // within the viewport. The tolerance is kept tiny (2px, rounding only): a larger fuzzy
    // window combined with the 1dp anchor would treat a small upward scroll as "still at
    // bottom" and immediately re-enable following, yanking the reader back down.
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            last == null || (
                last.index == info.totalItemsCount - 1 &&
                    last.offset + last.size <= info.viewportEndOffset + 2
                )
        }
    }

    // Re-enable following only when the end of the conversation is actually reached AND the
    // user is not mid-gesture. Without the isScrollInProgress guard, the brief moment a
    // streaming update relayouts the list can satisfy isAtBottom and flip autoFollow back
    // on while the user is still dragging upward.
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !listState.isScrollInProgress) autoFollow = true
    }

    // Only user gestures (drag / mouse wheel / fling) pass through the nested-scroll chain;
    // programmatic animateScrollToItem calls do not. Any upward user scroll stops following
    // so streaming updates can't yank the reader back down. In Compose a scroll that reveals
    // earlier content (finger/content moving down) reports a positive available.y.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0.5f) autoFollow = false
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
    // The isScrollInProgress guard is essential: a streaming token must never start a
    // programmatic scroll while the user is dragging/flinging upward, otherwise the reader
    // is snapped back down to the start of the message below them.
    LaunchedEffect(messages.lastOrNull()?.aiResponseMarkdown) {
        if (autoFollow && messages.isNotEmpty() && !listState.isScrollInProgress) {
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
                    // Computed in `remember(message.id)` so an item scrolled out and back
                    // in (a fresh composition) re-checks `seenMessageIds` and resolves to
                    // false, never replaying the transition. See shouldPlayEntrance.
                    val playEntrance = remember(message.id) {
                        shouldPlayEntrance(
                            seenMessageIds = seenMessageIds,
                            messageId = message.id,
                            animationsEnabled = animationsEnabled,
                            isRestoring = isRestoring,
                        )
                    }
                    MessageEntrance(messageId = message.id, playEntrance = playEntrance) {
                        MessagePair(
                            message = message,
                            onFileClick = onFileClick,
                            onRetryClick = onRetryClick,
                            onOpenAgentSettings = onOpenAgentSettings,
                            onOpenLogs = onOpenLogs,
                            cachedAiBubbleHeight = aiBubbleHeights[message.id] ?: Dp.Unspecified,
                            onAiBubbleMeasured = { height -> aiBubbleHeights[message.id] = height },
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
 * Decides whether a bubble should play its one-shot entrance transition.
 *
 * Contract (intentionally side-effecting on [seenMessageIds] so the decision is made
 * exactly once per id): returns true only the FIRST time an id is ever observed while
 * the chat is live and animations are on. Every later call for the same id — including
 * when a recycled LazyColumn item re-enters composition after being scrolled away —
 * returns false, because the id is already in [seenMessageIds]. Keeping re-entering
 * items out of the animated path is what prevents the list from losing its scroll anchor
 * and snapping to the top of the conversation.
 *
 * [seenMessageIds] must be pre-seeded with the ids present on first composition so a
 * conversation restored from history does not replay entrances for its whole transcript.
 */
internal fun shouldPlayEntrance(
    seenMessageIds: MutableSet<String>,
    messageId: String,
    animationsEnabled: Boolean,
    isRestoring: Boolean,
): Boolean {
    val firstTimeSeen = seenMessageIds.add(messageId)
    return firstTimeSeen && animationsEnabled && !isRestoring
}

/**
 * Entrance animation for a newly inserted message bubble: fade-in plus a slight upward
 * slide (≤150ms).
 *
 * IMPORTANT — scroll anchoring: [AnimatedVisibility] sits in the measurement path of the
 * item it wraps and can report a transient/zero size while its transition settles. Inside
 * a [LazyColumn] that breaks the list's scroll-offset bookkeeping: when the user scrolls
 * up and a recycled item re-enters composition, the momentary size change makes the list
 * lose its anchor and snap `firstVisibleItemIndex` back to 0 — i.e. the viewport jumps to
 * the very top of the conversation.
 *
 * To avoid this we only ever put [AnimatedVisibility] in the layout path for the single
 * bubble that is genuinely playing its one-shot entrance ([playEntrance] == true). Every
 * other bubble — already seen, restored from history, animations disabled, or simply
 * scrolled back into view — renders [content] directly at its full, stable height, so
 * scrolling never disturbs the list anchor.
 */
@Composable
private fun MessageEntrance(
    messageId: String,
    playEntrance: Boolean,
    content: @Composable () -> Unit,
) {
    if (!playEntrance) {
        // Stable, full-height content with no transition in the measurement path.
        content()
        return
    }

    val entranceState = remember(messageId) {
        MutableTransitionState(initialState = false).apply { targetState = true }
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
