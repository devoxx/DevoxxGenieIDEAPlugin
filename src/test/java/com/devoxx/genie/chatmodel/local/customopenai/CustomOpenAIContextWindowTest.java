package com.devoxx.genie.chatmodel.local.customopenai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOpenAIContextWindowTest {

    @Test
    void resolve_returnsDefault_whenNull() {
        assertThat(CustomOpenAIContextWindow.resolve(null))
                .isEqualTo(CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW);
    }

    @Test
    void resolve_returnsDefault_whenZeroOrNegative() {
        assertThat(CustomOpenAIContextWindow.resolve(0))
                .isEqualTo(CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW);
        assertThat(CustomOpenAIContextWindow.resolve(-1))
                .isEqualTo(CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW);
    }

    @Test
    void resolve_returnsConfiguredValue_whenPositive() {
        assertThat(CustomOpenAIContextWindow.resolve(32_000)).isEqualTo(32_000);
        assertThat(CustomOpenAIContextWindow.resolve(1)).isEqualTo(1);
    }
}
