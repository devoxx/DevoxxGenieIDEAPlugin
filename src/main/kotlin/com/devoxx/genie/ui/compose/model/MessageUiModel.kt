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

/**
 * Lifecycle of an activity timeline row. Tool calls start RUNNING and resolve to
 * SUCCESS or ERROR when their response arrives; PENDING_APPROVAL is an intermediate
 * state while the approval dialog is up. INFO is for entries with no lifecycle
 * (agent reasoning, MCP/RAG log lines) — terminal immediately, never a spinner.
 */
enum class ActivityStatus {
    RUNNING,
    SUCCESS,
    ERROR,
    PENDING_APPROVAL,
    INFO,
}

data class ActivityEntryUiModel(
    val source: String,
    val content: String,
    val toolName: String? = null,
    val arguments: String? = null,
    val result: String? = null,
    val callNumber: Int = 0,
    val maxCalls: Int = 0,
    val status: ActivityStatus = ActivityStatus.INFO,
    /**
     * Wall-clock millis when the tool request was observed; 0 for entries without a
     * lifecycle. Drives the live "running… Ns" suffix on long-running RUNNING rows.
     */
    val startedAt: Long = 0,
    /**
     * True for tool-call activity (requests, MCP messages) — rendering of these rows is
     * gated by [MessageUiModel.showToolActivity]. Agent reasoning entries are false and
     * always rendered.
     */
    val isToolActivity: Boolean = false,
    /** Set when the entry originates inside a parallel_explore sub-agent. */
    val subAgentId: String? = null,
    /** Indented child rows, e.g. sub-agents spawned by a parallel_explore call. */
    val children: List<ActivityEntryUiModel> = emptyList(),
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
    /**
     * Snapshot of the "Show tool activity in chat output" setting taken when the prompt
     * was submitted. Tool entries are always tracked in [activityEntries] (the live
     * status line needs them); this flag only gates rendering of the detailed rows.
     */
    val showToolActivity: Boolean = false,
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
