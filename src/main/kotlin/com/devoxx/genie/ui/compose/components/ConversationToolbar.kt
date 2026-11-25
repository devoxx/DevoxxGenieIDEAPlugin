package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.model.TraceUploadUiState
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor
import com.devoxx.genie.ui.compose.util.ChatTranscriptFormatter

/**
 * A thin toolbar rendered above the chat's [LazyColumn] containing a "Copy chat"
 * button that copies the full Markdown transcript of the conversation, and — while agent
 * trace recording is on — an "Upload to Hugging Face" button (task-280).
 *
 * Hidden whenever there is no message with a non-blank user prompt.
 */
@Composable
fun ConversationToolbar(
    messages: List<MessageUiModel>,
    modifier: Modifier = Modifier,
    traceUpload: TraceUploadUiState = TraceUploadUiState.HIDDEN,
    onTraceUploadClick: () -> Unit = {},
) {
    if (messages.none { it.userPrompt.isNotBlank() }) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (traceUpload.visible) {
            val streaming = messages.lastOrNull()?.isStreaming == true
            TraceUploadButton(
                label = traceUploadLabel(traceUpload),
                enabled = traceUploadEnabled(traceUpload, streaming),
                tooltip = traceUploadTooltip(traceUpload, streaming),
                onClick = onTraceUploadClick,
            )
        }
        CopyButton(
            textToCopy = ChatTranscriptFormatter.toMarkdown(messages),
            label = "⎘ Copy chat",
            copiedLabel = "✓ Chat copied",
        )
    }
}

internal fun traceUploadEnabled(state: TraceUploadUiState, streaming: Boolean): Boolean =
    state.visible && state.hasTrace && !state.uploading && !streaming

internal fun traceUploadLabel(state: TraceUploadUiState): String =
    if (state.uploading) "↻ Uploading…" else "⇪ Upload to Hugging Face"

internal fun traceUploadTooltip(state: TraceUploadUiState, streaming: Boolean): String = when {
    state.uploading -> "Uploading this conversation's trace to Hugging Face…"
    streaming -> "Wait for the response to finish before uploading"
    !state.hasTrace -> "No agent trace recorded for this conversation yet (only turns that use tools are recorded)"
    else -> "Upload this conversation as an agent trace to your Hugging Face dataset"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TraceUploadButton(label: String, enabled: Boolean, tooltip: String, onClick: () -> Unit) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    val tooltipShape = RoundedCornerShape(4.dp)
    TooltipArea(
        tooltip = {
            Box(
                modifier = Modifier
                    .shadow(4.dp, tooltipShape)
                    .background(Color(0xFF2B2D30), tooltipShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                BasicText(text = tooltip, style = typography.caption.copy(fontSize = 11.sp, color = Color.White))
            }
        },
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
    ) {
        BasicText(
            text = label,
            style = typography.caption.copy(
                color = if (enabled) colors.primary else colors.onSurface.copy(alpha = 0.3f),
            ),
            modifier = Modifier
                .padding(4.dp)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        )
    }
}
