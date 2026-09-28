package com.devoxx.genie.chatmodel.cloud.anthropic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeModelsTest {

    @Test
    void rejectsTemperatureForClaudeModelsWithoutSamplingParameters() {
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-opus-4-7")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("us.anthropic.claude-opus-4-8")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-opus-5")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-opus-5-5")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-sonnet-5")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("claude-sonnet-5-5")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-sonnet-5-5")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-fable-5-1")).isTrue();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-mythos-5-1")).isTrue();

        assertThat(ClaudeModels.rejectsTemperature("global.anthropic.claude-opus-4-6-v1")).isFalse();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-sonnet-4-6")).isFalse();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-sonnet-4-5-20250929-v1:0")).isFalse();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-haiku-4-5-20251001-v1:0")).isFalse();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-opus-4-20250514-v1:0")).isFalse();
        assertThat(ClaudeModels.rejectsTemperature("anthropic.claude-3-7-sonnet-20250219-v1:0")).isFalse();
    }
}
