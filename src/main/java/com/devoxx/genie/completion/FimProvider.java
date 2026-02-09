package com.devoxx.genie.completion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstraction for Fill-in-the-Middle (FIM) completion providers.
 * Allows swapping the backing LLM (Ollama, LMStudio, etc.) without
 * changing the inline-completion pipeline.
 */
public interface FimProvider {

    /**
     * Generate a FIM completion. Blocking — call from a background thread.
     *
     * @param request the FIM request containing prefix, suffix, model info
     * @return FIM response with completion text, or null if cancelled/failed
     */
    @Nullable FimResponse generate(@NotNull FimRequest request);

    /**
     * Cancel any currently active FIM request.
     */
    void cancelActiveCall();
}
