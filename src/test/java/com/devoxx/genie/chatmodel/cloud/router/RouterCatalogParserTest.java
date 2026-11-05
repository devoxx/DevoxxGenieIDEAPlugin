package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.analytics.ProviderType;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouterCatalogParserTest {

    private static final RouterDefinition REQUESTY = new RouterDefinition(
            ModelProvider.OpenRouter, "Requesty", "https://router.requesty.ai/v1",
            List.of("/models/managed", "/models"), "requestyKey", "https://example.invalid/keys",
            null,
            new CatalogFields(null, List.of("context_window"), "input_price", "output_price",
                    1_000_000, "api", "chat"),
            4000);

    private static final RouterDefinition PLAIN = new RouterDefinition(
            ModelProvider.OpenRouter, "Plain", "https://example.invalid/v1",
            List.of("/models"), "plainKey", "https://example.invalid/keys", null, null, null);

    private static String resource(String name) throws IOException {
        try (var in = RouterCatalogParserTest.class.getResourceAsStream("/router/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void openRouter_mapsNestedFields() throws IOException {
        RouterDefinition def = RouterRegistry.find(ModelProvider.OpenRouter).orElseThrow();

        List<LanguageModel> models = RouterCatalogParser.parse(def, resource("openrouter-models.json"));

        assertThat(models).hasSize(2);
        LanguageModel sonnet = models.get(0);
        assertThat(sonnet.getModelName()).isEqualTo("anthropic/claude-3.5-sonnet");
        assertThat(sonnet.getDisplayName()).isEqualTo("Anthropic: Claude 3.5 Sonnet");
        assertThat(sonnet.getInputCost()).isEqualTo(3.0);
        assertThat(sonnet.getOutputCost()).isEqualTo(15.0);
        assertThat(sonnet.getInputMaxTokens()).isEqualTo(200_000);
        assertThat(sonnet.getProvider()).isEqualTo(ModelProvider.OpenRouter);

        LanguageModel mini = models.get(1);
        assertThat(mini.getInputCost()).isEqualTo(1.5);
        assertThat(mini.getOutputCost()).isEqualTo(0.6);
        assertThat(mini.getInputMaxTokens()).isEqualTo(128_000);
    }

    @Test
    void requesty_keepsChatEntriesOnly() throws IOException {
        List<LanguageModel> models = RouterCatalogParser.parse(REQUESTY, resource("requesty-models.json"));

        assertThat(models).extracting(LanguageModel::getModelName)
                .containsExactly("coding/claude-4-sonnet", "openai/gpt-5-mini");
        assertThat(models.get(0).getInputMaxTokens()).isEqualTo(200_000);
        assertThat(models.get(0).getInputCost()).isEqualTo(3.0);
        assertThat(models.get(1).getOutputCost()).isEqualTo(2.0);
    }

    @Test
    void noMapping_usesDefaults() {
        List<LanguageModel> models = RouterCatalogParser.parse(PLAIN, "{\"data\":[{\"id\":\"some-model\"}]}");

        assertThat(models).hasSize(1);
        LanguageModel m = models.get(0);
        assertThat(m.getDisplayName()).isEqualTo("some-model");
        assertThat(m.getInputMaxTokens()).isEqualTo(RouterCatalogParser.DEFAULT_CONTEXT_WINDOW).isEqualTo(128000);
        assertThat(m.getInputCost()).isZero();
        assertThat(m.getOutputCost()).isZero();
        assertThat(m.isApiKeyUsed()).isTrue();
    }

    @Test
    void skipsBadEntries_keepsIdsVerbatim() {
        String json = """
                {"data":[null, {"id":"  "}, {"name":"no id"}, {"id":"vendor/model"}, {"id":"@cf/x"},
                         {"id":"bad-price","input_price":"abc","output_price":{"x":1}}]}
                """;

        List<LanguageModel> models = RouterCatalogParser.parse(REQUESTY, json);

        assertThat(models).extracting(LanguageModel::getModelName)
                .containsExactly("vendor/model", "@cf/x", "bad-price");
        LanguageModel bad = models.get(2);
        assertThat(bad.getInputCost()).isZero();
        assertThat(bad.getOutputCost()).isZero();
    }

    @Test
    void invalidJson_throws() {
        assertThatThrownBy(() -> RouterCatalogParser.parse(PLAIN, "<html>nope</html>"))
                .isInstanceOf(JsonParseException.class);
        assertThatThrownBy(() -> RouterCatalogParser.parse(PLAIN, "{}"))
                .isInstanceOf(JsonParseException.class);
        assertThatThrownBy(() -> RouterCatalogParser.parse(PLAIN, "{\"data\":{}}"))
                .isInstanceOf(JsonParseException.class);
    }

    @Test
    void providerType_routerMapsToCloud() {
        assertThat(ModelProvider.OpenRouter.getType()).isEqualTo(ModelProvider.Type.ROUTER);
        assertThat(ModelProvider.Cloudflare.getType()).isEqualTo(ModelProvider.Type.ROUTER);
        assertThat(ProviderType.fromModelProvider(ModelProvider.OpenRouter)).isEqualTo(ProviderType.CLOUD);
        assertThat(ProviderType.fromModelProvider(ModelProvider.Cloudflare)).isEqualTo(ProviderType.CLOUD);
    }

    @Test
    void registry_hasOpenRouterOnly() {
        assertThat(RouterRegistry.isGeneric(ModelProvider.OpenRouter)).isTrue();
        assertThat(RouterRegistry.isGeneric(ModelProvider.Cloudflare)).isFalse();
        RouterDefinition def = RouterRegistry.find(ModelProvider.OpenRouter).orElseThrow();
        assertThat(def.baseUrl()).isEqualTo("https://openrouter.ai/api/v1/");
        assertThat(def.modelEndpoints()).containsExactly("/models");
        assertThat(def.credentialSubKey()).isEqualTo("openRouterKey");
        assertThat(def.maxTokens()).isEqualTo(4000);
    }
}
