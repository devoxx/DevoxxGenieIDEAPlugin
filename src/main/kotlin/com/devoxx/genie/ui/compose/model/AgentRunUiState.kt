package com.devoxx.genie.ui.compose.model

/**
 * State of the chat toolbar's "View agent run" button (TASK-284; it replaced task-280's
 * "Upload to Hugging Face" button, whose upload now lives in the run view's Export menu).
 *
 * @property visible  shown only while "Record agent traces" is enabled
 * @property hasTrace the current conversation has recorded trace events to show
 */
data class AgentRunUiState(
    val visible: Boolean = false,
    val hasTrace: Boolean = false,
) {
    companion object {
        @JvmField
        val HIDDEN = AgentRunUiState()
    }
}
