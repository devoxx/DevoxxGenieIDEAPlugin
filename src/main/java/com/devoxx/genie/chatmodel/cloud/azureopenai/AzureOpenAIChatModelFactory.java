package com.devoxx.genie.chatmodel.cloud.azureopenai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

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
     * but it's not through the same endpoint and needs a different api key,
     * so we're sadly creating a single mock model for now, which is the name of deployment.
     */
    @Override
    public List<LanguageModel> getModels() {
        return List.of(LanguageModel.builder()
                .provider(MODEL_PROVIDER)
                .modelName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .displayName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .inputCost(0.0)
                .outputCost(0.0)
                .inputMaxTokens(0)
                .apiKeyUsed(true)
                .build());
    }
}
