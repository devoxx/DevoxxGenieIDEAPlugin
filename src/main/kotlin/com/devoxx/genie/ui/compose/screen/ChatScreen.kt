package com.devoxx.genie.ui.compose.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.components.MessagePair
import com.devoxx.genie.ui.compose.model.MessageUiModel

@Composable
fun ChatScreen(
    messages: List<MessageUiModel>,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll only when user is already near the bottom
    val shouldAutoScroll by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= messages.size - 2
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.aiResponseMarkdown) {
        if (shouldAutoScroll && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(vertical = 4.dp),
    ) {
        itemsIndexed(
            items = messages,
            key = { _, message -> message.id },
        ) { _, message ->
            MessagePair(
                message = message,
                onFileClick = onFileClick,
            )
        }
    }
}
