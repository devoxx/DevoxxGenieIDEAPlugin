package com.devoxx.genie.ui.compose.components

import com.devoxx.genie.ui.compose.model.ActivityEntryUiModel
import com.devoxx.genie.ui.compose.model.ActivityStatus
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.model.TerminalState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * TASK-288: the in-flight progress line, so a slow agent run (60–90 s of model thinking
 * between tool calls on a local model) no longer looks stuck.
 */
class AgentProgressLineTest {

    private val submitted = 1_000_000L

    private fun inFlight(vararg entries: ActivityEntryUiModel) = MessageUiModel(
        id = "m1",
        userPrompt = "find things",
        timestamp = submitted,
        isStreaming = true,
        isLoadingIndicatorVisible = true,
        activityEntries = entries.toList(),
    )

    private fun tool(name: String, call: Int, status: ActivityStatus, subAgent: String? = null) = ActivityEntryUiModel(
        source = "AGENT",
        content = "",
        toolName = name,
        callNumber = call,
        maxCalls = 50,
        status = status,
        isToolActivity = true,
        subAgentId = subAgent,
    )

    @Test
    fun `quick replies never show the line`() {
        assertThat(progressLineText(inFlight(), submitted + 4_000)).isNull()
    }

    @Test
    fun `without tool events it still shows the elapsed time`() {
        assertThat(progressLineText(inFlight(), submitted + 372_000)).isEqualTo("Working… · 6:12")
    }

    @Test
    fun `while a tool runs it names the tool and the step`() {
        val message = inFlight(
            tool("search_files", 28, ActivityStatus.SUCCESS),
            tool("run_script", 29, ActivityStatus.RUNNING),
        )
        assertThat(progressLineText(message, submitted + 372_000))
            .isEqualTo("Running run_script… (step 29/50) · 6:12")
    }

    @Test
    fun `a running tool is shown right away, without the elapsed time before the threshold`() {
        val message = inFlight(tool("run_script", 1, ActivityStatus.RUNNING))
        assertThat(progressLineText(message, submitted + 1_000)).isEqualTo("Running run_script… (step 1/50)")
    }

    @Test
    fun `between tool calls it says the agent is waiting for the model`() {
        val message = inFlight(
            tool("run_script", 28, ActivityStatus.SUCCESS),
            tool("run_script", 29, ActivityStatus.ERROR),
        )
        assertThat(progressLineText(message, submitted + 372_000))
            .isEqualTo("Waiting for the model · 29/50 tool calls · 6:12")
    }

    @Test
    fun `sub-agent tool events don't change the main agent's step`() {
        val message = inFlight(
            tool("parallel_explore", 3, ActivityStatus.SUCCESS),
            tool("search_files", 40, ActivityStatus.SUCCESS, subAgent = "sub-agent-1"),
            tool("read_file", 41, ActivityStatus.RUNNING, subAgent = "sub-agent-1"),
        )
        assertThat(progressLineText(message, submitted + 65_000))
            .isEqualTo("Waiting for the model · 3/50 tool calls · 1:05")
    }

    @Test
    fun `a pending approval explains the pause`() {
        val message = inFlight(tool("run_command", 5, ActivityStatus.PENDING_APPROVAL))
        assertThat(progressLineText(message, submitted + 30_000))
            .isEqualTo("Waiting for your approval… (run_command) · 0:30")
    }

    @Test
    fun `no line once the run has finished, errored or was stopped`() {
        val now = submitted + 372_000
        assertThat(progressLineText(inFlight().copy(isStreaming = false, isLoadingIndicatorVisible = false), now)).isNull()
        assertThat(progressLineText(inFlight().copy(terminalState = TerminalState.STOPPED), now)).isNull()
        assertThat(progressLineText(inFlight().copy(terminalState = TerminalState.ERROR), now)).isNull()
    }

    @Test
    fun `elapsed time formatting`() {
        assertThat(formatRunElapsed(5_000)).isEqualTo("0:05")
        assertThat(formatRunElapsed(372_000)).isEqualTo("6:12")
        assertThat(formatRunElapsed(3_725_000)).isEqualTo("1:02:05")
    }
}
