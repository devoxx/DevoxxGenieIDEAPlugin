package com.devoxx.genie.chatmodel.local.customopenai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOpenAICostTest {

    @Test
    void resolve_returnsZero_whenNull() {
        assertThat(CustomOpenAICost.resolve(null)).isZero();
    }

    @Test
    void resolve_returnsZero_whenNegative() {
        assertThat(CustomOpenAICost.resolve(-1.0)).isZero();
    }

    @Test
    void resolve_returnsConfiguredValue_whenPositive() {
        assertThat(CustomOpenAICost.resolve(3.0)).isEqualTo(3.0);
        assertThat(CustomOpenAICost.resolve(0.15)).isEqualTo(0.15);
    }
}
