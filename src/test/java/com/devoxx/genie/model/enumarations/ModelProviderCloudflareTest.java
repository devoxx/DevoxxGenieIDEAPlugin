package com.devoxx.genie.model.enumarations;

import com.devoxx.genie.service.credentials.CredentialKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderCloudflareTest {

    @Test
    void cloudflareProviderIsRegisteredAsRouter() {
        ModelProvider provider = ModelProvider.fromString("Cloudflare");
        assertThat(provider).isEqualTo(ModelProvider.Cloudflare);
        assertThat(provider.getType()).isEqualTo(ModelProvider.Type.ROUTER);
        assertThat(provider.getName()).isEqualTo("Cloudflare");
    }

    @Test
    void cloudflareCredentialKeyMatchesLegacyFieldName() {
        assertThat(CredentialKey.CLOUDFLARE_KEY.getSubKey()).isEqualTo("cloudflareKey");
    }
}
