package com.devoxx.genie.chatmodel.openrouter;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.openrouter.Data;
import com.devoxx.genie.service.openrouter.OpenRouterService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenRouterChatModelFactory implements ChatModelFactory {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private List<LanguageModel> cachedModels = null;
    private static final int PRICE_SCALING_FACTOR = 1_000_000; // To convert to per million tokens

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(4_000)
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .maxTokens(4_000)
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public String getApiKey() {
        return DevoxxGenieStateService.getInstance().getOpenRouterKey().trim();
    }

    /**
     * Get the model names from the Jan service.
     *
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {
        if (cachedModels != null) {
            return cachedModels;
        }

        List<LanguageModel> modelNames = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            List<Data> models = OpenRouterService.getInstance().getModels();
            for (Data model : models) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // Convert scientific notation prices to regular decimals and scale to per million tokens
                    double inputCost = convertAndScalePrice(model.getPricing().getPrompt());
                    double outputCost = convertAndScalePrice(model.getPricing().getCompletion());

                    LanguageModel languageModel = LanguageModel.builder()
                        .provider(ModelProvider.OpenRouter)
                        .modelName(model.getId())
                        .displayName(model.getName())
                        .inputCost(inputCost)
                        .outputCost(outputCost)
                        .contextWindow(model.getContextLength() == null ? model.getTopProvider().getContextLength() : model.getContextLength())
                        .apiKeyUsed(true)
                        .build();
                    synchronized (modelNames) {
                        modelNames.add(languageModel);
                    }
                }, executorService);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            cachedModels = modelNames;
        } catch (IOException e) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "Unable to reach OpenRouter, please try again later.");
            cachedModels = List.of();
        }
        return cachedModels;
    }

    private double convertAndScalePrice(double price) {
        // Convert the price to BigDecimal for precise calculation
        BigDecimal bd = BigDecimal.valueOf(price);
        // Multiply by 1,000,000 to get price per million tokens
        bd = bd.multiply(BigDecimal.valueOf(PRICE_SCALING_FACTOR));
        // Round to 6 decimal places
        bd = bd.setScale(6, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
