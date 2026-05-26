package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.util.ChatTranscriptFormatter

/**
 * A thin toolbar rendered above the chat's [LazyColumn] containing a "Copy chat"
 * button that copies the full Markdown transcript of the conversation.
 *
 * Hidden whenever there is no message with a non-blank user prompt.
 */
@Composable
fun ConversationToolbar(
    messages: List<MessageUiModel>,
    modifier: Modifier = Modifier,
) {
    if (messages.none { it.userPrompt.isNotBlank() }) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        CopyButton(
            textToCopy = ChatTranscriptFormatter.toMarkdown(messages),
            label = "\u2398 Copy chat",
            copiedLabel = "\u2713 Chat copied",
        )
    }
}
