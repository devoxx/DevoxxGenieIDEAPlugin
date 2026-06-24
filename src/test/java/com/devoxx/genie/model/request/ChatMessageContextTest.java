package com.devoxx.genie.model.request;

import com.devoxx.genie.model.LanguageModel;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ChatMessageContextTest {

    private static LanguageModel model() {
        return LanguageModel.builder()
                .inputCost(3.0)
                .outputCost(15.0)
                .build();
    }

    @Test
    void setTokenUsageAndCost_computesCostWhenCountsPresent() {
        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(model())
                .build();

        context.setTokenUsageAndCost(new TokenUsage(1_000_000, 1_000_000));

        // (1_000_000 * 3 + 1_000_000 * 15) / 1_000_000 = 18.0
        assertThat(context.getCost()).isEqualTo(18.0);
    }

    /**
     * Reproduces issue #1149: some providers (e.g. when running a cloud model in agent
     * mode) return a non-null TokenUsage whose inputTokenCount()/outputTokenCount() are
     * null. Auto-unboxing those nulls during cost calculation threw a NullPointerException.
     */
    @Test
    void setTokenUsageAndCost_doesNotThrowWhenTokenCountsAreNull() {
        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(model())
                .build();

        TokenUsage nullCounts = new TokenUsage(null, null, null);

        assertThatCode(() -> context.setTokenUsageAndCost(nullCounts))
                .doesNotThrowAnyException();

        assertThat(context.getTokenUsage()).isSameAs(nullCounts);
        assertThat(context.getCost()).isZero();
    }

    @Test
    void setTokenUsageAndCost_handlesPartialNullCounts() {
        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(model())
                .build();

        // input present, output null
        assertThatCode(() -> context.setTokenUsageAndCost(new TokenUsage(1_000_000, null)))
                .doesNotThrowAnyException();

        // (1_000_000 * 3 + 0 * 15) / 1_000_000 = 3.0
        assertThat(context.getCost()).isEqualTo(3.0);
    }
}
