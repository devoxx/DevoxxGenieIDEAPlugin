package com.devoxx.genie.service.agent.loop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunMetricsTest {

    private static final long MS = 1_000_000L;

    @Test
    void wallTime_mergesOverlappingIntervals_whileSummedTimeAddsThemAll() {
        AgentRunMetrics metrics = new AgentRunMetrics();
        metrics.recordToolCall("a", 0, 100 * MS, false, false);
        metrics.recordToolCall("b", 50 * MS, 150 * MS, false, false);   // overlaps a
        metrics.recordToolCall("c", 200 * MS, 250 * MS, true, false);   // separate

        assertThat(metrics.getSummedToolMillis()).isEqualTo(250);
        assertThat(metrics.getWallToolMillis()).isEqualTo(200);
        assertThat(metrics.getExecutedToolCalls()).isEqualTo(3);
        assertThat(metrics.getToolStats().get("c").errors()).isEqualTo(1);
    }

    @Test
    void cacheHits_areCountedButNotTimed() {
        AgentRunMetrics metrics = new AgentRunMetrics();
        metrics.recordToolCall("read_file", 0, 10 * MS, false, false);
        metrics.recordToolCall("read_file", 20 * MS, 20 * MS, false, true);

        assertThat(metrics.getCacheHits()).isEqualTo(1);
        assertThat(metrics.getExecutedToolCalls()).isEqualTo(1);
        assertThat(metrics.getToolStats().get("read_file").calls()).isEqualTo(2);
        assertThat(metrics.getToolStats().get("read_file").cached()).isEqualTo(1);
    }

    @Test
    void summary_reportsAllCounters() {
        AgentRunMetrics metrics = new AgentRunMetrics();
        metrics.recordRequest(1_234);
        metrics.recordCompaction(2, 9_000);
        metrics.recordHiddenToolSpecs(30);
        metrics.recordToolsUnlocked(3);
        metrics.recordValidationReject();
        metrics.recordRetry();
        metrics.recordRepeatBlocked();
        metrics.recordTokens(100, 20);

        String summary = metrics.summary();

        assertThat(summary)
                .contains("LLM round trips: 1")
                .contains("request chars sent: 1234")
                .contains("tokens in/out: 100/20")
                .contains("2 older tool results trimmed, 9000 chars not re-sent")
                .contains("30 tool definitions withheld, 3 unlocked")
                .contains("1 calls rejected by local argument validation, 1 transient-failure retries")
                .contains("repeat calls blocked: 1");
    }

    @Test
    void wallTime_isZeroWithoutCalls() {
        assertThat(new AgentRunMetrics().getWallToolMillis()).isZero();
    }
}
