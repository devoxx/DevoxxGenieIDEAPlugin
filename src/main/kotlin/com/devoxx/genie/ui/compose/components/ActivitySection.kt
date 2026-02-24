package com.devoxx.genie.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.model.ActivityEntryUiModel
import com.devoxx.genie.ui.compose.theme.DevoxxBlue
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor

@Composable
fun ActivitySection(
    entries: List<ActivityEntryUiModel>,
    visible: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return

    var expanded by remember { mutableStateOf(!completed) }
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = if (expanded) "\u25BC" else "\u25B6",
                style = typography.caption.copy(color = DevoxxBlue),
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                text = "Activity (${entries.size})",
                style = typography.caption.copy(color = DevoxxBlue),
            )
            if (completed) {
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = "completed",
                    style = typography.caption.copy(color = colors.textSecondary),
                )
            }
        }

        // Entries
        AnimatedVisibility(
            visible = expanded && visible,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.codeBackground, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                entries.forEach { entry ->
                    ActivityEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun ActivityEntryRow(entry: ActivityEntryUiModel) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = "[${entry.source}]",
                style = typography.caption.copy(
                    color = DevoxxBlue,
                    fontFamily = FontFamily.Monospace,
                ),
            )
            if (entry.toolName != null) {
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = entry.toolName,
                    style = typography.caption.copy(
                        fontSize = 11.sp,
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
            if (entry.callNumber > 0) {
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = "(${entry.callNumber}/${entry.maxCalls})",
                    style = typography.caption.copy(color = colors.textSecondary),
                )
            }
        }
        if (entry.content.isNotBlank()) {
            BasicText(
                text = entry.content,
                style = typography.caption.copy(color = colors.textSecondary),
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
