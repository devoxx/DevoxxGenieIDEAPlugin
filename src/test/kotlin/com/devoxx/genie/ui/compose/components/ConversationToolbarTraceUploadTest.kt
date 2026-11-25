package com.devoxx.genie.ui.compose.components

import com.devoxx.genie.ui.compose.model.TraceUploadUiState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** task-280: enable/label/tooltip rules of the chat toolbar's "Upload to Hugging Face" button. */
class ConversationToolbarTraceUploadTest {

    private val ready = TraceUploadUiState(visible = true, hasTrace = true, uploading = false)

    @Test
    fun `enabled only when visible, with a trace, not uploading and not streaming`() {
        assertThat(traceUploadEnabled(ready, streaming = false)).isTrue()
        assertThat(traceUploadEnabled(ready, streaming = true)).isFalse()
        assertThat(traceUploadEnabled(ready.copy(hasTrace = false), streaming = false)).isFalse()
        assertThat(traceUploadEnabled(ready.copy(uploading = true), streaming = false)).isFalse()
        assertThat(traceUploadEnabled(TraceUploadUiState.HIDDEN, streaming = false)).isFalse()
    }

    @Test
    fun `tooltip explains why the button is disabled`() {
        assertThat(traceUploadTooltip(ready, streaming = true)).contains("Wait for the response")
        assertThat(traceUploadTooltip(ready.copy(hasTrace = false), streaming = false)).contains("No agent trace")
        assertThat(traceUploadTooltip(ready.copy(uploading = true), streaming = false)).contains("Uploading")
        assertThat(traceUploadTooltip(ready, streaming = false)).contains("Upload this conversation")
    }

    @Test
    fun `label shows progress while uploading`() {
        assertThat(traceUploadLabel(ready)).contains("Upload to Hugging Face")
        assertThat(traceUploadLabel(ready.copy(uploading = true))).contains("Uploading")
    }
}
