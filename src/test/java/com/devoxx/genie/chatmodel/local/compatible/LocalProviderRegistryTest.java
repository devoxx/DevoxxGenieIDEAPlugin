package com.devoxx.genie.chatmodel.local.compatible;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.controller.ProjectContextController;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.agent.SubAgentRunner;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.ProviderEnabledResolver;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard for the "one enum line plus one definition" design goal: every registered local provider must be
 * reachable from every shared-code site without any further production change.
 */
class LocalProviderRegistryTest {

    @Test
    void registry_hasExpectedProviders() {
        assertThat(LocalProviderRegistry.definitions()).extracting(LocalProviderDefinition::provider)
                .containsExactly(ModelProvider.Jan, ModelProvider.LLaMA, ModelProvider.GPT4All, ModelProvider.Nativ,
                        ModelProvider.RapidMLX);
    }

    @Test
    void enabledByDefault_matchesWhatEachProviderShippedWith() {
        // Jan, llama.cpp and GPT4All shipped enabled before they moved here; Nativ shares llama.cpp's port and
        // Rapid-MLX is opt-in.
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        assertThat(state.isLocalProviderEnabled(ModelProvider.Jan)).isTrue();
        assertThat(state.isLocalProviderEnabled(ModelProvider.LLaMA)).isTrue();
        assertThat(state.isLocalProviderEnabled(ModelProvider.GPT4All)).isTrue();
        assertThat(state.isLocalProviderEnabled(ModelProvider.Nativ)).isFalse();
        assertThat(state.isLocalProviderEnabled(ModelProvider.RapidMLX)).isFalse();
    }

    @Test
    void everyDefinition_isFullyWired() throws Exception {
        Set<String> enabledByDefaultPorts = new HashSet<>();
        DevoxxGenieStateService state = new DevoxxGenieStateService();

        Method supported = ProjectContextController.class.getDeclaredMethod("isSupportedProvider", ModelProvider.class);
        supported.setAccessible(true);
        ProjectContextController controller = Mockito.mock(ProjectContextController.class, Mockito.CALLS_REAL_METHODS);

        for (LocalProviderDefinition def : LocalProviderRegistry.definitions()) {
            ModelProvider provider = def.provider();

            assertThat(provider.getType()).as("type of %s", provider).isEqualTo(ModelProvider.Type.LOCAL);
            Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider(provider.name());
            assertThat(factory).as("factory for %s", provider).isPresent();
            assertThat(factory.get()).as("factory for %s", provider).isInstanceOf(OpenAiCompatibleChatModelFactory.class);
            assertThat((Boolean) supported.invoke(controller, provider)).as("supported %s", provider).isTrue();

            assertThat(state.isLocalProviderEnabled(provider)).as("default enabled %s", provider)
                    .isEqualTo(def.enabledByDefault());
            assertThat(ProviderEnabledResolver.isEnabled(state, provider, false)).as("resolver default %s", provider)
                    .isEqualTo(def.enabledByDefault());
            state.setLocalProviderEnabled(provider, false);
            assertThat(ProviderEnabledResolver.isEnabled(state, provider, false)).as("resolver off %s", provider).isFalse();
            state.setLocalProviderEnabled(provider, true);
            assertThat(ProviderEnabledResolver.isEnabled(state, provider, false)).as("resolver on %s", provider).isTrue();

            assertThat(def.defaultUrl()).as("default URL %s", provider).startsWith("http://localhost:");
            // Two providers enabled out of the box on one port would list each other's models. Sharing a port
            // is fine when one of them is off by default (Nativ and llama.cpp both default to 8080).
            if (def.enabledByDefault()) {
                URI uri = URI.create(def.defaultUrl());
                assertThat(enabledByDefaultPorts.add(uri.getHost() + ":" + uri.getPort()))
                        .as("unique default port among enabled-by-default providers: %s", provider).isTrue();
            }

            String customUrl = "http://localhost:9" + provider.ordinal() + "/v1/";
            state.setLocalProviderUrl(provider, customUrl);
            assertThat(chatModelBaseUrl(state, provider)).as("chat base URL %s", provider).isEqualTo(customUrl);
            assertThat(subAgentBaseUrl(state, provider.getName())).as("sub-agent base URL %s", provider).isEqualTo(customUrl);
        }
    }

    @Test
    void subAgent_nameThatIsNotADisplayName_stillUsesTheExistingCases() throws Exception {
        // "LLaMA" is a constant name, not a display name, so ModelProvider.fromString rejects it.
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        state.setLocalProviderUrl(ModelProvider.LLaMA, "http://llama-box:8080");
        assertThat(subAgentBaseUrl(state, "LLaMA")).isEqualTo("http://llama-box:8080");
        assertThat(subAgentBaseUrl(state, "no-such-provider")).isNull();
    }

    private static String chatModelBaseUrl(DevoxxGenieStateService state, ModelProvider provider) {
        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(LanguageModel.builder().provider(provider).modelName("m").build())
                .build();
        try (MockedStatic<DevoxxGenieStateService> statics = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            statics.when(DevoxxGenieStateService::getInstance).thenReturn(state);
            return new ChatModelProvider().initChatModel(context).getBaseUrl();
        }
    }

    private static String subAgentBaseUrl(DevoxxGenieStateService state, String providerName) throws Exception {
        SubAgentRunner runner = Mockito.mock(SubAgentRunner.class, Mockito.CALLS_REAL_METHODS);
        Field name = SubAgentRunner.class.getDeclaredField("resolvedProviderName");
        name.setAccessible(true);
        name.set(runner, providerName);
        Method setBaseUrl = SubAgentRunner.class.getDeclaredMethod(
                "setBaseUrlIfLocal", CustomChatModel.class, DevoxxGenieStateService.class);
        setBaseUrl.setAccessible(true);

        CustomChatModel config = new CustomChatModel();
        setBaseUrl.invoke(runner, config, state);
        return config.getBaseUrl();
    }
}
