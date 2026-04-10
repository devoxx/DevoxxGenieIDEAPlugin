package com.devoxx.genie.service.models;

import com.devoxx.genie.model.models.ModelConfig;
import com.devoxx.genie.model.models.ModelConfigEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that docusaurus/static/api/models.json is well-formed and
 * can be deserialized by the same Gson path used in production.
 */
class ModelConfigJsonTest {

    private static final Path MODELS_JSON_PATH =
            Path.of("docusaurus/static/api/models.json");

    private static final Set<String> EXPECTED_PROVIDERS = Set.of(
            "Anthropic", "OpenAI", "Google", "Mistral", "Grok",
            "Groq", "DeepInfra", "DeepSeek", "Bedrock", "GLM", "Kimi"
    );

    private ModelConfig config;

    @BeforeEach
    void setUp() throws IOException {
        assertThat(MODELS_JSON_PATH).exists();
        String json = Files.readString(MODELS_JSON_PATH);
        Gson gson = new GsonBuilder().create();
        config = gson.fromJson(json, ModelConfig.class);
    }

    @Test
    void shouldParseSchemaVersion() {
        assertThat(config.getSchemaVersion()).isEqualTo(1);
    }

    @Test
    void shouldHaveLastUpdatedDate() {
        assertThat(config.getLastUpdated()).isNotBlank();
        // Basic ISO date format check (YYYY-MM-DD)
        assertThat(config.getLastUpdated()).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void shouldContainAllExpectedProviders() {
        assertThat(config.getProviders()).isNotNull();
        assertThat(config.getProviders().keySet()).containsAll(EXPECTED_PROVIDERS);
    }

    @Test
    void shouldHaveNonEmptyModelListsForAllProviders() {
        for (Map.Entry<String, List<ModelConfigEntry>> entry : config.getProviders().entrySet()) {
            assertThat(entry.getValue())
                    .as("Provider '%s' should have at least one model", entry.getKey())
                    .isNotEmpty();
        }
    }

    @Test
    void shouldHaveValidModelEntriesForAllProviders() {
        for (Map.Entry<String, List<ModelConfigEntry>> entry : config.getProviders().entrySet()) {
            String provider = entry.getKey();
            for (ModelConfigEntry model : entry.getValue()) {
                assertThat(model.getModelName())
                        .as("modelName in provider '%s'", provider)
                        .isNotBlank();

                assertThat(model.getDisplayName())
                        .as("displayName for '%s' in provider '%s'", model.getModelName(), provider)
                        .isNotBlank();

                assertThat(model.getInputMaxTokens())
                        .as("inputMaxTokens for '%s' in provider '%s'", model.getModelName(), provider)
                        .isGreaterThan(0);

                assertThat(model.getInputCost())
                        .as("inputCost for '%s' in provider '%s'", model.getModelName(), provider)
                        .isGreaterThanOrEqualTo(0);

                assertThat(model.getOutputCost())
                        .as("outputCost for '%s' in provider '%s'", model.getModelName(), provider)
                        .isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    void shouldHaveNoDuplicateModelNamesPerProvider() {
        for (Map.Entry<String, List<ModelConfigEntry>> entry : config.getProviders().entrySet()) {
            String provider = entry.getKey();
            List<String> modelNames = entry.getValue().stream()
                    .map(ModelConfigEntry::getModelName)
                    .toList();
            assertThat(modelNames)
                    .as("Duplicate model names in provider '%s'", provider)
                    .doesNotHaveDuplicates();
        }
    }

    @Test
    void shouldHaveReasonableTokenLimits() {
        for (Map.Entry<String, List<ModelConfigEntry>> entry : config.getProviders().entrySet()) {
            for (ModelConfigEntry model : entry.getValue()) {
                assertThat(model.getInputMaxTokens())
                        .as("inputMaxTokens for '%s'", model.getModelName())
                        .isLessThanOrEqualTo(10_000_000);

                if (model.getOutputMaxTokens() > 0) {
                    assertThat(model.getOutputMaxTokens())
                            .as("outputMaxTokens for '%s'", model.getModelName())
                            .isLessThanOrEqualTo(1_000_000);
                }
            }
        }
    }
}
