package com.devoxx.genie.ui.compose.model

/**
 * State of the chat toolbar's "Upload to Hugging Face" button (task-280).
 *
 * @property visible   shown only while "Record agent traces" is enabled
 * @property hasTrace  the current conversation has recorded trace events to upload
 * @property uploading an upload of this conversation is running
 */
data class TraceUploadUiState(
    val visible: Boolean = false,
    val hasTrace: Boolean = false,
    val uploading: Boolean = false,
) {
    companion object {
        @JvmField
        val HIDDEN = TraceUploadUiState()
    }
}
