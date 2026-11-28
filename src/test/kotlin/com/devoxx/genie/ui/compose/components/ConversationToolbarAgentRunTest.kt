package com.devoxx.genie.ui.compose.components

import com.devoxx.genie.ui.compose.model.AgentRunUiState
import com.devoxx.genie.ui.compose.model.MessageUiModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** TASK-284: visible/enabled/tooltip rules of the chat toolbar's "View agent run" button. */
class ConversationToolbarAgentRunTest {

    private val ready = AgentRunUiState(visible = true, hasTrace = true)

    private fun message(
        id: String,
        streaming: Boolean = false,
        loading: Boolean = false,
        steeringOnly: Boolean = false,
    ) = MessageUiModel(
        id = id,
        userPrompt = "hi",
        isStreaming = streaming,
        isLoadingIndicatorVisible = loading,
        isSteeringOnly = steeringOnly,
    )

    @Test
    fun `hidden while trace recording is off`() {
        assertThat(AgentRunUiState.HIDDEN.visible).isFalse()
        assertThat(viewAgentRunEnabled(AgentRunUiState.HIDDEN, inFlight = false)).isFalse()
    }

    @Test
    fun `enabled only when visible, with a trace, and no response in flight`() {
        assertThat(viewAgentRunEnabled(ready, inFlight = false)).isTrue()
        assertThat(viewAgentRunEnabled(ready, inFlight = true)).isFalse()
        assertThat(viewAgentRunEnabled(ready.copy(hasTrace = false), inFlight = false)).isFalse()
    }

    @Test
    fun `tooltip explains why the button is disabled`() {
        assertThat(viewAgentRunTooltip(ready, inFlight = true)).contains("Wait for the response")
        assertThat(viewAgentRunTooltip(ready.copy(hasTrace = false), inFlight = false)).contains("No agent trace")
        assertThat(viewAgentRunTooltip(ready, inFlight = false)).contains("Open this conversation's agent run")
    }

    @Test
    fun `a streaming response is in flight`() {
        assertThat(responseInFlight(listOf(message("1"), message("2", streaming = true)))).isTrue()
    }

    @Test
    fun `a finished conversation is not in flight`() {
        assertThat(responseInFlight(listOf(message("1"), message("2")))).isFalse()
        assertThat(responseInFlight(emptyList())).isFalse()
    }

    @Test
    fun `a non-streaming agent run is in flight while its loading indicator shows`() {
        // Non-streaming models never set isStreaming; the run is still going while the indicator shows.
        assertThat(responseInFlight(listOf(message("1", loading = true)))).isTrue()
    }

    @Test
    fun `a queued prompt bubble after the running response does not hide that it is in flight`() {
        val running = message("1", streaming = true, loading = true)
        val queued = message("queued-1", steeringOnly = true)
        assertThat(responseInFlight(listOf(running, queued))).isTrue()
    }

    @Test
    fun `a stale loading flag on an earlier turn does not count as in flight`() {
        // Web search, /find, CLI and ACP turns finish without clearing their loading flag; a later
        // finished turn must not be treated as still running because of it.
        val staleWebSearchTurn = message("1", streaming = true, loading = true)
        val finishedAgentTurn = message("2")
        assertThat(responseInFlight(listOf(staleWebSearchTurn, finishedAgentTurn))).isFalse()
    }
}
