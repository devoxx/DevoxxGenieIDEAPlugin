package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;
import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;
import static dev.langchain4j.model.openai.OpenAiChatModelName.*;

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

        String claude2 = CLAUDE_2.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claude2,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(claude2)
            .displayName("Claude 2.0")
            .inputCost(8)
            .outputCost(24)
            .contextWindow(100_000)
            .apiKeyUsed(true)
            .build());

        String claude21 = CLAUDE_2_1.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claude21,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(claude21)
            .displayName("Claude 2.1")
            .inputCost(8)
            .outputCost(24)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        String claudeHaiku3 = CLAUDE_3_HAIKU_20240307.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeHaiku3,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(claudeHaiku3)
            .displayName("Claude 3 Haiku")
            .inputCost(0.25)
            .outputCost(1.25)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        String claudeSonnet3 = CLAUDE_3_SONNET_20240229.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeSonnet3,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(claudeSonnet3)
            .displayName("Claude 3 Sonnet")
            .inputCost(3)
            .outputCost(15)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        String claudeOpus3 = CLAUDE_3_OPUS_20240229.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeOpus3,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(claudeOpus3)
            .displayName("Claude 3 Opus")
            .inputCost(15)
            .outputCost(75)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        String claudeSonnet35 = CLAUDE_3_5_SONNET_20241022.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeSonnet35,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(claudeSonnet35)
            .displayName("Claude 3.5 Sonnet")
            .inputCost(3)
            .outputCost(15)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());

        String claudeHaiku35 = CLAUDE_3_5_HAIKU_20241022.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeHaiku35,
            LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName(claudeHaiku35)
            .displayName("Claude 3.5 Haiku")
            .inputCost(1)
            .outputCost(5)
            .contextWindow(200_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addOpenAiModels() {

        String o1Mini = "o1-mini";
        models.put(ModelProvider.OpenAI.getName() + ":" + o1Mini,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(o1Mini)
            .displayName("o1 mini")
            .inputCost(5)
            .outputCost(15)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String o1Preview = "o1-preview";
        models.put(ModelProvider.OpenAI.getName() + ":" + o1Preview,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(o1Preview)
            .displayName("o1 preview")
            .inputCost(10)
            .outputCost(30)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String gpt35Turbo = GPT_3_5_TURBO.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt35Turbo,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(gpt35Turbo)
            .displayName("GPT 3.5 Turbo")
            .inputCost(0.5)
            .outputCost(1.5)
            .contextWindow(16_000)
            .apiKeyUsed(true)
            .build());

        String gpt4 = GPT_4.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt4,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(gpt4)
            .displayName("GPT 4")
            .inputCost(30)
            .outputCost(60)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        String gpt4TurboPreview = GPT_4_TURBO_PREVIEW.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt4TurboPreview,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(gpt4TurboPreview)
            .displayName("GPT 4 Turbo")
            .inputCost(10)
            .outputCost(30)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String gpt4o = GPT_4_O.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt4o,
            LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(gpt4o)
            .displayName("GPT 4o")
            .inputCost(5)
            .outputCost(15)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String gpt4oMini = GPT_4_O_MINI.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt4oMini,
                LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(gpt4oMini)
            .displayName("GPT 4o mini")
            .inputCost(0.15)
            .outputCost(0.6)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addDeepInfraModels() {
        String metaLlama31Instruct405B = "meta-llama/Meta-Llama-3.1-405B-Instruct";
        models.put(ModelProvider.DeepInfra.getName() + ":" + metaLlama31Instruct405B,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(metaLlama31Instruct405B)
            .displayName("Meta Llama 3.1 405B")
            .inputCost(2.7)
            .outputCost(2.7)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String metaLlama31Instruct70B = "meta-llama/Meta-Llama-3.1-70B-Instruct";
        models.put(ModelProvider.DeepInfra.getName() + ":" + metaLlama31Instruct70B,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(metaLlama31Instruct70B)
            .displayName("Meta Llama 3.1 70B")
            .inputCost(0.35)
            .outputCost(0.4)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String metaLlama31Instruct8B = "meta-llama/Meta-Llama-3.1-8B-Instruct";
        models.put(ModelProvider.DeepInfra.getName() + ":" + metaLlama31Instruct8B,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(metaLlama31Instruct8B)
            .displayName("Meta Llama 3.1 8B")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String mistralNemoInstruct2407 = "mistralai/Mistral-Nemo-Instruct-2407";
        models.put(ModelProvider.DeepInfra.getName() + ":" + mistralNemoInstruct2407,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(mistralNemoInstruct2407)
            .displayName("Mistral Nemo 12B")
            .inputCost(0.13)
            .outputCost(0.13)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String mistralMixtral8x7BInstruct = "mistralai/Mixtral-8x7B-Instruct-v0.1";
        models.put(ModelProvider.DeepInfra.getName() + ":" + mistralMixtral8x7BInstruct,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(mistralMixtral8x7BInstruct)
            .displayName("Mixtral 8x7B Instruct v0.1")
            .inputCost(0.24)
            .outputCost(0.24)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String mistralMixtral8x22BInstruct = "mistralai/Mixtral-8x22B-Instruct-v0.1";
        models.put(ModelProvider.DeepInfra.getName() + ":" + mistralMixtral8x22BInstruct,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(mistralMixtral8x22BInstruct)
            .displayName("Mixtral 8x22B Instruct v0.1")
            .inputCost(0.65)
            .outputCost(0.65)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        String mistralMistral7BInstruct = "mistralai/Mistral-7B-Instruct-v0.3";
        models.put(ModelProvider.DeepInfra.getName() + ":" + mistralMistral7BInstruct,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(mistralMistral7BInstruct)
            .displayName("Mistral 7B Instruct v0.3")
            .inputCost(0.07)
            .outputCost(0.07)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String microsoftWizardLM8x22B = "microsoft/WizardLM-2-8x22B";
        models.put(ModelProvider.DeepInfra.getName() + ":" + microsoftWizardLM8x22B,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(microsoftWizardLM8x22B)
            .displayName("Wizard LM 2 8x22B")
            .inputCost(0.5)
            .outputCost(0.5)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        String microsoftWizardLM7B = "microsoft/WizardLM-2-7B";
        models.put(ModelProvider.DeepInfra.getName() + ":" + microsoftWizardLM7B,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(microsoftWizardLM7B)
            .displayName("Wizard LM 2 7B")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String openchat35 = "openchat/openchat_3.5";
        models.put(ModelProvider.DeepInfra.getName() + ":" + openchat35,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(openchat35)
            .displayName("OpenChat 3.5")
            .inputCost(0.055)
            .outputCost(0.055)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        String googleGemma9b = "google/gemma-2-9b-it";
        models.put(ModelProvider.DeepInfra.getName() + ":" + googleGemma9b,
            LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(googleGemma9b)
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

        String gemini15Flash = "gemini-1.5-flash";
        models.put(ModelProvider.Google.getName() + ":" + gemini15Flash,
            LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName(gemini15Flash)
            .displayName("Gemini 1.5 Flash")
            .inputCost(0.0375)
            .outputCost(0.6)
            .contextWindow(1_000_000)
            .apiKeyUsed(true)
            .build());

        String gemini15Pro = "gemini-1.5-pro";
        models.put(ModelProvider.Google.getName() + ":" + gemini15Pro,
            LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName(gemini15Pro)
            .displayName("Gemini 1.5 Pro")
            .inputCost(7)
            .outputCost(21)
            .contextWindow(2_000_000)
            .apiKeyUsed(true)
            .build());

        String gemini15ProExp0801 = "gemini-1.5-pro-exp-0801";
        models.put(ModelProvider.Google.getName() + ":" + gemini15ProExp0801,
            LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName(gemini15ProExp0801)
            .displayName("Gemini 1.5 Pro 0801")
            .inputCost(7)
            .outputCost(21)
            .contextWindow(2_000_000)
            .apiKeyUsed(true)
            .build());

        String gemini10Pro = "gemini-1.0-pro";
        models.put(ModelProvider.Google.getName() + ":" + gemini10Pro,
            LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName(gemini10Pro)
            .displayName("Gemini 1.0 Pro")
            .inputCost(0.5)
            .outputCost(1.5)
            .contextWindow(1_000_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addGroqModels() {

        String gemma7b = "gemma-7b-it";
        models.put(ModelProvider.Groq.getName() + ":" + gemma7b,
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(gemma7b)
            .displayName("Gemma 7B it")
            .inputCost(0.07)
            .outputCost(0.07)
            .contextWindow(8_192)
            .apiKeyUsed(true)
            .build());

        String gemma2 = "gemma2-9b-it";
        models.put(ModelProvider.Groq.getName() + ":" + gemma2,
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(gemma2)
            .displayName("Gemma 2 9B it")
            .inputCost(0.2)
            .outputCost(0.2)
            .contextWindow(8_192)
            .apiKeyUsed(true)
            .build());

        String llama3 = "llama3-8b-8192";
        models.put(ModelProvider.Groq.getName() + ":" + llama3,
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(llama3)
            .displayName("Llama 3 8B")
            .inputCost(0.05)
            .outputCost(0.05)
            .contextWindow(8_000)
            .apiKeyUsed(true)
            .build());

        String llama31Versatile = "llama-3.1-70b-versatile";
        models.put(ModelProvider.Groq.getName() + ":" + llama31Versatile,
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(llama31Versatile)
            .displayName("Llama 3.1 70B")
            .inputCost(0.59)
            .outputCost(0.79)
            .contextWindow(131_072)
            .apiKeyUsed(true)
            .build());

        String llama31Instant = "llama-3.1-8b-instant";
        models.put(ModelProvider.Groq.getName() + ":" + llama31Instant,
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(llama31Instant)
            .displayName("Llama 3.1 8B")
            .inputCost(0.05)
            .outputCost(0.08)
            .contextWindow(131_072)
            .apiKeyUsed(true)
            .build());

        String mixtral8x7b = "mixtral-8x7b-32768";
        models.put(ModelProvider.Groq.getName() + ":" + mixtral8x7b,
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(mixtral8x7b)
            .displayName("Mixtral 8x7B")
            .inputCost(0.24)
            .outputCost(0.24)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String llama370b = "llama3-70b-8192";
        models.put(ModelProvider.Groq.getName() + ":" + llama370b,
            LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(llama370b)
            .displayName("Llama 3 70B")
            .inputCost(0.59)
            .outputCost(0.79)
            .contextWindow(8192)
            .apiKeyUsed(true)
            .build());
    }

    private void addMistralModels() {
        String openMistral7B = OPEN_MISTRAL_7B.toString();
        models.put(ModelProvider.Mistral.getName() + ":" + openMistral7B,
            LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(openMistral7B)
            .displayName("Mistral 7B")
            .inputCost(0.25)
            .outputCost(0.25)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String openMixtral8x7B = OPEN_MIXTRAL_8x7B.toString();
        models.put(ModelProvider.Mistral.getName() + ":" + openMixtral8x7B,
            LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(openMixtral8x7B)
            .displayName("Mistral 8x7B")
            .inputCost(0.7)
            .outputCost(0.7)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String openMixtral8x22B = OPEN_MIXTRAL_8X22B.toString();
        models.put(ModelProvider.Mistral.getName() + ":" + openMixtral8x22B,
            LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(openMixtral8x22B)
            .displayName("Mistral 8x22b")
            .inputCost(2)
            .outputCost(6)
            .contextWindow(64_000)
            .apiKeyUsed(true)
            .build());

        String mistralSmallLatest = MISTRAL_SMALL_LATEST.toString();
        models.put(ModelProvider.Mistral.getName() + ":" + mistralSmallLatest,
            LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(mistralSmallLatest)
            .displayName("Mistral Small")
            .inputCost(1)
            .outputCost(3)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String mistralMediumLatest = MISTRAL_MEDIUM_LATEST.toString();
        models.put(ModelProvider.Mistral.getName() + ":" + mistralMediumLatest,
            LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(mistralMediumLatest)
            .displayName("Mistral Medium")
            .inputCost(2.7)
            .outputCost(0.1)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String mistralLargeLatest = MISTRAL_LARGE_LATEST.toString();
        models.put(ModelProvider.Mistral.getName() + ":" + mistralLargeLatest,
            LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(mistralLargeLatest)
            .displayName("Mistral Large")
            .inputCost(4)
            .outputCost(12)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());

        String codestral = "codestral-2405";
        models.put(ModelProvider.Mistral.getName() + ":" + codestral,
            LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName(codestral)
            .displayName("Codestral")
            .inputCost(1)
            .outputCost(3)
            .contextWindow(32_000)
            .apiKeyUsed(true)
            .build());
    }

    private void addDeepSeekModels() {
        String coder = "deepseek-coder";
        models.put(ModelProvider.DeepSeek.getName() + ":" + coder,
            LanguageModel.builder()
            .provider(ModelProvider.DeepSeek)
            .modelName(coder)
            .displayName("DeepSeek Coder")
            .inputCost(0.14)
            .outputCost(0.28)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());

        String chat = "deepseek-chat";
        models.put(ModelProvider.DeepSeek.getName() + ":" + chat,
            LanguageModel.builder()
            .provider(ModelProvider.DeepSeek)
            .modelName(chat)
            .displayName("DeepSeek Chat")
            .inputCost(0.14)
            .outputCost(0.28)
            .contextWindow(128_000)
            .apiKeyUsed(true)
            .build());
    }

    @NotNull
    public List<LanguageModel> getModels() {
        ModelProvider OPEN_ROUTER_PROVIDER = ModelProvider.OpenRouter;

        // Create a copy of the current models
        Map<String, LanguageModel> modelsCopy = new HashMap<>(models);

        // Add OpenRouter models if API key exists
        OpenRouterChatModelFactory openRouterChatModelFactory = new OpenRouterChatModelFactory();
        String apiKey = openRouterChatModelFactory.getApiKey(OPEN_ROUTER_PROVIDER);
        if (apiKey != null && !apiKey.isEmpty()) {
            openRouterChatModelFactory.getModels().forEach(model ->
                modelsCopy.put(OPEN_ROUTER_PROVIDER.getName() + ":" + model.getModelName(), model));
        }

        return new ArrayList<>(modelsCopy.values());
    }

    public void setModels(Map<String, LanguageModel> models) {
        this.models = new HashMap<>(models);
    }
}
