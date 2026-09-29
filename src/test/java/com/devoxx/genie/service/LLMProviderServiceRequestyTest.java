package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.cloud.requesty.RequestyChatModelFactory;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LLMProviderServiceRequestyTest {

    @Test
    void requestyResolvesToItsFactory() {
        Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider("Requesty");
        assertThat(factory).isPresent();
        assertThat(factory.get()).isInstanceOf(RequestyChatModelFactory.class);
    }

    @Test
    void requestyRequiresApiKey() {
        assertThat(LLMProviderService.requiresApiKey(ModelProvider.Requesty)).isTrue();
    }
}
