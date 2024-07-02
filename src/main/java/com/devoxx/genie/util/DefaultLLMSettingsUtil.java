package com.devoxx.genie.util;

import com.devoxx.genie.model.enumarations.ModelProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;
import static dev.langchain4j.model.anthropic.AnthropicChatModelName.CLAUDE_INSTANT_1_2;
import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;

public class DefaultLLMSettingsUtil {
    public static final Map<CostKey, Double> DEFAULT_INPUT_COSTS = new HashMap<>();
    public static final Map<CostKey, Double> DEFAULT_OUTPUT_COSTS = new HashMap<>();

    static {
        // OpenAI models
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.OpenAI, "gpt-4"), 0.03);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.OpenAI, "gpt-4"), 0.06);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.OpenAI, "gpt-3.5-turbo"), 0.0015);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.OpenAI, "gpt-3.5-turbo"), 0.002);

        // Anthropic models
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, "claude-3-5-sonnet-20240620"), 3.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, "claude-3-5-sonnet-20240620"), 15.0);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_3_OPUS_20240229.toString()), 15.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_3_OPUS_20240229.toString()), 75.0);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_3_SONNET_20240229.toString()), 3.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_3_SONNET_20240229.toString()), 15.0);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_3_HAIKU_20240307.toString()), 0.25);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_3_HAIKU_20240307.toString()), 1.25);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_2_1.toString()), 8.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_2_1.toString()), 24.0);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_2.toString()), 8.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_2.toString()), 24.0);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_INSTANT_1_2.toString()), 0.8);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Anthropic, CLAUDE_INSTANT_1_2.toString()), 2.4);

        // Gemini
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Gemini, "gemini-pro"), 0.5);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Gemini, "gemini-pro"), 1.5);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Gemini, "gemini-1.5-pro-latest"), 7.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Gemini, "gemini-1.5-pro-latest"), 21.0);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Gemini, "gemini-1.5-flash-latest"), 0.7);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Gemini, "gemini-1.5-flash-latest"), 2.1);

        // DeepInfra
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "meta-llama/Meta-Llama-3-70B-Instruct"), 0.56);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "meta-llama/Meta-Llama-3-70B-Instruct"), 0.77);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "meta-llama/Meta-Llama-3-8B-Instruct"), 0.064);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "meta-llama/Meta-Llama-3-8B-Instruct"), 0.064);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "mistralai/Mixtral-8x7B-Instruct-v0.1"), 0.24);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "mistralai/Mixtral-8x7B-Instruct-v0.1"), 0.24);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "mistralai/Mixtral-8x22B-Instruct-v0.1"), 0.65);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "mistralai/Mixtral-8x22B-Instruct-v0.1"), 0.65);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "mistralai/Mistral-7B-Instruct-v0.3"), 0.07);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "mistralai/Mistral-7B-Instruct-v0.3"), 0.07);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "microsoft/WizardLM-2-8x22B"), 0.65);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "microsoft/WizardLM-2-8x22B"), 0.65);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "microsoft/WizardLM-2-7B"), 0.07);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "microsoft/WizardLM-2-7B"), 0.07);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "openchat/openchat_3.5"), 0.07);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "openchat/openchat_3.5"), 0.07);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "google/gemma-1.1-7b-it"), 0.07);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "google/gemma-1.1-7b-it"), 0.07);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "Phind/Phind-CodeLlama-34B-v2"), 0.6);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "Phind/Phind-CodeLlama-34B-v2"), 0.6);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "cognitivecomputations/dolphin-2.6-mixtral-8x7b"), 0.24);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.DeepInfra, "cognitivecomputations/dolphin-2.6-mixtral-8x7b"), 0.24);

        // Mistral
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Mistral, OPEN_MISTRAL_7B.toString()), 0.25);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Mistral, OPEN_MISTRAL_7B.toString()), 0.25);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Mistral, OPEN_MIXTRAL_8x7B.toString()), 0.7);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Mistral, OPEN_MIXTRAL_8x7B.toString()), 0.7);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Mistral, MISTRAL_SMALL_LATEST.toString()), 1.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Mistral, MISTRAL_SMALL_LATEST.toString()), 3.0);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Mistral, MISTRAL_MEDIUM_LATEST.toString()), 2.7);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Mistral, MISTRAL_MEDIUM_LATEST.toString()), 8.1);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Mistral, MISTRAL_LARGE_LATEST.toString()), 4.0);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Mistral, MISTRAL_LARGE_LATEST.toString()), 12.0);

        // Groq
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Groq, "gemma-7b-it"), 0.07);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Groq, "gemma-7b-it"), 0.07);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Groq, "llama3-8b-8192"), 0.05);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Groq, "llama3-8b-8192"), 0.08);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Groq, "llama3-70b-8192"), 0.59);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Groq, "llama3-70b-8192"), 0.79);
        DEFAULT_INPUT_COSTS.put(new CostKey(ModelProvider.Groq, "mixtral-8x7b-32768"), 0.24);
        DEFAULT_OUTPUT_COSTS.put(new CostKey(ModelProvider.Groq, "mixtral-8x7b-32768"), 0.24);
    }

    public static boolean isApiBasedProvider(ModelProvider provider) {
        return provider == ModelProvider.OpenAI ||
            provider == ModelProvider.Anthropic ||
            provider == ModelProvider.Mistral ||
            provider == ModelProvider.Groq ||
            provider == ModelProvider.DeepInfra ||
            provider == ModelProvider.Gemini;
    }

    public static class CostKey {
        public final ModelProvider provider;
        public final String modelName;

        public CostKey(ModelProvider provider, String modelName) {
            this.provider = provider;
            this.modelName = modelName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CostKey costKey = (CostKey) o;
            return provider == costKey.provider && modelName.equals(costKey.modelName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, modelName);
        }
    }
}
