package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.MessageUiModel

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

        // AI response bubble
        AiBubble(
            message = message,
            onRetryClick = onRetryClick,
            onOpenAgentSettings = onOpenAgentSettings,
            cachedHeight = cachedAiBubbleHeight,
            onMeasured = onAiBubbleMeasured,
        )

        // File references
        if (message.fileReferences.isNotEmpty()) {
            FileReferencesSection(
                files = message.fileReferences,
                onFileClick = onFileClick,
            )
        }
    }
}
