package com.devoxx.genie.model.enumarations;

import com.devoxx.genie.service.credentials.CredentialKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderRequestyTest {

    @Test
    void requestyProviderIsRegisteredAsCloud() {
        ModelProvider provider = ModelProvider.fromString("Requesty");
        assertThat(provider).isEqualTo(ModelProvider.Requesty);
        assertThat(provider.getType()).isEqualTo(ModelProvider.Type.CLOUD);
        assertThat(provider.getName()).isEqualTo("Requesty");
    }

    @Test
    void requestyCredentialKeyMatchesLegacyFieldName() {
        assertThat(CredentialKey.REQUESTY_KEY.getSubKey()).isEqualTo("requestyKey");
    }
}
