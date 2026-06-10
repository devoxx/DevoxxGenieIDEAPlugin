package com.devoxx.genie.ui.compose.model

import com.devoxx.genie.model.activity.ActivityMessage

/**
 * How a message's execution ended. COMPLETED is both the in-flight default and the
 * normal end state; the other three are explicit terminal states that are mutually
 * exclusive and final — once set, a message can never transition to another state
 * (a stopped message cannot be flipped back to completed by a straggling token).
 *
 * Persistence note (v1 decision): terminal states are session-only. Conversations
 * restored from history are created via addChatMessage and therefore default to
 * COMPLETED — old persisted data renders gracefully without any schema change.
 */
enum class TerminalState {
    COMPLETED,
    STOPPED,
    ERROR,
    LOOP_LIMIT,
}

data class TokenUsageInfo(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cost: Double = 0.0,
)

data class FileReferenceUiModel(
    val filePath: String,
    val fileName: String,
)

data class ActivityEntryUiModel(
    val source: String,
    val content: String,
    val toolName: String? = null,
    val arguments: String? = null,
    val result: String? = null,
    val callNumber: Int = 0,
    val maxCalls: Int = 0,
)

data class MessageUiModel(
    val id: String,
    val userPrompt: String,
    val aiResponseMarkdown: String = "",
    val modelName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val executionTimeMs: Long = 0,
    val tokenUsage: TokenUsageInfo = TokenUsageInfo(),
    val isStreaming: Boolean = false,
    val isLoadingIndicatorVisible: Boolean = false,
    val fileReferences: List<FileReferenceUiModel> = emptyList(),
    val activityEntries: List<ActivityEntryUiModel> = emptyList(),
    val activitySectionVisible: Boolean = true,
    val mcpLogsCompleted: Boolean = false,
    val intermediateTexts: List<String> = emptyList(),
    /** Terminal state of this message — defaults to COMPLETED for backward compat. */
    val terminalState: TerminalState = TerminalState.COMPLETED,
    /** Human-readable error summary, only meaningful when [terminalState] is ERROR. */
    val errorText: String? = null,
    /** Configured max tool calls, only meaningful when [terminalState] is LOOP_LIMIT. */
    val loopLimitMaxCalls: Int = 0,
    /** True once the user clicked Retry on this message's error card (one-shot guard). */
    val retryAttempted: Boolean = false,
)
