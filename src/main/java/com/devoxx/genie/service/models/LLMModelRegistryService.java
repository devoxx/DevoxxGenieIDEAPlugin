package com.devoxx.genie.service.models;

import com.devoxx.genie.chatmodel.cloud.openai.OpenAIChatModelName;
import com.devoxx.genie.chatmodel.cloud.openrouter.OpenRouterChatModelFactory;
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
        addBedrockModels();
        addGrokModels();
    }

    private void addAnthropicModels() {

        // Available until March 2025
        String claudeHaiku3 = CLAUDE_3_HAIKU_20240307.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeHaiku3,
                LanguageModel.builder()
                        .provider(ModelProvider.Anthropic)
                        .modelName(claudeHaiku3)
                        .displayName("Claude 3 Haiku (20240307)")
                        .inputCost(0.25)
                        .outputCost(1.25)
                        .inputMaxTokens(200_000)
                        .apiKeyUsed(true)
                        .build());

        // Available until October 2025
        String claudeHaiku35 = CLAUDE_3_5_HAIKU_20241022.toString();
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeHaiku35,
                LanguageModel.builder()
                        .provider(ModelProvider.Anthropic)
                        .modelName(claudeHaiku35)
                        .displayName("Claude 3.5 Haiku (20241022)")
                        .inputCost(1)
                        .outputCost(5)
                        .inputMaxTokens(200_000)
                        .apiKeyUsed(true)
                        .build());

        String claudeSonnet37 = "claude-3-7-sonnet-latest";
        models.put(ModelProvider.Anthropic.getName() + "-" + claudeSonnet37,
                LanguageModel.builder()
                        .provider(ModelProvider.Anthropic)
                        .modelName(claudeSonnet37)
                        .displayName("Claude 3.7 Sonnet")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .apiKeyUsed(true)
                        .build());

        String claude4Opus = "claude-opus-4-20250514";
        models.put(ModelProvider.Anthropic.getName() + "-" + claude4Opus,
                LanguageModel.builder()
                        .provider(ModelProvider.Anthropic)
                        .modelName(claude4Opus)
                        .displayName("Claude 4 Opus")
                        .inputCost(15)
                        .outputCost(75)
                        .inputMaxTokens(200_000)
                        .apiKeyUsed(true)
                        .build());

        String claude4Sonnet = "claude-sonnet-4-20250514";
        models.put(ModelProvider.Anthropic.getName() + "-" + claude4Sonnet,
                LanguageModel.builder()
                        .provider(ModelProvider.Anthropic)
                        .modelName(claude4Sonnet)
                        .displayName("Claude 4 Sonnet")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .apiKeyUsed(true)
                        .build());
    }

    private void addOpenAiModels() {

        String gpt41Model = "gpt-4.1-2025-04-14";
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt41Model,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(gpt41Model)
                        .displayName("GPT 4.1 (2025-04-14)")
                        .inputCost(2)
                        .outputCost(8)
                        .inputMaxTokens(1_000_000)
                        .outputMaxTokens(32_000)
                        .apiKeyUsed(true)
                        .build());

        String gpt41MiniModel = "gpt-4.1-mini-2025-04-14";
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt41MiniModel,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(gpt41MiniModel)
                        .displayName("GPT 4.1 Mini (2025-04-14)")
                        .inputCost(0.4)
                        .outputCost(1.6)
                        .inputMaxTokens(1_000_000)
                        .outputMaxTokens(32_000)
                        .apiKeyUsed(true)
                        .build());

        String gpt41NanoModel = "gpt-4.1-nano-2025-04-14";
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt41NanoModel,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(gpt41NanoModel)
                        .displayName("GPT 4.1 Nano (2025-04-14)")
                        .inputCost(0.1)
                        .outputCost(0.4)
                        .inputMaxTokens(1_000_000)
                        .outputMaxTokens(32_000)
                        .apiKeyUsed(true)
                        .build());

        String o3MiniModel = OpenAIChatModelName.O3_MINI.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + o3MiniModel,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(o3MiniModel)
                        .displayName("o3-mini")
                        .inputCost(5)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .outputMaxTokens(100_000)
                        .apiKeyUsed(true)
                        .build());

        String o1Model = OpenAIChatModelName.O1.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + o1Model,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(o1Model)
                        .displayName("o1")
                        .inputCost(5)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .outputMaxTokens(100_000)
                        .apiKeyUsed(true)
                        .build());

        String o1Mini = OpenAIChatModelName.O1_MINI.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + o1Mini,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(o1Mini)
                        .displayName("o1-mini")
                        .inputCost(5)
                        .outputCost(15)
                        .inputMaxTokens(128_000)
                        .outputMaxTokens(65_536)
                        .apiKeyUsed(true)
                        .build());

        String o1Preview = OpenAIChatModelName.O1_PREVIEW.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + o1Preview,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(o1Preview)
                        .displayName("o1 preview")
                        .inputCost(10)
                        .outputCost(30)
                        .inputMaxTokens(128_000)
                        .outputMaxTokens(32_768)
                        .apiKeyUsed(true)
                        .build());

        String gpt4dot5 = OpenAIChatModelName.GPT_4_5.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt4dot5,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(gpt4dot5)
                        .displayName("GPT 4.5 Preview")
                        .inputCost(75)
                        .outputCost(150)
                        .inputMaxTokens(128_000)
                        .outputMaxTokens(8_192)
                        .apiKeyUsed(true)
                        .build());

        String gpt4 = OpenAIChatModelName.GPT_4.toString();
        models.put(ModelProvider.OpenAI.getName() + ":" + gpt4,
                LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName(gpt4)
                        .displayName("GPT 4")
                        .inputCost(30)
                        .outputCost(60)
                        .inputMaxTokens(8_192)
                        .outputMaxTokens(8_192)
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
                        .inputMaxTokens(128_000)
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
                        .inputMaxTokens(128_000)
                        .outputMaxTokens(16_384)
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
                        .inputMaxTokens(128_000)
                        .outputMaxTokens(4_096)
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
                        .inputMaxTokens(16_385)
                        .outputMaxTokens(4_096)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(128_000)
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
                        .inputMaxTokens(128_000)
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
                        .inputMaxTokens(128_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(64_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(64_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(8_000)
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
                        .inputMaxTokens(4_000)
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
                        .inputCost(0.15)
                        .outputCost(0.6)
                        .inputMaxTokens(1_048_576)
                        .outputMaxTokens(8_192)
                        .apiKeyUsed(true)
                        .build());

        String gemini15Flash8B = "gemini-1.5-flash-8b";
        models.put(ModelProvider.Google.getName() + ":" + gemini15Flash8B,
                LanguageModel.builder()
                        .provider(ModelProvider.Google)
                        .modelName(gemini15Flash8B)
                        .displayName("Gemini 1.5 Flash 8B")
                        .inputCost(0.0375)
                        .outputCost(0.6)
                        .inputMaxTokens(1_048_576)
                        .outputMaxTokens(8_192)
                        .apiKeyUsed(true)
                        .build());

        String gemini15ProExp0801 = "gemini-1.5-pro";
        models.put(ModelProvider.Google.getName() + ":" + gemini15ProExp0801,
                LanguageModel.builder()
                        .provider(ModelProvider.Google)
                        .modelName(gemini15ProExp0801)
                        .displayName("Gemini 1.5 Pro")
                        .inputCost(1.25)
                        .outputCost(5)
                        .inputMaxTokens(2_000_000)
                        .apiKeyUsed(true)
                        .build());

        String gemini2FlashExp = "gemini-2.0-flash-001";
        models.put(ModelProvider.Google.getName() + ":" + gemini2FlashExp,
                LanguageModel.builder()
                        .provider(ModelProvider.Google)
                        .modelName(gemini2FlashExp)
                        .displayName("Gemini 2.0 Flash")
                        .inputCost(0.10)
                        .outputCost(0.40)
                        .inputMaxTokens(1_048_576)
                        .outputMaxTokens(8_192)
                        .apiKeyUsed(true)
                        .build());

        String geminiFlashThinking = "gemini-2.0-flash-thinking-exp-01-21";
        models.put(ModelProvider.Google.getName() + ":" + geminiFlashThinking,
                LanguageModel.builder()
                        .provider(ModelProvider.Google)
                        .modelName(geminiFlashThinking)
                        .displayName("Gemini 2.0 Flash Thinking Exp. 01-21")
                        .inputCost(0)
                        .outputCost(0)
                        .inputMaxTokens(1_048_576)
                        .outputMaxTokens(64_000)
                        .apiKeyUsed(true)
                        .build());

        String gemini2FlashLite = " gemini-2.0-flash-lite-001";
        models.put(ModelProvider.Google.getName() + ":" + gemini2FlashLite,
                LanguageModel.builder()
                        .provider(ModelProvider.Google)
                        .modelName(gemini2FlashLite)
                        .displayName("Gemini 2.0 Flash Lite")
                        .inputCost(0.075)
                        .outputCost(0.30)
                        .inputMaxTokens(1_048_576)
                        .outputMaxTokens(8_192)
                        .apiKeyUsed(true)
                        .build());

        // gemini-2.5-pro-preview-05-06
        String gemini2dot5Pro0506 = "gemini-2.5-pro-preview-05-06";
        models.put(ModelProvider.Google.getName() + ":" + gemini2dot5Pro0506,
                LanguageModel.builder()
                        .provider(ModelProvider.Google)
                        .modelName(gemini2dot5Pro0506)
                        .displayName("Gemini 2.5 Pro Preview 05-06")
                        .inputCost(2.50)
                        .outputCost(15.0)
                        .inputMaxTokens(1_048_576)
                        .outputMaxTokens(64_000)
                        .apiKeyUsed(true)
                        .build());

        // gemini-2.5-flash-preview-04-17
        String gemini2dot5Flash = "gemini-2.5-flash-preview-04-17";
        models.put(ModelProvider.Google.getName() + ":" + gemini2dot5Flash,
                LanguageModel.builder()
                        .provider(ModelProvider.Google)
                        .modelName(gemini2dot5Flash)
                        .displayName("Gemini 2.5 Flash Preview 04-17")
                        .inputCost(0.15)
                        .outputCost(3.50)
                        .inputMaxTokens(1_048_576)
                        .outputMaxTokens(65_536)
                        .apiKeyUsed(true)
                        .build());
    }

    private void addGroqModels() {

        String gemma2 = "gemma2-9b-it";
        models.put(ModelProvider.Groq.getName() + ":" + gemma2,
                LanguageModel.builder()
                        .provider(ModelProvider.Groq)
                        .modelName(gemma2)
                        .displayName("Gemma 2 9B it")
                        .inputCost(0.2)
                        .outputCost(0.2)
                        .inputMaxTokens(8_192)
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
                        .inputMaxTokens(8_000)
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
                        .inputMaxTokens(131_072)
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
                        .inputMaxTokens(131_072)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(8192)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(64_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(32_000)
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
                        .inputMaxTokens(128_000)
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
                        .inputMaxTokens(128_000)
                        .apiKeyUsed(true)
                        .build());
    }

    /**
     * The Bedrock models.
     * @link <a href="https://ai21.com/docs/bedrock">Pricing</a>
     */
    private void addBedrockModels() {

        // AI21 Labs @ https://www.ai21.com/jamba

        String ai12JambaInstruct = "ai21.jamba-instruct-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + ai12JambaInstruct,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(ai12JambaInstruct)
                        .displayName("AI21 Labs - Jamba Instruct")
                        .inputCost(0.5)
                        .outputCost(0.7)
                        .inputMaxTokens(8_192)
                        .build());

        String ai12J2MidV1 = "ai21.ai21.j2-mid-v1";
        models.put(ModelProvider.Bedrock.getName() + ":" + ai12J2MidV1,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(ai12J2MidV1)
                        .displayName("AI21 Labs - Jurassic-2 Mid v1")
                        .inputCost(12.5)
                        .outputCost(12.5)
                        .inputMaxTokens(8_192)
                        .build());

        String ai12J2UltraV1 = "ai21.ai21.j2-ultra-v1";
        models.put(ModelProvider.Bedrock.getName() + ":" + ai12J2UltraV1,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(ai12J2UltraV1)
                        .displayName("AI21 Labs - Jurassic-2 ultra v1")
                        .inputCost(18.8)
                        .outputCost(18.8)
                        .inputMaxTokens(8_192)
                        .build());

        String ai12Jamba15LargeV1 = "ai21.jamba-1-5-large-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + ai12Jamba15LargeV1,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(ai12Jamba15LargeV1)
                        .displayName("AI21 Labs - Jamba 1.5 Large")
                        .inputCost(2)
                        .outputCost(8)
                        .inputMaxTokens(256_000)
                        .build());

        String ai12Jamba15MiniV1 = "ai21.jamba-1-5-mini-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + ai12Jamba15MiniV1,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(ai12Jamba15MiniV1)
                        .displayName("AI21 Labs - Jamba 1.5 Mini")
                        .inputCost(0.2)
                        .outputCost(.4)
                        .inputMaxTokens(256_000)
                        .build());

        // Excluded from list
        // Anthropic - Claude Instant - anthropic.claude-instant-v1
        // Anthropic - Claude - anthropic.claude-v2:1
        // Anthropic - Claude - anthropic.claude-v2

        // Anthropic - Claude 4 Sonnet - anthropic.claude-sonnet-4-20250514-v1:0
        String claude4 = "anthropic.claude-sonnet-4-20250514-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + claude4,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(claude4)
                        .displayName("Claude Sonnet 4")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .build());

        // Anthropic - Claude 3.7 Sonnet - anthropic.claude-3-7-sonnet-20250219-v1:0
        String claude3dot7 = "anthropic.claude-3-7-sonnet-20250219-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + claude3dot7,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(claude3dot7)
                        .displayName("Claude 3.7 Sonnet")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .build());

        // Anthropic - Claude 3 Sonnet - anthropic.claude-3-sonnet-20240229-v1:0
        String claude3v1 = "anthropic.claude-3-sonnet-20240229-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + claude3v1,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(claude3v1)
                        .displayName("Claude 3 Sonnet v1")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .build());

        // Claude 3 Haiku - anthropic.claude-3-haiku-20240307-v1:0
        String claude3dot2haiku = "anthropic.claude-3-haiku-20240307-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + claude3dot2haiku,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(claude3dot2haiku)
                        .displayName("Claude 3.5 Haiku")
                        .inputCost(0.8)
                        .outputCost(4)
                        .inputMaxTokens(200_000)
                        .build());

        // Anthropic - Claude 3.5 Sonnet v2 - anthropic.claude-3-5-sonnet-20241022-v2:0
        String claude3dot5v2 = "anthropic.claude-3-5-sonnet-20241022-v2:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + claude3dot5v2,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(claude3dot5v2)
                        .displayName("Claude 3.5 Sonnet v2")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .build());

        // Anthropic - Claude 3.5 Sonnet - anthropic.claude-3-5-sonnet-20240620-v1:0
        String claude3dot5v1 = "anthropic.claude-3-5-sonnet-20240620-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + claude3dot5v1,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(claude3dot5v1)
                        .displayName("Claude 3.5 Sonnet v1")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .build());

        String claude3dot5 = "anthropic.claude-3-sonnet-20240229-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + claude3dot5,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(claude3dot5)
                        .displayName("Claude 3 Sonnet")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(200_000)
                        .build());


        // Excluded because it's for images
        // Stability AI - SDXL 1.0 - stability.stable-diffusion-xl-v1

        // https://docs.cohere.com/v2/docs/command-r
        // Cohere - Command R - cohere.command-r-v1:0
        // Cohere - Command - cohere.command-text-v14
        // Cohere - Command Light - cohere.command-light-text-v14
        // Cohere - Command R+ - cohere.command-r-plus-v1:0

        // Embedding model, not supported by DevoxxGenie
        // Cohere - Embed English - cohere.embed-english-v3
        // Cohere - Embed Multilingual - cohere.embed-multilingual-v3

        // Cohere - Command R - cohere.command-r-v1:0
        String cohereCommandR = "cohere.command-r-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + cohereCommandR,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(cohereCommandR)
                        .displayName("Cohere - Command R")
                        .inputCost(0.5)
                        .outputCost(1.5)
                        .inputMaxTokens(128_000)
                        .build());

        // Cohere - Command - cohere.command-text-v14
        String cohereCommand = "cohere.command-text-v14";
        models.put(ModelProvider.Bedrock.getName() + ":" + cohereCommand,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(cohereCommand)
                        .displayName("Cohere - Command")
                        .inputCost(1.5)
                        .outputCost(2)
                        .inputMaxTokens(4_000)
                        .build());

        // Cohere - Command Light - cohere.command-light-text-v14
        String cohereCommandLight = "cohere.command-light-text-v14";
        models.put(ModelProvider.Bedrock.getName() + ":" + cohereCommandLight,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(cohereCommandLight)
                        .displayName("Cohere - Command Light")
                        .inputCost(0.3)
                        .outputCost(0.6)
                        .inputMaxTokens(4_000)
                        .build());

        // Cohere - Command R+ - cohere.command-r-plus-v1:0
        String cohereCommandRPlus = "cohere.command-r-plus-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + cohereCommandRPlus,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(cohereCommandRPlus)
                        .displayName("Cohere - Command R+")
                        .inputCost(3)
                        .outputCost(15)
                        .inputMaxTokens(128_000)
                        .build());

        // META @ https://ai.meta.com/blog/meta-llama-3/
        // Meta - Llama 3 70B Instruct - meta.llama3-70b-instruct-v1:0
        String metaLlama3_70BInstruct = "meta.llama3-70b-instruct-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + metaLlama3_70BInstruct,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(metaLlama3_70BInstruct)
                        .displayName("Meta - Llama 3 70B Instruct")
                        .inputCost(2.65)
                        .outputCost(3.5)
                        .inputMaxTokens(128_000)
                        .build());

        // Meta - Llama 3 8B Instruct - meta.llama3-8b-instruct-v1:0
        String metaLlama3_8BInstruct = "meta.llama3-8b-instruct-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + metaLlama3_8BInstruct,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(metaLlama3_8BInstruct)
                        .displayName("Meta - Llama 3 8B Instruct")
                        .inputCost(0.3)
                        .outputCost(0.6)
                        .inputMaxTokens(128_000)
                        .build());

        // Mistral @ https://docs.mistral.ai/getting-started/models/models_overview/

        // Mistral AI - Mistral Large (24.02) - mistral.mistral-large-2402-v1:0
        String mistralLargeV1 = "mistral.mistral-large-2402-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + mistralLargeV1,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(mistralLargeV1)
                        .displayName("Mistral AI - Mistral Large (24.02)")
                        .inputCost(4)
                        .outputCost(12)
                        .inputMaxTokens(131_000)
                        .build());

        // Mistral AI - Mistral Small (24.02) - mistral.mistral-small-2402-v1:0
        String mistralSmall = "mistral.mistral-small-2402-v1:0";
        models.put(ModelProvider.Bedrock.getName() + ":" + mistralSmall,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(mistralSmall)
                        .displayName("Mistral AI - Mistral Small (24.02)")
                        .inputCost(1)
                        .outputCost(3)
                        .inputMaxTokens(131_000)
                        .build());

        // Mistral AI - Mixtral 8x7B Instruct - mistral.mixtral-8x7b-instruct-v0:1
        String mistralMixtral8x7b = "mistral.mixtral-8x7b-instruct-v0:1";
        models.put(ModelProvider.Bedrock.getName() + ":" + mistralMixtral8x7b,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(mistralMixtral8x7b)
                        .displayName("Mistral AI - Mixtral 8x7B Instruct")
                        .inputCost(0.45)
                        .outputCost(0.7)
                        .inputMaxTokens(32_000)
                        .build());

        // Mistral AI - Mistral 7B Instruct - mistral.mistral-7b-instruct-v0:2
        String mistralMixtral7b = "mistral.mistral-7b-instruct-v0:2";
        models.put(ModelProvider.Bedrock.getName() + ":" + mistralMixtral7b,
                LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(mistralMixtral7b)
                        .displayName("Mistral AI - Mistral 7B Instruct")
                        .inputCost(0.15)
                        .outputCost(0.2)
                        .inputMaxTokens(32_000)
                        .build());
    }

    private void addGrokModels() {
        String grok3 = "grok-3";
        models.put(ModelProvider.Grok.getName() + ":" + grok3,
                LanguageModel.builder()
                        .provider(ModelProvider.Grok)
                        .modelName(grok3)
                        .displayName("Grok-3")
                        .inputCost(3.00)
                        .outputCost(15.00)
                        .inputMaxTokens(131_072)
                        .outputMaxTokens(131_072)
                        .apiKeyUsed(true)
                        .build());

        String grok3Mini = "grok-3-mini";
        models.put(ModelProvider.Grok.getName() + ":" + grok3Mini,
                LanguageModel.builder()
                        .provider(ModelProvider.Grok)
                        .modelName(grok3Mini)
                        .displayName("Grok-3-mini")
                        .inputCost(0.30)
                        .outputCost(0.50)
                        .inputMaxTokens(131_072)
                        .outputMaxTokens(131_072)
                        .apiKeyUsed(true)
                        .build());

        String grok3Fast = "grok-3-fast";
        models.put(ModelProvider.Grok.getName() + ":" + grok3Fast,
                LanguageModel.builder()
                        .provider(ModelProvider.Grok)
                        .modelName(grok3Fast)
                        .displayName("Grok-3-fast")
                        .inputCost(5.00)
                        .outputCost(25.00)
                        .inputMaxTokens(131_072)
                        .outputMaxTokens(131_072)
                        .apiKeyUsed(true)
                        .build());

        String grok3MiniFast = "grok-3-mini-fast";
        models.put(ModelProvider.Grok.getName() + ":" + grok3MiniFast,
                LanguageModel.builder()
                        .provider(ModelProvider.Grok)
                        .modelName(grok3MiniFast)
                        .displayName("Grok-3-mini-fast")
                        .inputCost(0.60)
                        .outputCost(4.00)
                        .inputMaxTokens(131_072)
                        .outputMaxTokens(131_072)
                        .apiKeyUsed(true)
                        .build());

        String grok2Vision1212 = "grok-2-vision-1212";
        models.put(ModelProvider.Grok.getName() + ":" + grok2Vision1212,
                LanguageModel.builder()
                        .provider(ModelProvider.Grok)
                        .modelName(grok2Vision1212)
                        .displayName("Grok-2-vision-1212")
                        .inputCost(2.00)
                        .outputCost(10.00)
                        .inputMaxTokens(32_768)
                        .outputMaxTokens(32_768)
                        .apiKeyUsed(true)
                        .build());

        String grok21212 = "grok-2-1212";
        models.put(ModelProvider.Grok.getName() + ":" + grok21212,
                LanguageModel.builder()
                        .provider(ModelProvider.Grok)
                        .modelName(grok21212)
                        .displayName("Grok-2-1212")
                        .inputCost(2.00)
                        .outputCost(10.00)
                        .inputMaxTokens(131_072)
                        .outputMaxTokens(131_072)
                        .apiKeyUsed(true)
                        .build());
    }

    @NotNull
    public List<LanguageModel> getModels() {

        // Create a copy of the current models
        Map<String, LanguageModel> modelsCopy = new HashMap<>(models);

        getOpenRouterModels(modelsCopy);

        return new ArrayList<>(modelsCopy.values());
    }

    private static void getOpenRouterModels(Map<String, LanguageModel> modelsCopy) {
        // Add OpenRouter models if API key exists
        OpenRouterChatModelFactory openRouterChatModelFactory = new OpenRouterChatModelFactory();
        String apiKey = openRouterChatModelFactory.getApiKey(ModelProvider.OpenRouter);
        if (apiKey != null && !apiKey.isEmpty()) {
            openRouterChatModelFactory.getModels().forEach(model ->
                modelsCopy.put(ModelProvider.OpenRouter.getName() + ":" + model.getModelName(), model));
        }
    }

    public void setModels(Map<String, LanguageModel> models) {
        this.models = new HashMap<>(models);
    }
}
