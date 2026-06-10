package com.devoxx.genie.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.ActivityEntryUiModel
import com.devoxx.genie.ui.compose.model.ActivityStatus
import com.devoxx.genie.ui.compose.theme.DevoxxBlue
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor

private val SuccessGreen = Color(0xFF43A047)
private val ErrorRed = Color(0xFFE53935)

@Composable
fun ActivitySection(
    entries: List<ActivityEntryUiModel>,
    visible: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier,
    showToolEntries: Boolean = true,
    onOpenLogs: () -> Unit = {},
) {
    // Tool rows are opt-in ("Show tool activity in chat output"); reasoning rows always show
    val visibleEntries = if (showToolEntries) entries else entries.filter { !it.isToolActivity }
    if (visibleEntries.isEmpty()) return

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
                text = if (expanded) "▼" else "▶",
                style = typography.caption.copy(color = DevoxxBlue),
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                text = "Activity (${visibleEntries.size})",
                style = typography.caption.copy(color = DevoxxBlue),
            )
            if (completed) {
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = "completed",
                    style = typography.caption.copy(color = colors.textSecondary),
                )
            }
            Spacer(Modifier.weight(1f))
            BasicText(
                text = "Open Logs",
                style = typography.caption.copy(
                    color = DevoxxBlue,
                    textDecoration = TextDecoration.Underline,
                ),
                modifier = Modifier.padding(horizontal = 4.dp).clickable(onClick = onOpenLogs),
            )
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
                visibleEntries.forEachIndexed { index, entry ->
                    ActivityEntryRow(entry, rowKey = "$index/${entry.toolName}/${entry.callNumber}")
                    entry.children.forEach { child ->
                        ActivityEntryRow(
                            child,
                            rowKey = "$index/${entry.callNumber}/${child.toolName}",
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityEntryRow(
    entry: ActivityEntryUiModel,
    rowKey: String,
    modifier: Modifier = Modifier,
) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    var detailsExpanded by remember(rowKey) { mutableStateOf(false) }
    val hasDetails = !entry.arguments.isNullOrBlank() || !entry.result.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .let { base ->
                if (hasDetails) base.clickable { detailsExpanded = !detailsExpanded } else base
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatusIcon(entry.status)
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = "[${entry.source}]",
                style = typography.caption.copy(
                    color = DevoxxBlue,
                    fontFamily = FontFamily.Monospace,
                ),
            )
            if (entry.toolName != null) {
                Spacer(Modifier.width(4.dp))
                val summary = summarizeToolAction(entry.toolName, entry.arguments)
                BasicText(
                    text = summary,
                    style = typography.caption.copy(
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (entry.callNumber > 0 && entry.maxCalls > 0) {
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

        // Inline detail area — full (already truncated at the ViewModel) arguments and result
        AnimatedVisibility(
            visible = detailsExpanded && hasDetails,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)) {
                if (!entry.arguments.isNullOrBlank()) {
                    DetailBlock(label = "arguments", text = entry.arguments)
                }
                if (!entry.result.isNullOrBlank()) {
                    if (!entry.arguments.isNullOrBlank()) Spacer(Modifier.height(4.dp))
                    DetailBlock(label = "result", text = entry.result)
                }
            }
        }
    }
}

@Composable
private fun DetailBlock(label: String, text: String) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = label,
            style = typography.caption.copy(color = colors.textSecondary),
        )
        BasicText(
            text = text,
            style = typography.caption.copy(
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.codeBackground, RoundedCornerShape(3.dp))
                .padding(4.dp),
        )
    }
}

/**
 * Per-row status glyph: animated spinner while RUNNING, green check on SUCCESS,
 * red cross on ERROR, pause glyph while PENDING_APPROVAL. INFO rows get a neutral dot.
 */
@Composable
private fun StatusIcon(status: ActivityStatus) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    when (status) {
        ActivityStatus.RUNNING -> RunningSpinner()
        ActivityStatus.SUCCESS -> BasicText(
            text = "✓",
            style = typography.caption.copy(color = SuccessGreen),
        )
        ActivityStatus.ERROR -> BasicText(
            text = "✗",
            style = typography.caption.copy(color = ErrorRed),
        )
        ActivityStatus.PENDING_APPROVAL -> BasicText(
            text = "⏸",
            style = typography.caption.copy(color = DevoxxBlue),
        )
        ActivityStatus.INFO -> BasicText(
            text = "•",
            style = typography.caption.copy(color = colors.textSecondary),
        )
    }
}

/** Small pulsing dot — same infiniteTransition pattern as ThinkingIndicator. */
@Composable
internal fun RunningSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Box(
        modifier = modifier
            .size(7.dp)
            .alpha(pulseAlpha)
            .background(DevoxxBlue, CircleShape),
    )
}

/**
 * Extracts a concise action summary from tool name and its JSON arguments.
 * For example: "edit_file /src/Main.java" or "list_files /src".
 */
private fun summarizeToolAction(toolName: String, arguments: String?): String {
    if (arguments.isNullOrBlank()) return toolName
    // Extract the first string value from the JSON arguments as a key identifier
    // Typical keys: "path", "file_path", "directory", "url", "query", "pattern", "command"
    val target = extractFirstStringArg(arguments)
    return if (target != null) "$toolName: $target" else toolName
}

private fun extractFirstStringArg(json: String): String? {
    // Lightweight extraction — look for common key names first, then fall back to first string value
    val priorityKeys = listOf("path", "file_path", "directory", "url", "query", "pattern", "command", "name")
    for (key in priorityKeys) {
        val match = Regex(""""$key"\s*:\s*"([^"]+)"""").find(json)
        if (match != null) return match.groupValues[1]
    }
    // Fall back to first string value in the JSON
    val firstValue = Regex(""":\s*"([^"]+)"""").find(json)
    return firstValue?.groupValues?.get(1)
}
