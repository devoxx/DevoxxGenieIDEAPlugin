package com.devoxx.genie.chatmodel.local.customopenai;

/**
 * Resolves the context window (input max tokens) to use for the Custom OpenAI-compatible provider.
 *
 * <p>Custom / internal OpenAI-compatible servers do not expose their context length via the
 * {@code /models} endpoint, so DevoxxGenie previously assumed a hardcoded {@value #DEFAULT_CONTEXT_WINDOW}
 * token window. That drove the token-usage bar red as soon as a modest amount of project context was
 * added, even when the backing model actually supported a far larger window. Users can now override the
 * value in Settings; when nothing is configured we fall back to the historical default.</p>
 */
public final class CustomOpenAIContextWindow {

    /** Historical default used when the user has not configured a context window. */
    public static final int DEFAULT_CONTEXT_WINDOW = 4096;

    private CustomOpenAIContextWindow() {
    }

    /**
     * @param configured the user-configured context window, or {@code null} when unset
     * @return the configured value when it is a positive number, otherwise {@link #DEFAULT_CONTEXT_WINDOW}
     */
    public static int resolve(Integer configured) {
        return (configured != null && configured > 0) ? configured : DEFAULT_CONTEXT_WINDOW;
    }
}
