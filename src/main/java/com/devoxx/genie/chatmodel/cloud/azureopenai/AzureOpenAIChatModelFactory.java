package com.devoxx.genie.chatmodel.cloud.azureopenai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public class AzureOpenAIChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.AzureOpenAI;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        String modelName = customChatModel.getModelName();

        boolean isReasoningModel = isReasoningModelWithLimitedParameters(modelName);

        final var builder = AzureOpenAiChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .deploymentName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(isReasoningModel ? 1.0 : customChatModel.getTemperature())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(isReasoningModel ? 1.0 : customChatModel.getTopP())
                .endpoint(DevoxxGenieStateService.getInstance().getAzureOpenAIEndpoint())
                .listeners(getListener(customChatModel.getProject()));

        return builder.build();
    }

    /**
     * Returns whether the model is a reasoning model with limited parameter support, in order to provide default
     * values instead of given configuration.
     * <p>
     * @see <a href="https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/reasoning?tabs=python-secure%2Cpy#not-supported">Azure OpenAI reasoning models - parameters not supported</a>
     * for details on parameter support for reasoning models.
     *
     * @param modelName name of the model to check
     * @return true if the model name indicates a reasoning model
     */
    static boolean isReasoningModelWithLimitedParameters(String modelName) {
        boolean isO1 = modelName.startsWith("o1");
        boolean isO3 = modelName.startsWith("o3");
        boolean isO4 = modelName.startsWith("o4-mini");
        boolean isCodex = modelName.equalsIgnoreCase("codex-mini");

        return isO1 || isO3 || isO4 || isCodex;
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        boolean isO1 = customChatModel.getModelName().startsWith("o1-");

        final var builder = AzureOpenAiStreamingChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .deploymentName(DevoxxGenieStateService.getInstance().getAzureOpenAIDeployment())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(isO1 ? 1.0 : customChatModel.getTopP())
                .endpoint(DevoxxGenieStateService.getInstance().getAzureOpenAIEndpoint())
                .listeners(getListener(customChatModel.getProject()));

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
