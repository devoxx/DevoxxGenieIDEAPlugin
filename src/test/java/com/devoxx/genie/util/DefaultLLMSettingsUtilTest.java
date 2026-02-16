package com.devoxx.genie.util;

import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultLLMSettingsUtilTest {

    // --- API-key-based providers (should return true) ---

    @Test
    void isApiKeyBasedProvider_openAI_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.OpenAI)).isTrue();
    }

    @Test
    void isApiKeyBasedProvider_anthropic_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Anthropic)).isTrue();
    }

    @Test
    void isApiKeyBasedProvider_mistral_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Mistral)).isTrue();
    }

    @Test
    void isApiKeyBasedProvider_groq_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Groq)).isTrue();
    }

    @Test
    void isApiKeyBasedProvider_deepInfra_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.DeepInfra)).isTrue();
    }

    @Test
    void isApiKeyBasedProvider_google_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Google)).isTrue();
    }

    @Test
    void isApiKeyBasedProvider_openRouter_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.OpenRouter)).isTrue();
    }

    @Test
    void isApiKeyBasedProvider_azureOpenAI_returnsTrue() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.AzureOpenAI)).isTrue();
    }

    // --- Non-API-key-based providers (should return false) ---

    @Test
    void isApiKeyBasedProvider_ollama_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Ollama)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_gpt4All_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.GPT4All)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_lmStudio_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.LMStudio)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_llama_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.LLaMA)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_jan_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Jan)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_customOpenAI_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.CustomOpenAI)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_deepSeek_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.DeepSeek)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_bedrock_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Bedrock)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_grok_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Grok)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_kimi_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Kimi)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_glm_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.GLM)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_cliRunners_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.CLIRunners)).isFalse();
    }

    @Test
    void isApiKeyBasedProvider_acpRunners_returnsFalse() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.ACPRunners)).isFalse();
    }

    // --- Exhaustive test: verify every enum value is handled ---

    @ParameterizedTest
    @EnumSource(ModelProvider.class)
    void isApiKeyBasedProvider_allProviders_doesNotThrow(ModelProvider provider) {
        // Should not throw for any provider
        boolean result = DefaultLLMSettingsUtil.isApiKeyBasedProvider(provider);
        assertThat(result).isNotNull();
    }

    // --- CostKey record tests ---

    @Test
    void costKey_equality_sameValues_returnsTrue() {
        DefaultLLMSettingsUtil.CostKey key1 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        DefaultLLMSettingsUtil.CostKey key2 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void costKey_equality_differentProvider_returnsFalse() {
        DefaultLLMSettingsUtil.CostKey key1 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        DefaultLLMSettingsUtil.CostKey key2 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.Anthropic, "gpt-4");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void costKey_equality_differentModel_returnsFalse() {
        DefaultLLMSettingsUtil.CostKey key1 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        DefaultLLMSettingsUtil.CostKey key2 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-3.5");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void costKey_equality_withNull_returnsFalse() {
        DefaultLLMSettingsUtil.CostKey key = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        assertThat(key).isNotEqualTo(null);
    }

    @Test
    void costKey_equality_withDifferentType_returnsFalse() {
        DefaultLLMSettingsUtil.CostKey key = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        assertThat(key).isNotEqualTo("not a CostKey");
    }

    @Test
    void costKey_toString_containsProviderAndModel() {
        DefaultLLMSettingsUtil.CostKey key = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        String result = key.toString();
        assertThat(result).contains("OpenAI");
        assertThat(result).contains("gpt-4");
    }

    @Test
    void costKey_hashCode_sameValues_sameHash() {
        DefaultLLMSettingsUtil.CostKey key1 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.Anthropic, "claude-3");
        DefaultLLMSettingsUtil.CostKey key2 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.Anthropic, "claude-3");
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    void costKey_nullModelName_handledGracefully() {
        DefaultLLMSettingsUtil.CostKey key1 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, null);
        DefaultLLMSettingsUtil.CostKey key2 = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, null);
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void costKey_selfEquality_returnsTrue() {
        DefaultLLMSettingsUtil.CostKey key = new DefaultLLMSettingsUtil.CostKey(ModelProvider.OpenAI, "gpt-4");
        assertThat(key).isEqualTo(key);
    }
}
