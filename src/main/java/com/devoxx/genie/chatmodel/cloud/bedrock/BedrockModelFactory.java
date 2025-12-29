package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.util.List;

/**
 * This factory supports creating models from different providers available on AWS Bedrock.
 */
public class BedrockModelFactory implements ChatModelFactory {
    private static final String MODEL_PREFIX_ANTHROPIC = "anthropic";
    private static final String MODEL_PREFIX_MISTRAL = "mistral";
    private static final String MODEL_PREFIX_META = "meta";
    private static final String MODEL_PREFIX_COHERE = "cohere";
    private static final String MODEL_PREFIX_AI21 = "ai21";
    private static final String MODEL_PREFIX_STABILITY = "stability";

    // Langchain4J doesn't support this yet
    private static final String MODEL_PREFIX_AMAZON = "amazon";

    /**
     * Creates a {@link ChatModel} based on the provided {@link CustomChatModel}.
     *
     * @param customChatModel The configuration for the chat model.
     * @return An instance of {@link ChatModel} configured with the provided settings.
     * @throws NotImplementedException if the requested model is not supported.
     */
    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        final String modelName = customChatModel.getModelName().toLowerCase();

        if (modelName.contains(MODEL_PREFIX_ANTHROPIC)) {
            return createAnthropicChatModel(customChatModel);
        } else if (modelName.contains(MODEL_PREFIX_MISTRAL)) {
            return createMistralChatModel(customChatModel);
        } else if (modelName.contains(MODEL_PREFIX_COHERE)) {
            return createCohereChatModel(customChatModel);
        } else if (modelName.contains(MODEL_PREFIX_META)) {
            return createLamaChatModel(customChatModel);
        } else if (modelName.contains(MODEL_PREFIX_AI21)) {
            return createAI21ChatModel(customChatModel);
        } else if (modelName.contains(MODEL_PREFIX_STABILITY)) {
            return createStabilityChatModel(customChatModel);
        } else {
            throw new NotImplementedException(modelName + " not yet supported.");
        }
    }

    /**
     * Creates a {@link StreamingChatModel} based on the provided {@link CustomChatModel}.
     *
     * @param customChatModel The configuration for the chat model.
     * @return  An instance of {@link StreamingChatModel} configured with the provided settings.
     * @throws NotImplementedException if the requested model is not supported.
     */
    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        final String modelName = customChatModel.getModelName().toLowerCase();

        if (modelName.contains(MODEL_PREFIX_ANTHROPIC)) {
            return createAnthropicStreamingChatModel(customChatModel);
        } else {
            return null;
        }
    }

    /**
     * Creates a {@link ChatModel} for Anthropic models on AWS Bedrock.
     *
     * @param customChatModel The configuration for the chat model.
     * @return An instance of {@link ChatModel} configured for Anthropic models.
     */
    private ChatModel createAnthropicChatModel(@NotNull CustomChatModel customChatModel) {
        return BedrockChatModel.builder()
                .modelId(getModelId(customChatModel.getModelName()))
                .client(BedrockRuntimeClient.builder()
                        .region(getRegion())
                        .credentialsProvider(getCredentialsProvider())
                        .build())
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .temperature(customChatModel.getTemperature())
                        .maxOutputTokens(customChatModel.getMaxTokens())
                        .build())
                .build();
    }

    /**
     * Creates a {@link StreamingChatModel} for Anthropic models on AWS Bedrock.
     * All Bedrock Anthropic models currently support streaming responses.
     *
     * @param customChatModel The configuration for the chat model.
     * @return An instance of {@link StreamingChatModel} configured for Anthropic models.
     */
    private BedrockStreamingChatModel createAnthropicStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return BedrockStreamingChatModel.builder()
                .modelId(getModelId(customChatModel.getModelName()))
                .client(BedrockRuntimeAsyncClient.builder()
                        .region(getRegion())
                        .credentialsProvider(getCredentialsProvider())
                        .build())
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .temperature(customChatModel.getTemperature())
                        .maxOutputTokens(customChatModel.getMaxTokens())
                        .build())
                .build();
    }

    private ChatModel createMistralChatModel(@NotNull CustomChatModel customChatModel) {
        return BedrockChatModel.builder()
                .modelId(customChatModel.getModelName())
                .client(BedrockRuntimeClient.builder()
                        .region(getRegion())
                        .credentialsProvider(getCredentialsProvider())
                        .build())
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .temperature(customChatModel.getTemperature())
                        .maxOutputTokens(customChatModel.getMaxTokens())
                        .build())
                .build();
    }

    private ChatModel createCohereChatModel(@NotNull CustomChatModel customChatModel) {
        return BedrockChatModel.builder()
                .modelId(customChatModel.getModelName())
                .client(BedrockRuntimeClient.builder()
                        .region(getRegion())
                        .credentialsProvider(getCredentialsProvider())
                        .build())
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .temperature(customChatModel.getTemperature())
                        .maxOutputTokens(customChatModel.getMaxTokens())
                        .build())
                .build();
    }

    private ChatModel createLamaChatModel(@NotNull CustomChatModel customChatModel) {
        return BedrockChatModel.builder()
                .modelId(getModelId(customChatModel.getModelName()))
                .client(BedrockRuntimeClient.builder()
                        .region(getRegion())
                        .credentialsProvider(getCredentialsProvider())
                        .build())
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .temperature(customChatModel.getTemperature())
                        .maxOutputTokens(customChatModel.getMaxTokens())
                        .build())
                .build();
    }

    private ChatModel createAI21ChatModel(@NotNull CustomChatModel customChatModel) {
        return BedrockChatModel.builder()
                .modelId(customChatModel.getModelName())
                .client(BedrockRuntimeClient.builder()
                        .region(getRegion())
                        .credentialsProvider(getCredentialsProvider())
                        .build())
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .temperature(customChatModel.getTemperature())
                        .maxOutputTokens(customChatModel.getMaxTokens())
                        .build())
                .build();
    }

    private ChatModel createStabilityChatModel(@NotNull CustomChatModel customChatModel) {
        return BedrockChatModel.builder()
                .modelId(customChatModel.getModelName())
                .client(BedrockRuntimeClient.builder()
                        .region(getRegion())
                        .credentialsProvider(getCredentialsProvider())
                        .build())
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .temperature(customChatModel.getTemperature())
                        .maxOutputTokens(customChatModel.getMaxTokens())
                        .build())
                .build();
    }

    /**
     * Returns a list of supported language models for the Bedrock provider.
     * <p>
     * Full list of models
     * <a href="https://docs.aws.amazon.com/bedrock/latest/userguide/models-supported.html">can be found at</a>
     *
     * @return A list of {@link LanguageModel} for Bedrock.
     */
    @Override
    public List<LanguageModel> getModels() {
        return getModels(ModelProvider.Bedrock);
    }

    /**
     * Creates an {@link AwsCredentialsProvider} using the configured AWS access key and secret key
     * from the {@link DevoxxGenieStateService}.
     *
     * @return An {@link AwsCredentialsProvider} for authenticating with AWS.
     */
    public @NotNull AwsCredentialsProvider getCredentialsProvider() {
        String accessKeyId = DevoxxGenieStateService.getInstance().getAwsAccessKeyId();
        String secretKey = DevoxxGenieStateService.getInstance().getAwsSecretKey();

        boolean shouldPowerFromProfile = DevoxxGenieStateService.getInstance().getShouldPowerFromAWSProfile();
        String profileName = DevoxxGenieStateService.getInstance().getAwsProfileName();

        return shouldPowerFromProfile ? ProfileCredentialsProvider.create(profileName) :
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKey));
    }

    /**
     * Retrieves the AWS region from the {@link DevoxxGenieStateService}.
     *
     * @return The AWS {@link Region}.
     */
    public Region getRegion() {
        return Region.of(
                DevoxxGenieStateService.getInstance().getAwsRegion()
        );
    }

    /**
     * Get the model name with prefix us, eu or apac
     * @param modelName the base model name
     * @return update model name
     */
    private @NotNull String getModelId(String modelName) {
        Region userProvidedRegion = getRegion();
        String strRegion = userProvidedRegion.toString().toLowerCase();
        String strPrefix = "";

        if (DevoxxGenieStateService.getInstance().getShouldEnableAWSRegionalInference()) {
            if (strRegion.startsWith("eu")) {
                strPrefix = "eu.";
            } else if (strRegion.startsWith("ap")) {
                strPrefix = "apac.";
            } else if (strRegion.startsWith("us")) {
                strPrefix = "us.";
            }
        }

        return strPrefix + modelName;
    }
}
