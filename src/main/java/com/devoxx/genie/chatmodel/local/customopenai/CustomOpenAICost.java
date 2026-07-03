package com.devoxx.genie.chatmodel.local.customopenai;

/**
 * Resolves the input/output cost (dollars per 1,000,000 tokens) to use for the Custom OpenAI-compatible
 * provider.
 *
 * <p>Custom / internal OpenAI-compatible servers do not report pricing, so DevoxxGenie previously assumed
 * a cost of {@code 0}, which hid the cost figure in the conversation bubbles. Users can now configure the
 * per-1M-token input and output cost in Settings; when nothing (or a negative value) is configured we fall
 * back to {@code 0} (free / cost hidden), matching the historical behaviour.</p>
 *
 * <p>The unit convention (dollars per 1M tokens) mirrors {@code LanguageModel.inputCost/outputCost} as used
 * in {@code ChatMessageContext.setTokenUsageAndCost}, which divides {@code tokens * cost} by 1,000,000.</p>
 */
public final class CustomOpenAICost {

    private CustomOpenAICost() {
    }

    /**
     * @param configured the user-configured cost per 1M tokens, or {@code null} when unset
     * @return the configured value when it is a non-negative number, otherwise {@code 0.0}
     */
    public static double resolve(Double configured) {
        return (configured != null && configured > 0) ? configured : 0.0;
    }
}
