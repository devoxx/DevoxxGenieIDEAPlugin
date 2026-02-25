package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.MessageUiModel

@Composable
fun MessagePair(
    message: MessageUiModel,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
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
            )
        }

        // AI response bubble
        AiBubble(message = message)

        // File references
        if (message.fileReferences.isNotEmpty()) {
            FileReferencesSection(
                files = message.fileReferences,
                onFileClick = onFileClick,
            )
        }
    }
}
