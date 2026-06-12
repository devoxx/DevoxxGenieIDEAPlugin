package com.devoxx.genie.ui.util;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;

import static com.devoxx.genie.ui.topic.AppTopics.APPEARANCE_SETTINGS_TOPIC;

/**
 * Adjusts the chat output font size (prose and code together) and persists it.
 * <p>
 * The new size is stored in {@link DevoxxGenieStateService} (which is {@code @State}-persisted,
 * so it survives IDE restarts) and an {@code APPEARANCE_SETTINGS_TOPIC} event is published so the
 * Compose-based conversation view recomposes with the new sizes.
 */
public final class ChatFontSizeService {

    public static final int MIN_FONT_SIZE = 8;
    public static final int MAX_FONT_SIZE = 24;

    /** Defaults must mirror the fallbacks used by the Compose ConversationViewModel. */
    private static final int DEFAULT_TEXT_FONT_SIZE = 13;
    private static final int DEFAULT_CODE_FONT_SIZE = 12;

    private ChatFontSizeService() {
    }

    /** Clamp a font size to the supported range. */
    public static int clamp(int size) {
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
    }

    /** Apply a delta to a font size, keeping the result within the supported range. */
    public static int nextSize(int currentSize, int delta) {
        return clamp(currentSize + delta);
    }

    /** Increase the chat font size by one step. */
    public static void increase() {
        changeBy(+1);
    }

    /** Decrease the chat font size by one step. */
    public static void decrease() {
        changeBy(-1);
    }

    /**
     * Change both the prose and code chat font sizes by {@code delta}, persist the result, and
     * notify open conversation panels to refresh.
     */
    public static void changeBy(int delta) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        int newTextSize = nextSize(effectiveTextSize(state), delta);
        int newCodeSize = nextSize(effectiveCodeSize(state), delta);

        // Enable the custom sizes so the values are actually applied by the view model.
        state.setUseCustomFontSize(true);
        state.setUseCustomCodeFontSize(true);
        state.setCustomFontSize(newTextSize);
        state.setCustomCodeFontSize(newCodeSize);

        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(APPEARANCE_SETTINGS_TOPIC)
                .appearanceSettingsChanged();
    }

    private static int effectiveTextSize(DevoxxGenieStateService state) {
        return Boolean.TRUE.equals(state.getUseCustomFontSize())
                ? state.getCustomFontSize() : DEFAULT_TEXT_FONT_SIZE;
    }

    private static int effectiveCodeSize(DevoxxGenieStateService state) {
        return Boolean.TRUE.equals(state.getUseCustomCodeFontSize())
                ? state.getCustomCodeFontSize() : DEFAULT_CODE_FONT_SIZE;
    }
}
