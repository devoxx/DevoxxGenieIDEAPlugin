package com.devoxx.genie.service.analytics;

import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderTypeTest {

    @Test
    void nullProviderMapsToNone() {
        assertThat(ProviderType.fromModelProvider(null)).isEqualTo(ProviderType.NONE);
    }

    @Test
    void everyLocalProviderMapsToLocal() {
        for (ModelProvider p : ModelProvider.values()) {
            if (p.getType() == ModelProvider.Type.LOCAL) {
                assertThat(ProviderType.fromModelProvider(p))
                        .as("local provider %s", p)
                        .isEqualTo(ProviderType.LOCAL);
            }
        }
    }

    @Test
    void everyCloudProviderMapsToCloud() {
        for (ModelProvider p : ModelProvider.values()) {
            if (p.getType() == ModelProvider.Type.CLOUD) {
                assertThat(ProviderType.fromModelProvider(p))
                        .as("cloud provider %s", p)
                        .isEqualTo(ProviderType.CLOUD);
            }
        }
    }

    @Test
    void optionalProvidersFoldIntoCloud() {
        // Task-209 AC #16 — AzureOpenAI and Bedrock are enterprise cloud endpoints, not a separate bucket.
        assertThat(ProviderType.fromModelProvider(ModelProvider.AzureOpenAI)).isEqualTo(ProviderType.CLOUD);
        assertThat(ProviderType.fromModelProvider(ModelProvider.Bedrock)).isEqualTo(ProviderType.CLOUD);
    }

    @Test
    void wireValuesAreExactStringsInSchema() {
        assertThat(ProviderType.LOCAL.wireValue()).isEqualTo("local");
        assertThat(ProviderType.CLOUD.wireValue()).isEqualTo("cloud");
        assertThat(ProviderType.NONE.wireValue()).isEqualTo("none");
    }

    @Test
    void allModelProviderValuesCoveredWithoutThrowing() {
        // Guardrail: if a new Type enum constant is added, this test forces us to extend the switch.
        for (ModelProvider p : ModelProvider.values()) {
            assertThat(ProviderType.fromModelProvider(p)).isNotNull();
        }
    }
}
