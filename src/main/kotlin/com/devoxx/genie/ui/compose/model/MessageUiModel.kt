package com.devoxx.genie.ui.compose.model

import com.devoxx.genie.model.activity.ActivityMessage

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
)
