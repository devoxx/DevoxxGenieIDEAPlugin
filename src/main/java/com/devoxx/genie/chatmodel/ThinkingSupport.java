package com.devoxx.genie.chatmodel;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

/**
 * Resolves the global "Show Thinking" setting for chat model factories.
 *
 * <p>Factories pass the flag to their builder's {@code returnThinking(...)}; langchain4j then
 * surfaces any reasoning the provider emits (e.g. the OpenAI-compatible
 * {@code reasoning_content} field) as {@link dev.langchain4j.data.message.AiMessage#thinking()}.
 * The flag is client-side only: enabling it for providers/models that emit no reasoning is a
 * no-op, so it is safe to apply unconditionally to every OpenAI-compatible builder.
 */
public final class ThinkingSupport {

    private ThinkingSupport() {
    }

    public static boolean isEnabled() {
        return Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowThinkingEnabled());
    }
}
