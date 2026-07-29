package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.model.TerminalState

/**
 * Issue #1241: messages created by a steering split (frozen copies and continuation
 * areas) hide their AI bubble while there is nothing to show — an empty header-only
 * frame would visually break the question/answer sequence. Loading and terminal
 * states still render; regular messages are unaffected.
 */
internal fun shouldHideAiBubble(message: MessageUiModel): Boolean {
    val partOfSteeringSplit = message.isSteeringFrozen || message.aiContentOffset > 0
    return partOfSteeringSplit &&
        message.aiResponseMarkdown.isBlank() &&
        message.terminalState == TerminalState.COMPLETED &&
        !message.isLoadingIndicatorVisible
}

@Composable
fun MessagePair(
    message: MessageUiModel,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRetryClick: (String) -> Unit = {},
    onOpenAgentSettings: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    cachedAiBubbleHeight: Dp = Dp.Unspecified,
    onAiBubbleMeasured: (Dp) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // User prompt bubble
        UserBubble(promptText = message.userPrompt)

        // A steering message (issue #1241) is a user bubble only — the AI output
        // continues in the dedicated continuation message that follows it.
        if (message.isSteeringOnly) {
            return
        }

        // Activity section (MCP/Agent logs)
        if (message.activityEntries.isNotEmpty()) {
            ActivitySection(
                entries = message.activityEntries,
                visible = message.activitySectionVisible,
                completed = message.mcpLogsCompleted,
                showToolEntries = message.showToolActivity,
                onOpenLogs = onOpenLogs,
            )
        }

        if (message.thinkingMarkdown.isNotBlank()) {
            ThinkingBubble(thinkingMarkdown = message.thinkingMarkdown)
            Spacer(Modifier.height(4.dp))
        }

        // AI response bubble
        if (!shouldHideAiBubble(message)) {
            AiBubble(
                message = message,
                onRetryClick = onRetryClick,
                onOpenAgentSettings = onOpenAgentSettings,
                cachedHeight = cachedAiBubbleHeight,
                onMeasured = onAiBubbleMeasured,
            )
        }

        // File references
        if (message.fileReferences.isNotEmpty()) {
            FileReferencesSection(
                files = message.fileReferences,
                onFileClick = onFileClick,
            )
        }
    }
}
