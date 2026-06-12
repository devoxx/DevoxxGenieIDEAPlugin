package com.devoxx.genie.ui.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatFontSizeServiceTest {

    @Test
    void clamp_keepsValuesWithinBounds() {
        assertThat(ChatFontSizeService.clamp(14)).isEqualTo(14);
        assertThat(ChatFontSizeService.clamp(ChatFontSizeService.MIN_FONT_SIZE)).isEqualTo(ChatFontSizeService.MIN_FONT_SIZE);
        assertThat(ChatFontSizeService.clamp(ChatFontSizeService.MAX_FONT_SIZE)).isEqualTo(ChatFontSizeService.MAX_FONT_SIZE);
    }

    @Test
    void clamp_clipsBelowMinimum() {
        assertThat(ChatFontSizeService.clamp(ChatFontSizeService.MIN_FONT_SIZE - 1))
                .isEqualTo(ChatFontSizeService.MIN_FONT_SIZE);
        assertThat(ChatFontSizeService.clamp(-100)).isEqualTo(ChatFontSizeService.MIN_FONT_SIZE);
    }

    @Test
    void clamp_clipsAboveMaximum() {
        assertThat(ChatFontSizeService.clamp(ChatFontSizeService.MAX_FONT_SIZE + 1))
                .isEqualTo(ChatFontSizeService.MAX_FONT_SIZE);
        assertThat(ChatFontSizeService.clamp(1000)).isEqualTo(ChatFontSizeService.MAX_FONT_SIZE);
    }

    @Test
    void nextSize_incrementsAndDecrementsWithinBounds() {
        assertThat(ChatFontSizeService.nextSize(14, +1)).isEqualTo(15);
        assertThat(ChatFontSizeService.nextSize(14, -1)).isEqualTo(13);
    }

    @Test
    void nextSize_doesNotExceedBounds() {
        assertThat(ChatFontSizeService.nextSize(ChatFontSizeService.MAX_FONT_SIZE, +1))
                .isEqualTo(ChatFontSizeService.MAX_FONT_SIZE);
        assertThat(ChatFontSizeService.nextSize(ChatFontSizeService.MIN_FONT_SIZE, -1))
                .isEqualTo(ChatFontSizeService.MIN_FONT_SIZE);
    }
}
