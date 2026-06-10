package com.devoxx.genie.ui.compose.components

import com.devoxx.genie.ui.compose.model.ActivityEntryUiModel
import com.devoxx.genie.ui.compose.model.ActivityStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for the derived elapsed-time suffix shown on long-running activity rows (task-236).
 */
class ActivitySectionElapsedTest {

    private fun runningEntry(startedAt: Long) = ActivityEntryUiModel(
        source = "AGENT",
        content = "",
        toolName = "run_command",
        status = ActivityStatus.RUNNING,
        startedAt = startedAt,
        isToolActivity = true,
    )

    @Test
    fun `no suffix while a running tool is under the two second threshold`() {
        val now = 100_000L
        assertThat(elapsedSuffix(runningEntry(startedAt = now - 1_500), now)).isNull()
    }

    @Test
    fun `suffix appears once a tool has been running for more than two seconds`() {
        val now = 100_000L
        assertThat(elapsedSuffix(runningEntry(startedAt = now - 12_000), now))
            .isEqualTo("running\u2026 12s")
    }

    @Test
    fun `no suffix for resolved or lifecycle-less entries`() {
        val now = 100_000L
        val success = runningEntry(startedAt = now - 12_000).copy(status = ActivityStatus.SUCCESS)
        val errored = runningEntry(startedAt = now - 12_000).copy(status = ActivityStatus.ERROR)
        val info = runningEntry(startedAt = now - 12_000).copy(status = ActivityStatus.INFO)
        assertThat(elapsedSuffix(success, now)).isNull()
        assertThat(elapsedSuffix(errored, now)).isNull()
        assertThat(elapsedSuffix(info, now)).isNull()
    }

    @Test
    fun `no suffix for entries without a start timestamp`() {
        val entry = ActivityEntryUiModel(
            source = "AGENT",
            content = "reasoning",
            status = ActivityStatus.RUNNING,
        )
        assertThat(elapsedSuffix(entry, 100_000L)).isNull()
    }
}
