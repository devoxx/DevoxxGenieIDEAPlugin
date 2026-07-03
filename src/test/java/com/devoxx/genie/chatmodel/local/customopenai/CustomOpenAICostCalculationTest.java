package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check that a Custom OpenAI model carrying user-configured input/output costs produces a
 * non-zero cost on the {@link ChatMessageContext} — the value the conversation bubble renders when
 * {@code cost > 0}. Guards the wiring in {@code CustomOpenAIChatModelFactory} /
 * {@code ActionButtonsPanelController} that populates {@code inputCost}/{@code outputCost}.
 */
class CustomOpenAICostCalculationTest {

    @Test
    void configuredCosts_produceNonZeroBubbleCost() {
        // Cost values as they would be built from the settings (dollars per 1M tokens).
        LanguageModel model = LanguageModel.builder()
                .provider(ModelProvider.CustomOpenAI)
                .modelName("internal-model")
                .inputCost(CustomOpenAICost.resolve(3.0))    // $3 / 1M input
                .outputCost(CustomOpenAICost.resolve(15.0))  // $15 / 1M output
                .inputMaxTokens(CustomOpenAIContextWindow.resolve(32_000))
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(model)
                .build();

        context.setTokenUsageAndCost(new TokenUsage(1_000_000, 500_000));

        // 1_000_000 * 3 / 1e6 + 500_000 * 15 / 1e6 = 3 + 7.5 = 10.5
        assertThat(context.getCost()).isEqualTo(10.5);
    }

    @Test
    void unconfiguredCosts_leaveCostZero() {
        LanguageModel model = LanguageModel.builder()
                .provider(ModelProvider.CustomOpenAI)
                .modelName("internal-model")
                .inputCost(CustomOpenAICost.resolve(null))
                .outputCost(CustomOpenAICost.resolve(null))
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(model)
                .build();

        context.setTokenUsageAndCost(new TokenUsage(1_000_000, 500_000));

        assertThat(context.getCost()).isZero();
    }
}
