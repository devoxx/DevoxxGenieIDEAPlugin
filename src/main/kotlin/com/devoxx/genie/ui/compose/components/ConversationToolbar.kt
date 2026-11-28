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
import com.devoxx.genie.ui.compose.model.AgentRunUiState
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor
import com.devoxx.genie.ui.compose.util.ChatTranscriptFormatter

/**
 * A thin toolbar rendered above the chat's [LazyColumn] containing a "Copy chat"
 * button that copies the full Markdown transcript of the conversation, and — while agent
 * trace recording is on — a "View agent run" button that opens the conversation's run view
 * (TASK-284). Saving and uploading a trace live in that view's Export menu.
 *
 * Hidden whenever there is no message with a non-blank user prompt.
 */
@Composable
fun ConversationToolbar(
    messages: List<MessageUiModel>,
    modifier: Modifier = Modifier,
    agentRun: AgentRunUiState = AgentRunUiState.HIDDEN,
    onViewAgentRunClick: () -> Unit = {},
) {
    if (messages.none { it.userPrompt.isNotBlank() }) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (agentRun.visible) {
            val inFlight = responseInFlight(messages)
            ToolbarTextButton(
                label = "◎ View agent run",
                enabled = viewAgentRunEnabled(agentRun, inFlight),
                tooltip = viewAgentRunTooltip(agentRun, inFlight),
                onClick = onViewAgentRunClick,
            )
        }
        CopyButton(
            textToCopy = ChatTranscriptFormatter.toMarkdown(messages),
            label = "⎘ Copy chat",
            copiedLabel = "✓ Chat copied",
        )
    }
}

/**
 * Whether a response is still being produced, judged by the latest real turn: queued and
 * steering bubbles are appended after the running response, so they are skipped. Both flags
 * count, since a non-streaming run never sets `isStreaming`. Earlier turns are ignored on
 * purpose: web search, /find, CLI and ACP turns finish without clearing their flags.
 */
internal fun responseInFlight(messages: List<MessageUiModel>): Boolean =
    messages.lastOrNull { !it.isSteeringOnly }?.let { it.isStreaming || it.isLoadingIndicatorVisible } == true

internal fun viewAgentRunEnabled(state: AgentRunUiState, inFlight: Boolean): Boolean =
    state.visible && state.hasTrace && !inFlight

internal fun viewAgentRunTooltip(state: AgentRunUiState, inFlight: Boolean): String = when {
    inFlight -> "Wait for the response to finish to view its agent run"
    !state.hasTrace -> "No agent trace recorded for this conversation yet (only turns that use tools are recorded)"
    else -> "Open this conversation's agent run: every step, tool call and result, with export and upload"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarTextButton(label: String, enabled: Boolean, tooltip: String, onClick: () -> Unit) {
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
