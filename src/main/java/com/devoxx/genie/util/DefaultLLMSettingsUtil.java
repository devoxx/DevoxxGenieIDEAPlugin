package com.devoxx.genie.util;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMModelRegistryService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultLLMSettingsUtil {

    public static final Map<CostKey, Double> DEFAULT_INPUT_COSTS = new HashMap<>();
    public static final Map<CostKey, Double> DEFAULT_OUTPUT_COSTS = new HashMap<>();

    public static void initializeDefaultCosts() {
        LLMModelRegistryService modelRegistry = LLMModelRegistryService.getInstance();
        for (LanguageModel model : modelRegistry.getModels()) {
            if (isApiKeyBasedProvider(model.getProvider())) {
                DEFAULT_INPUT_COSTS.put(new CostKey(model.getProvider(), model.getModelName()), model.getInputCost());
                DEFAULT_OUTPUT_COSTS.put(new CostKey(model.getProvider(), model.getModelName()), model.getOutputCost());
            }
        }
    }

    /**
     * Does the ModelProvider use an API KEY?
     *
     * @param provider the LLM provider
     * @return true when API Key is required, meaning a cost is involved
     */
    public static boolean isApiKeyBasedProvider(ModelProvider provider) {
        return provider == ModelProvider.OpenAI ||
                provider == ModelProvider.Anthropic ||
                provider == ModelProvider.Mistral ||
                provider == ModelProvider.Groq ||
                provider == ModelProvider.DeepInfra ||
                provider == ModelProvider.Google ||
                provider == ModelProvider.AzureOpenAI;
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
            return provider == costKey.provider &&
                Objects.equals(modelName, costKey.modelName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, modelName);
        }

        @Override
        public String toString() {
            return "CostKey{provider=" + provider + ", modelName='" + modelName + "'}";
        }
    }
}
