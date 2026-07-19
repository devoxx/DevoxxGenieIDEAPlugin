package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareChatModelFactory;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LLMProviderServiceCloudflareTest {

    @Test
    void cloudflareResolvesToItsFactory() {
        Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider("Cloudflare");
        assertThat(factory).isPresent();
        assertThat(factory.get()).isInstanceOf(CloudflareChatModelFactory.class);
    }

    @Test
    void cloudflareRequiresApiKey() {
        assertThat(LLMProviderService.requiresApiKey(ModelProvider.Cloudflare)).isTrue();
    }
}
