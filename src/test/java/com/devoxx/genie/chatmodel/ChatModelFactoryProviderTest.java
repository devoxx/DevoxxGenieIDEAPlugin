package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.cloud.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.azureopenai.AzureOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.bedrock.BedrockModelFactory;
import com.devoxx.genie.chatmodel.cloud.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.deepseek.DeepSeekChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.glm.GLMChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.google.GoogleChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.grok.GrokChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.kimi.KimiChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openai.OpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.local.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.local.jan.JanChatModelFactory;
import com.devoxx.genie.chatmodel.local.acprunners.AcpRunnersChatModelFactory;
import com.devoxx.genie.chatmodel.local.clirunners.CliRunnersChatModelFactory;
import com.devoxx.genie.chatmodel.local.llamacpp.LlamaChatModelFactory;
import com.devoxx.genie.chatmodel.local.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.local.ollama.OllamaChatModelFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatModelFactoryProviderTest {

    static Stream<Arguments> providerToFactoryClass() {
        return Stream.of(
                Arguments.of("Anthropic", AnthropicChatModelFactory.class),
                Arguments.of("AzureOpenAI", AzureOpenAIChatModelFactory.class),
                Arguments.of("Bedrock", BedrockModelFactory.class),
                Arguments.of("CustomOpenAI", CustomOpenAIChatModelFactory.class),
                Arguments.of("DeepInfra", DeepInfraChatModelFactory.class),
                Arguments.of("DeepSeek", DeepSeekChatModelFactory.class),
                Arguments.of("Google", GoogleChatModelFactory.class),
                Arguments.of("Groq", GroqChatModelFactory.class),
                Arguments.of("GPT4All", GPT4AllChatModelFactory.class),
                Arguments.of("Jan", JanChatModelFactory.class),
                Arguments.of("LLaMA", LlamaChatModelFactory.class),
                Arguments.of("LMStudio", LMStudioChatModelFactory.class),
                Arguments.of("Mistral", MistralChatModelFactory.class),
                Arguments.of("Ollama", OllamaChatModelFactory.class),
                Arguments.of("OpenAI", OpenAIChatModelFactory.class),
                Arguments.of("OpenRouter", OpenRouterChatModelFactory.class),
                Arguments.of("Grok", GrokChatModelFactory.class),
                Arguments.of("Kimi", KimiChatModelFactory.class),
                Arguments.of("GLM", GLMChatModelFactory.class),
                Arguments.of("CLI Runners", CliRunnersChatModelFactory.class),
                Arguments.of("ACP Runners", AcpRunnersChatModelFactory.class)
        );
    }

    @ParameterizedTest(name = "Provider ''{0}'' should return {1}")
    @MethodSource("providerToFactoryClass")
    void testGetFactoryByProvider_ReturnsCorrectFactory(String providerName, Class<? extends ChatModelFactory> expectedClass) {
        Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider(providerName);

        assertThat(factory).isPresent();
        assertThat(factory.get()).isInstanceOf(expectedClass);
    }

    @Test
    void testGetFactoryByProvider_UnknownProvider_ReturnsEmpty() {
        Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider("UnknownProvider");

        assertThat(factory).isEmpty();
    }

    @Test
    void testGetFactoryByProvider_EmptyString_ReturnsEmpty() {
        Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider("");

        assertThat(factory).isEmpty();
    }

    @Test
    void testGetFactoryByProvider_CachesFactory() {
        Optional<ChatModelFactory> first = ChatModelFactoryProvider.getFactoryByProvider("OpenAI");
        Optional<ChatModelFactory> second = ChatModelFactoryProvider.getFactoryByProvider("OpenAI");

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get()).isSameAs(second.get());
    }

    @Test
    void testGetFactoryByProvider_CaseSensitive() {
        // The factory uses exact case matching
        Optional<ChatModelFactory> lowercase = ChatModelFactoryProvider.getFactoryByProvider("openai");
        assertThat(lowercase).isEmpty();

        Optional<ChatModelFactory> uppercase = ChatModelFactoryProvider.getFactoryByProvider("OPENAI");
        assertThat(uppercase).isEmpty();
    }

    @Test
    void testGetFactoryByProvider_AllCloudProviders() {
        String[] cloudProviders = {"Anthropic", "AzureOpenAI", "Bedrock", "DeepInfra", "DeepSeek",
                "Google", "Groq", "Mistral", "OpenAI", "OpenRouter", "Grok", "Kimi", "GLM"};

        for (String provider : cloudProviders) {
            Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider(provider);
            assertThat(factory)
                    .as("Factory for cloud provider '%s' should be present", provider)
                    .isPresent();
        }
    }

    @Test
    void testGetFactoryByProvider_AllLocalProviders() {
        String[] localProviders = {"CustomOpenAI", "GPT4All", "Jan", "LLaMA", "LMStudio", "Ollama",
                "CLI Runners", "ACP Runners"};

        for (String provider : localProviders) {
            Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider(provider);
            assertThat(factory)
                    .as("Factory for local provider '%s' should be present", provider)
                    .isPresent();
        }
    }
}
