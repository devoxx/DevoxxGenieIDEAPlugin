package com.devoxx.genie.chatmodel.cloud.azureopenai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openai.OpenAIChatModelName;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public class AzureOpenAIChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.AzureOpenAI;;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        boolean isO1 = chatModel.getModelName().startsWith("o1-");

        final var builder = AzureOpenAiChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .deploymentName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .maxRetries(chatModel.getMaxRetries())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .topP(isO1 ? 1.0 : chatModel.getTopP())
                .endpoint(DevoxxGenieStateService.getInstance().getAzureOpenAIEndpoint())
                .listeners(getListener());

        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        boolean isO1 = chatModel.getModelName().startsWith("o1-");

        final var builder = AzureOpenAiStreamingChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .deploymentName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .topP(isO1 ? 1.0 : chatModel.getTopP())
                .endpoint(DevoxxGenieStateService.getInstance().getAzureOpenAIEndpoint())
                .listeners(getListener());

        return builder.build();
    }

    /**
     * Using the Azure OpenAI provider, models are wrapped in a deployment.
     * There is an API available with which you can list your deployments and info about them,
     * but it's not through the same endpoint and needs a different api key.
     * We fall back to matching a deployed model with known models from OpenAI in general based on its name.
     * When there is a match, the settings are taken from those models from the LLMModelRegistryService.
     * In other cases, we're sadly creating a single mock model for now, which is the name of deployment.
     */
    @Override
    public List<LanguageModel> getModels() {
        return List.of(LanguageModel.builder()
                .provider(MODEL_PROVIDER)
                .modelName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .displayName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .inputCost(getAzureOpenAIModelConfig(LanguageModel::getInputCost, 0.0))
                .outputCost(getAzureOpenAIModelConfig(LanguageModel::getOutputCost, 0.0))
                .inputMaxTokens(getAzureOpenAIModelConfig(LanguageModel::getInputMaxTokens, 0))
                .apiKeyUsed(true)
                .build());
    }

    private <N extends Number> N getAzureOpenAIModelConfig(Function<LanguageModel, N> getConfigFunction, N defaultValue) {
        return LLMModelRegistryService.getInstance().getModels().stream()
                .filter(model -> model.getModelName().equals(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment()))
                .map(getConfigFunction).findFirst().orElse(defaultValue);
    }
}
