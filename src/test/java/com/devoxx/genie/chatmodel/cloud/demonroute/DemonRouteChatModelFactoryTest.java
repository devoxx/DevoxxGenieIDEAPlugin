package com.devoxx.genie.chatmodel.cloud.demonroute;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.credentials.CredentialKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the DemonRoute provider registration and {@link DemonRouteChatModelFactory#toLanguageModel(String)}
 * — the mapping applied to each model id returned by DemonRoute's OpenAI-compatible
 * {@code /v1/models} endpoint, which carries no context length or pricing.
 */
class DemonRouteChatModelFactoryTest {

    @Test
    void demonRouteProviderIsRegisteredAsCloud() {
        ModelProvider provider = ModelProvider.fromString("DemonRoute");
        assertThat(provider).isEqualTo(ModelProvider.DemonRoute);
        assertThat(provider.getType()).isEqualTo(ModelProvider.Type.CLOUD);
        assertThat(provider.getName()).isEqualTo("DemonRoute");
    }

    @Test
    void demonRouteCredentialKeyMatchesLegacyFieldName() {
        assertThat(CredentialKey.DEMONROUTE_KEY.getSubKey()).isEqualTo("demonRouteKey");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "shoukewei/Qwen3.8-27B-OBLITERATED22-AGENT",
            "some/model-id",
            "another/model-id-2"
    })
    void mapsListedModelIdsToDemonRouteLanguageModels(String modelId) {
        LanguageModel model = DemonRouteChatModelFactory.toLanguageModel(modelId);
        assertThat(model.getProvider()).isEqualTo(ModelProvider.DemonRoute);
        assertThat(model.getModelName()).isEqualTo(modelId);
        assertThat(model.getDisplayName()).isEqualTo(modelId);
        assertThat(model.isApiKeyUsed()).isTrue();
    }

    /** Every dynamically-listed model must get a positive window — never the zero-context bug. */
    @Test
    void listedModelsGetPositiveContextWindow() {
        LanguageModel model = DemonRouteChatModelFactory.toLanguageModel("some/model-id");
        assertThat(model.getInputMaxTokens()).isPositive();
    }
}
