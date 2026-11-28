package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.ActivityStatus
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.model.TerminalState
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor
import kotlinx.coroutines.delay

/**
 * In-flight runs shorter than this show no progress line (unless a tool is running), so quick
 * replies don't flicker. TASK-288.
 */
internal const val PROGRESS_LINE_THRESHOLD_MS = 5_000L

private const val SEPARATOR = " · "

/**
 * The one-line progress status shown while a response is in flight (TASK-288), or `null` when
 * nothing should be shown. With a slow local model an agent run spends most of its time between
 * tool calls, waiting for the model; this line keeps that visible instead of looking stuck.
 *
 * - a tool is running: "Running run_script… (step 29/50) · 6:12"
 * - between tool calls: "Waiting for the model · 29/50 tool calls · 6:12"
 * - no tool events (agent debug logs off): "Working… · 6:12"
 *
 * Only the main agent's tool events count; sub-agent events (parallel_explore) don't change the step.
 * Elapsed time runs from the prompt submission ([MessageUiModel.timestamp]).
 */
internal fun progressLineText(message: MessageUiModel, nowMs: Long): String? {
    val inFlight = message.isLoadingIndicatorVisible || message.isStreaming
    if (!inFlight || message.terminalState != TerminalState.COMPLETED) return null

    val elapsedMs = (nowMs - message.timestamp).coerceAtLeast(0)
    val elapsed = if (elapsedMs >= PROGRESS_LINE_THRESHOLD_MS) SEPARATOR + formatRunElapsed(elapsedMs) else ""
    val mainCalls = message.activityEntries.filter { it.isToolActivity && it.subAgentId == null && it.callNumber > 0 }

    val open = mainCalls.lastOrNull {
        it.status == ActivityStatus.RUNNING || it.status == ActivityStatus.PENDING_APPROVAL
    }
    if (open != null) {
        val tool = open.toolName ?: "tool"
        return if (open.status == ActivityStatus.PENDING_APPROVAL) {
            "Waiting for your approval… ($tool)$elapsed"
        } else {
            val step = if (open.maxCalls > 0) " (step ${open.callNumber}/${open.maxCalls})" else " (step ${open.callNumber})"
            "Running $tool…$step$elapsed"
        }
    }

    if (elapsed.isEmpty()) return null
    val latest = mainCalls.maxByOrNull { it.callNumber }
        ?: return "Working…$elapsed"
    val used = if (latest.maxCalls > 0) "${latest.callNumber}/${latest.maxCalls}" else "${latest.callNumber}"
    return "Waiting for the model${SEPARATOR}$used tool calls$elapsed"
}

/** `0:05`, `6:12`, `1:02:05`. */
internal fun formatRunElapsed(ms: Long): String {
    val totalSeconds = ms / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Live progress line for an in-flight message (TASK-288), re-evaluated every second while the
 * run is in flight. Replaces the tool-only status line, which disappeared between tool calls.
 */
@Composable
internal fun AgentProgressLine(message: MessageUiModel) {
    val inFlight = (message.isLoadingIndicatorVisible || message.isStreaming) &&
        message.terminalState == TerminalState.COMPLETED
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    // Keyed on inFlight: once the run ends the effect restarts with inFlight = false and exits.
    LaunchedEffect(inFlight) {
        while (inFlight) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val text = progressLineText(message, nowMs) ?: return

    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        RunningSpinner()
        Spacer(Modifier.width(6.dp))
        BasicText(text = text, style = typography.caption.copy(color = colors.textSecondary))
    }
}
