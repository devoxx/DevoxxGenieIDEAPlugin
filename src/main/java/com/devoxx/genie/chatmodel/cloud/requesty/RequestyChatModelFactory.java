package com.devoxx.genie.chatmodel.cloud.requesty;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ThinkingSupport;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.requesty.Data;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Requesty is an OpenAI compatible router in front of many model vendors.
 * Model ids are either a managed policy such as {@code claude-sonnet-4-5} or the
 * {@code vendor/model} form, for example {@code openai/gpt-4o-mini}.
 * See <a href="https://docs.requesty.ai">docs.requesty.ai</a>.
 */
public class RequestyChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.Requesty;

    private List<LanguageModel> cachedModels = null;
    private static final int PRICE_SCALING_FACTOR = 1_000_000; // To convert to per million tokens

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
            .baseUrl(RequestyService.BASE_URL + "/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .maxRetries(customChatModel.getMaxRetries())
            .temperature(customChatModel.getTemperature())
            .maxTokens(4_000)
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .topP(customChatModel.getTopP())
            .returnThinking(ThinkingSupport.isEnabled())
            .listeners(getListener())
            .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl(RequestyService.BASE_URL + "/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .maxTokens(4_000)
            .temperature(customChatModel.getTemperature())
            .topP(customChatModel.getTopP())
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .returnThinking(ThinkingSupport.isEnabled())
            .listeners(getListener())
            .build();
    }

    /**
     * Get the chat models from the Requesty catalog.
     *
     * @return List of language models
     */
    @Override
    public List<LanguageModel> getModels() {
        if (cachedModels != null) {
            return cachedModels;
        }

        List<LanguageModel> modelNames = new ArrayList<>();

        try {
            List<Data> models = RequestyService.getInstance().getModels(getApiKey(MODEL_PROVIDER));
            for (Data model : models) {
                if (model.getId() == null || !model.isChatModel()) {
                    continue;
                }

                // Requesty prices are USD per token, scale to per million tokens
                double inputCost = convertAndScalePrice(model.getInputPrice());
                double outputCost = convertAndScalePrice(model.getOutputPrice());

                LanguageModel languageModel = LanguageModel.builder()
                    .provider(MODEL_PROVIDER)
                    .modelName(model.getId())
                    .displayName(model.getId())
                    .inputCost(inputCost)
                    .outputCost(outputCost)
                    .inputMaxTokens(model.getContextWindow() == null ? 0 : model.getContextWindow())
                    .apiKeyUsed(true)
                    .build();
                modelNames.add(languageModel);
            }
            cachedModels = modelNames;
        } catch (IOException e) {
            handleModelFetchError(e);
            cachedModels = List.of();
        }
        return cachedModels;
    }

    @Override
    public void resetModels() {
        cachedModels = null;
    }

    protected void handleModelFetchError(IOException e) {
        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "Unable to reach Requesty, please try again later.");
    }

    private double convertAndScalePrice(Double price) {
        if (price == null) {
            return 0.0;
        }
        BigDecimal bd = BigDecimal.valueOf(price);
        bd = bd.multiply(BigDecimal.valueOf(PRICE_SCALING_FACTOR));
        bd = bd.setScale(6, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
