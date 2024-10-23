package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;
import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;

@Service
public final class LLMModelRegistryService {

    private Map<String, LanguageModel> models = new HashMap<>();

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

        models.put(ModelProvider.Anthropic.getName() + "-" + CLAUDE_INSTANT_1_2, LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_INSTANT_1_2.toString())
            .displayName("Claude 1.2")
            .inputCost(0.8)
            .outputCost(2.4)
            .contextWindow(100_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Anthropic.getName() + "-" + AnthropicChatModelName.CLAUDE_2,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(AnthropicChatModelName.CLAUDE_2.toString())
            .displayName("Claude 2.0")
            .inputCost(8)
            .outputCost(24)
            .contextWindow(100_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Anthropic.getName() + "-" + AnthropicChatModelName.CLAUDE_2_1,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(AnthropicChatModelName.CLAUDE_2_1.toString())
            .displayName("Claude 2.1")
            .inputCost(8)
            .outputCost(24)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Anthropic.getName() + "-" + CLAUDE_3_HAIKU_20240307,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_3_HAIKU_20240307.toString())
            .displayName("Claude 3 Haiku")
            .inputCost(0.25)
            .outputCost(1.25)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Anthropic.getName() + "-" + CLAUDE_3_SONNET_20240229,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_3_SONNET_20240229.toString())
            .displayName("Claude 3 Sonnet")
            .inputCost(3)
            .outputCost(15)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Anthropic.getName() + "-" + CLAUDE_3_OPUS_20240229,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(CLAUDE_3_OPUS_20240229.toString())
            .displayName("Claude 3 Opus")
            .inputCost(15)
            .outputCost(75)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Anthropic.getName() + "-claude-3-5-sonnet-20240620",
            LanguageModel.builder()
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

        models.put(ModelProvider.OpenAI.getName() + ":o1-mini",
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName("o1-mini")
            .displayName("o1 mini")
            .inputCost(5)
            .outputCost(15)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.OpenAI.getName() + ":o1-preview",
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName("o1-preview")
            .displayName("o1 preview")
            .inputCost(10)
            .outputCost(30)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.OpenAI.getName() + ":" + OpenAiChatModelName.GPT_3_5_TURBO,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_3_5_TURBO.toString())
            .displayName("GPT 3.5 Turbo")
            .inputCost(0.5)
            .outputCost(1.5)
            .contextWindow(16_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.OpenAI.getName() + ":" + OpenAiChatModelName.GPT_3_5_TURBO,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_3_5_TURBO.toString())
            .displayName("GPT 4")
            .inputCost(30)
            .outputCost(60)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.OpenAI.getName() + ":" + OpenAiChatModelName.GPT_4_TURBO_PREVIEW,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_4_TURBO_PREVIEW.toString())
            .displayName("GPT 4 Turbo")
            .inputCost(10)
            .outputCost(30)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.OpenAI.getName() + ":" + OpenAiChatModelName.GPT_4_O,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(OpenAiChatModelName.GPT_4_O.toString())
            .displayName("GPT 4o")
            .inputCost(5)
            .outputCost(15)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.OpenAI.getName() + ":gpt-4o-mini",
                LanguageModel.builder()
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
        models.put(ModelProvider.DeepInfra.getName() + ":meta-llama/Meta-Llama-3.1-405B-Instruct",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("meta-llama/Meta-Llama-3.1-405B-Instruct")
            .displayName("Meta Llama 3.1 405B")
            .inputCost(2.7)
            .outputCost(2.7)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":meta-llama/Meta-Llama-3.1-70B-Instruct",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("meta-llama/Meta-Llama-3.1-70B-Instruct")
            .displayName("Meta Llama 3.1 70B")
            .inputCost(0.35)
            .outputCost(0.4)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":meta-llama/Meta-Llama-3.1-8B-Instruct",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("meta-llama/Meta-Llama-3.1-8B-Instruct")
            .displayName("Meta Llama 3.1 8B")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":mistralai/Mistral-Nemo-Instruct-2407",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mistral-Nemo-Instruct-2407")
            .displayName("Mistral Nemo 12B")
            .inputCost(0.13)
            .outputCost(0.13)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":mistralai/Mixtral-8x7B-Instruct-v0.1",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mixtral-8x7B-Instruct-v0.1")
            .displayName("Mixtral 8x7B Instruct v0.1")
            .inputCost(0.24)
            .outputCost(0.24)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":mistralai/Mixtral-8x22B-Instruct-v0.1",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mixtral-8x22B-Instruct-v0.1")
            .displayName("Mixtral 8x22B Instruct v0.1")
            .inputCost(0.65)
            .outputCost(0.65)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":mistralai/Mistral-7B-Instruct-v0.3",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mistral-7B-Instruct-v0.3")
            .displayName("Mistral 7B Instruct v0.3")
            .inputCost(0.07)
            .outputCost(0.07)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":microsoft/WizardLM-2-8x22B",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("microsoft/WizardLM-2-8x22B")
            .displayName("Wizard LM 2 8x22B")
            .inputCost(0.5)
            .outputCost(0.5)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":microsoft/WizardLM-2-7B",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("microsoft/WizardLM-2-7B")
            .displayName("Wizard LM 2 7B")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":openchat/openchat_3.5",
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("openchat/openchat_3.5")
            .displayName("OpenChat 3.5")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepInfra.getName() + ":google/gemma-2-9b-it",
            LanguageModel.builder()
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

        models.put(ModelProvider.Google.getName() + ":gemini-1.5-flash",
            LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.5-flash")
            .displayName("Gemini 1.5 Flash")
            .inputCost(0.0375)
            .outputCost(0.6)
            .contextWindow(1_000_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Google.getName() + ":gemini-1.5-pro",
            LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.5-pro")
            .displayName("Gemini 1.5 Pro")
            .inputCost(7)
            .outputCost(21)
            .contextWindow(2_000_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Google.getName() + ":gemini-1.5-pro-exp-0801",
            LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.5-pro-exp-0801")
            .displayName("Gemini 1.5 Pro 0801")
            .inputCost(7)
            .outputCost(21)
            .contextWindow(2_000_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Google.getName() + ":gemini-1.0-pro",
            LanguageModel.builder()
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

        models.put(ModelProvider.Groq.getName() + ":gemma-7b-it",
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("gemma-7b-it")
            .displayName("Gemma 7B it")
            .inputCost(0.07)
            .outputCost(0.07)
            .contextWindow(8_192)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Groq.getName() + ":gemma2-9b-it", LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("gemma2-9b-it")
            .displayName("Gemma 2 9B it")
            .inputCost(0.2)
            .outputCost(0.2)
            .contextWindow(8_192)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Groq.getName() + ":llama3-8b-8192", LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("llama3-8b-8192")
            .displayName("Llama 3 8B")
            .inputCost(0.05)
            .outputCost(0.05)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Groq.getName() + ":llama-3.1-70b-versatile", LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("llama-3.1-70b-versatile")
            .displayName("Llama 3.1 70B")
            .inputCost(0.59)
            .outputCost(0.79)
            .contextWindow(131_072)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Groq.getName() + ":llama-3.1-8b-instant", LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("llama-3.1-8b-instant")
            .displayName("Llama 3.1 8B")
            .inputCost(0.05)
            .outputCost(0.08)
            .contextWindow(131_072)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Groq.getName() + ":mixtral-8x7b-32768", LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("mixtral-8x7b-32768")
            .displayName("Mixtral 8x7B")
            .inputCost(0.24)
            .outputCost(0.24)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Groq.getName() + ":llama3-70b-8192", LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName("llama3-70b-8192")
            .displayName("Llama 3 70B")
            .inputCost(0.59)
            .outputCost(0.79)
            .contextWindow(8192)
            .apiKeyUsed(true)
            .build());
    }

    private void addMistralModels() {
        models.put(ModelProvider.Mistral.getName() + ":" + OPEN_MISTRAL_7B, LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(OPEN_MISTRAL_7B.toString())
            .displayName("Mistral 7B")
            .inputCost(0.25)
            .outputCost(0.25)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Mistral.getName() + ":" + OPEN_MIXTRAL_8x7B, LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(OPEN_MIXTRAL_8x7B.toString())
            .displayName("Mistral 8x7B")
            .inputCost(0.7)
            .outputCost(0.7)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Mistral.getName() + ":" + OPEN_MIXTRAL_8X22B, LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(OPEN_MIXTRAL_8X22B.toString())
            .displayName("Mistral 8x22b")
            .inputCost(2)
            .outputCost(6)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Mistral.getName() + ":" + MISTRAL_SMALL_LATEST, LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(MISTRAL_SMALL_LATEST.toString())
            .displayName("Mistral Small")
            .inputCost(1)
            .outputCost(3)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Mistral.getName() + ":" + MISTRAL_MEDIUM_LATEST, LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(MISTRAL_MEDIUM_LATEST.toString())
            .displayName("Mistral Medium")
            .inputCost(2.7)
            .outputCost(0.1)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Mistral.getName() + ":" + MISTRAL_LARGE_LATEST, LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(MISTRAL_LARGE_LATEST.toString())
            .displayName("Mistral Large")
            .inputCost(4)
            .outputCost(12)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.Mistral.getName() + ":codestral-2405", LanguageModel.builder()
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
        models.put(ModelProvider.DeepSeek.getName() + ":deepseek-coder", LanguageModel.builder()
            .provider(ModelProvider.DeepSeek)
            .modelName("deepseek-coder")
            .displayName("DeepSeek Coder")
            .inputCost(0.14)
            .outputCost(0.28)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        models.put(ModelProvider.DeepSeek.getName() + ":deepseek-chat", LanguageModel.builder()
            .provider(ModelProvider.DeepSeek)
            .modelName("deepseek-chat")
            .displayName("DeepSeek Chat")
            .inputCost(0.14)
            .outputCost(0.28)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());
    }

    public @NotNull List<LanguageModel> getModels() {
        Map<String, LanguageModel> languageModels = new HashMap<>(models);
        addOpenRouterModels(languageModels);
        return new ArrayList<>(languageModels.values());
    }

    private static void addOpenRouterModels(Map<String, LanguageModel> languageModels) {
        OpenRouterChatModelFactory openRouterChatModelFactory = new OpenRouterChatModelFactory();
        String apiKey = openRouterChatModelFactory.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            new OpenRouterChatModelFactory().getModels().forEach(model -> {
                languageModels.put(ModelProvider.OpenRouter.getName() + ":" + model.getModelName(), model);
            });
        }
    }

    public void setModels(Map<String, LanguageModel> models) {
        this.models = new HashMap<>(models);
    }
}
