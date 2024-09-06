package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.openrouter.Data;
import com.devoxx.genie.service.openrouter.OpenRouterService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;
import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;

@Service(Service.Level.APP)
@State(
    name = "com.devoxx.genie.LLMModelRegistry",
    storages = @Storage("devoxxGenieLLMModels.xml")
)
public final class LLMModelRegistryService implements PersistentStateComponent<LLMModelRegistryService> {
    private List<LanguageModel> models = new ArrayList<>();

    @NotNull
    public static LLMModelRegistryService getInstance() {
        return ApplicationManager.getApplication().getService(LLMModelRegistryService.class);
    }

    public LLMModelRegistryService() {
        addOpenAiModels();
        addAnthropicModels();
        addDeepInfraModels();
        addGroqModels();
        addGeminiModels();
        addMistralModels();
        addDeepSeekModels();
    }

    private void addAnthropicModels() {

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_INSTANT_1_2.toString())
            .displayName("Claude 1.2")
            .inputCost(0.8)
            .outputCost(2.4)
            .contextWindow(100_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(AnthropicChatModelName.CLAUDE_2.toString())
            .displayName("Claude 2.0")
            .inputCost(8)
            .outputCost(24)
            .contextWindow(100_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(AnthropicChatModelName.CLAUDE_2_1.toString())
            .displayName("Claude 2.1")
            .inputCost(8)
            .outputCost(24)
            .contextWindow(100_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_3_HAIKU_20240307.toString())
            .displayName("Claude 3 Haiku")
            .inputCost(0.25)
            .outputCost(1.25)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_3_HAIKU_20240307.toString())
            .displayName("Claude 3 Sonnet")
            .inputCost(3)
            .outputCost(15)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_3_OPUS_20240229.toString())
            .displayName("Claude 3 Opus")
            .inputCost(15)
            .outputCost(75)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName("claude-3-5-sonnet-20240620")
            .displayName("Claude 3.5 Sonnet")
            .inputCost(3)
            .outputCost(15)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addOpenAiModels() {

        models.add(LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_3_5_TURBO.toString())
            .displayName("GPT 3.5 Turbo")
            .inputCost(0.5)
            .outputCost(1.5)
            .contextWindow(16_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_4.toString())
            .displayName("GPT 4")
            .inputCost(30)
            .outputCost(60)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_4_TURBO_PREVIEW.toString())
            .displayName("GPT 4 Turbo")
            .inputCost(10)
            .outputCost(30)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_4_O.toString())
            .displayName("GPT 4o")
            .inputCost(5)
            .outputCost(15)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName("gpt-4o-mini")
            .displayName("GPT 4o mini")
            .inputCost(0.15)
            .outputCost(0.6)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addDeepInfraModels() {
        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("meta-llama/Meta-Llama-3.1-405B-Instruct")
            .displayName("Meta Llama 3.1 405B")
            .inputCost(2.7)
            .outputCost(2.7)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("meta-llama/Meta-Llama-3.1-70B-Instruct")
            .displayName("Meta Llama 3.1 70B")
            .inputCost(0.35)
            .outputCost(0.4)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("meta-llama/Meta-Llama-3.1-8B-Instruct")
            .displayName("Meta Llama 3.1 8B")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mistral-Nemo-Instruct-2407")
            .displayName("Mistral Nemo 12B")
            .inputCost(0.13)
            .outputCost(0.13)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mixtral-8x7B-Instruct-v0.1")
            .displayName("Mixtral 8x7B Instruct v0.1")
            .inputCost(0.24)
            .outputCost(0.24)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mixtral-8x22B-Instruct-v0.1")
            .displayName("Mixtral 8x22B Instruct v0.1")
            .inputCost(0.65)
            .outputCost(0.65)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mistral-7B-Instruct-v0.3")
            .displayName("Mistral 7B Instruct v0.3")
            .inputCost(0.07)
            .outputCost(0.07)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("microsoft/WizardLM-2-8x22B")
            .displayName("Wizard LM 2 8x22B")
            .inputCost(0.5)
            .outputCost(0.5)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("microsoft/WizardLM-2-7B")
            .displayName("Wizard LM 2 7B")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("openchat/openchat_3.5")
            .displayName("OpenChat 3.5")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("google/gemma-2-9b-it")
            .displayName("Gemma 2 9B it")
            .inputCost(0.06)
            .outputCost(0.06)
            .contextWindow(4_000)
            .apiKeyUsed(true)
            .build());
    }

    /**
     * The Gemini models.
     *
     * @see <a href="https://ai.google.dev/gemini-api/docs/models/gemini">V1 Gemini models</a>
     */
    private void addGeminiModels() {

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.5-flash")
            .displayName("Gemini 1.5 Flash")
            .inputCost(0.0375)
            .outputCost(0.6)
            .contextWindow(1_000_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.5-pro")
            .displayName("Gemini 1.5 Pro")
            .inputCost(7)
            .outputCost(21)
            .contextWindow(2_000_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.5-pro-exp-0801")
            .displayName("Gemini 1.5 Pro 0801")
            .inputCost(7)
            .outputCost(21)
            .contextWindow(2_000_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.0-pro")
            .displayName("Gemini 1.0 Pro")
            .inputCost(0.5)
            .outputCost(1.5)
            .contextWindow(1_000_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addGroqModels() {

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("llama-3.1-405b-reasoning")
            .displayName("Llama 3.1 405B Reasoning")
            .inputCost(0.07)
            .outputCost(0.07)
            .contextWindow(131_072)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("gemma-7b-it")
            .displayName("Gemma 7B it")
            .inputCost(0.07)
            .outputCost(0.07)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("llama3-8b-8192")
            .displayName("Llama 3 8B")
            .inputCost(0.05)
            .outputCost(0.05)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("llama3-70b-8192")
            .displayName("Llama 3 70B")
            .inputCost(0.59)
            .outputCost(0.79)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("mixtral-8x7b-32768")
            .displayName("Mixtral 8x7B")
            .inputCost(0.24)
            .outputCost(0.24)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addMistralModels() {
        models.add(LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(OPEN_MISTRAL_7B.toString())
            .displayName("Mistral 7B")
            .inputCost(0.25)
            .outputCost(0.25)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(OPEN_MIXTRAL_8x7B.toString())
            .displayName("Mistral 8x7B")
            .inputCost(0.7)
            .outputCost(0.7)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(OPEN_MIXTRAL_8X22B.toString())
            .displayName("Mistral 8x22b")
            .inputCost(2)
            .outputCost(6)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(MISTRAL_SMALL_LATEST.toString())
            .displayName("Mistral Small")
            .inputCost(1)
            .outputCost(3)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(MISTRAL_MEDIUM_LATEST.toString())
            .displayName("Mistral Medium")
            .inputCost(2.7)
            .outputCost(0.1)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(MISTRAL_LARGE_LATEST.toString())
            .displayName("Mistral Large")
            .inputCost(4)
            .outputCost(12)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName("codestral-2405")
            .displayName("Codestral")
            .inputCost(1)
            .outputCost(3)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addDeepSeekModels() {
        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepSeek)
            .modelName("deepseek-coder")
            .displayName("DeepSeek Coder")
            .inputCost(0.14)
            .outputCost(0.28)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.add(LanguageModel.builder()
            .provider(ModelProvider.DeepSeek)
            .modelName("deepseek-chat")
            .displayName("DeepSeek Chat")
            .inputCost(0.14)
            .outputCost(0.28)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull List<LanguageModel> getModels() {
        ArrayList<LanguageModel> languageModels = new ArrayList<>(models);
        addOpenRouterModels(languageModels);
        return languageModels;
    }

    private static void addOpenRouterModels(ArrayList<LanguageModel> languageModels) {
        OpenRouterChatModelFactory openRouterChatModelFactory = new OpenRouterChatModelFactory();
        String apiKey = openRouterChatModelFactory.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            List<LanguageModel> openRouterModels = new OpenRouterChatModelFactory().getModels();
            languageModels.addAll(openRouterModels);
        }
    }

    public void setModels(List<LanguageModel> models) {
        this.models = new ArrayList<>(models);
    }

    @Override
    public LLMModelRegistryService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull LLMModelRegistryService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
