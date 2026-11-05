package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.controller.ProjectContextController;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard for the "one enum line plus one definition" design goal: every registered router must be
 * reachable from every shared-code site without any further production change.
 */
class RouterRegistryTest {

    @Test
    void registry_hasExpectedRouters() {
        assertThat(RouterRegistry.definitions()).extracting(RouterDefinition::provider)
                .containsExactly(ModelProvider.OpenRouter, ModelProvider.Requesty, ModelProvider.DemonRoute);
    }

    @Test
    void everyDefinition_isFullyWired() throws Exception {
        Set<ModelProvider> providers = new HashSet<>();
        Set<String> subKeys = new HashSet<>();
        Set<String> baseUrls = new HashSet<>();
        DevoxxGenieStateService state = new DevoxxGenieStateService();

        Method supported = ProjectContextController.class.getDeclaredMethod("isSupportedProvider", ModelProvider.class);
        supported.setAccessible(true);
        ProjectContextController controller = Mockito.mock(ProjectContextController.class, Mockito.CALLS_REAL_METHODS);

        for (RouterDefinition def : RouterRegistry.definitions()) {
            ModelProvider provider = def.provider();

            assertThat(provider.getType()).as("type of %s", provider).isEqualTo(ModelProvider.Type.ROUTER);
            Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider(provider.name());
            assertThat(factory).as("factory for %s", provider).isPresent();
            assertThat(factory.get()).isInstanceOf(RouterChatModelFactory.class);

            assertThat(LLMProviderService.requiresApiKey(provider)).as("requiresApiKey %s", provider).isTrue();
            assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(provider)).as("apiKeyBased %s", provider).isTrue();
            assertThat((Boolean) supported.invoke(controller, provider)).as("supported %s", provider).isTrue();
            assertThat(state.isRouterEnabled(provider)).as("disabled by default %s", provider).isFalse();

            assertThat(providers.add(provider)).as("unique provider %s", provider).isTrue();
            assertThat(subKeys.add(def.credentialSubKey())).as("unique sub-key %s", provider).isTrue();
            assertThat(def.baseUrl()).startsWith("https://");
            assertThat(baseUrls.add(def.baseUrl())).as("unique base URL %s", provider).isTrue();
            assertThat(def.keyUrl()).startsWith("https://");
        }
    }

    @Test
    void requesty_sampleCatalogParsesWithRegisteredDefinition() throws IOException {
        RouterDefinition def = RouterRegistry.find(ModelProvider.Requesty).orElseThrow();
        String json;
        try (var in = RouterRegistryTest.class.getResourceAsStream("/router/requesty-models.json")) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<LanguageModel> models = RouterCatalogParser.parse(def, json);

        assertThat(models).extracting(LanguageModel::getModelName)
                .containsExactly("coding/claude-4-sonnet", "openai/gpt-5-mini");
        assertThat(models.get(0).getProvider()).isEqualTo(ModelProvider.Requesty);
        assertThat(models.get(0).getInputCost()).isEqualTo(3.0);
        assertThat(models.get(0).getInputMaxTokens()).isEqualTo(200_000);
    }

    @Test
    void demonRoute_usesPlainCatalogAndSettingsMaxTokens() {
        RouterDefinition def = RouterRegistry.find(ModelProvider.DemonRoute).orElseThrow();

        assertThat(def.baseUrl()).isEqualTo("https://api.demonroute.com/v1");
        assertThat(def.modelEndpoints()).containsExactly("/models");
        assertThat(def.catalogFields()).isNull();
        assertThat(def.maxTokens()).isNull();
    }
}
