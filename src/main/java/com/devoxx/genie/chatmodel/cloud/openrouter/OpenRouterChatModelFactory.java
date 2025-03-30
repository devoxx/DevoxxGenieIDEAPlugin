package com.devoxx.genie.chatmodel.cloud.openrouter;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.openrouter.Data;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
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

public class OpenRouterChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.OpenRouter;

    private List<LanguageModel> cachedModels = null;
    private static final int PRICE_SCALING_FACTOR = 1_000_000; // To convert to per million tokens

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(4_000)
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .listeners(getListener())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .maxTokens(4_000)
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .listeners(getListener())
            .build();
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
                        .provider(MODEL_PROVIDER)
                        .modelName(model.getId())
                        .displayName(model.getName())
                        .inputCost(inputCost)
                        .outputCost(outputCost)
                        .inputMaxTokens(model.getContextLength() == null ? model.getTopProvider().getContextLength() : model.getContextLength())
                        .apiKeyUsed(true)
                        .build();
                    synchronized (modelNames) {
                        modelNames.add(languageModel);
                    }
                }, AppExecutorUtil.getAppExecutorService());
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            cachedModels = modelNames;
        } catch (IOException e) {
            handleModelFetchError(e);
            cachedModels = List.of();
        }
        return cachedModels;
    }

    protected void handleModelFetchError(IOException e) {
        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "Unable to reach OpenRouter, please try again later.");
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
