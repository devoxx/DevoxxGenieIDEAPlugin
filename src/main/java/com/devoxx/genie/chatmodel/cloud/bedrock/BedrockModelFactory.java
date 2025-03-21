package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.bedrock.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
     * Creates a {@link ChatLanguageModel} based on the provided {@link ChatModel}.
     *
     * @param chatModel The configuration for the chat model.
     * @return An instance of {@link ChatLanguageModel} configured with the provided settings.
     * @throws NotImplementedException if the requested model is not supported.
     */
    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        final String modelName = chatModel.getModelName().toLowerCase();

        if (modelName.startsWith(MODEL_PREFIX_ANTHROPIC)) {
            return createAnthropicChatModel(chatModel);
        } else if (modelName.startsWith(MODEL_PREFIX_MISTRAL)) {
            return createMistralChatModel(chatModel);
        } else if (modelName.startsWith(MODEL_PREFIX_COHERE)) {
            return createCohereChatModel(chatModel);
        } else if (modelName.startsWith(MODEL_PREFIX_META)) {
            return createLamaChatModel(chatModel);
        } else if (modelName.startsWith(MODEL_PREFIX_AI21)) {
            return createAI21ChatModel(chatModel);
        } else if (modelName.startsWith(MODEL_PREFIX_STABILITY)) {
            return createStabilityChatModel(chatModel);
        } else {
            throw new NotImplementedException(modelName + " not yet supported.");
        }
    }

    /**
     * Creates a {@link ChatLanguageModel} for Anthropic models on AWS Bedrock.
     *
     * @param chatModel The configuration for the chat model.
     * @return An instance of {@link ChatLanguageModel} configured for Anthropic models.
     */
    private ChatLanguageModel createAnthropicChatModel(@NotNull ChatModel chatModel) {
        // TODO Refactor the deprecated class and use the new one
        return BedrockAnthropicMessageChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createMistralChatModel(@NotNull ChatModel chatModel) {
        // TODO Refactor the deprecated class and use the new one
        return BedrockMistralAiChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createCohereChatModel(@NotNull ChatModel chatModel) {
        // TODO Refactor the deprecated class and use the new one
        return BedrockCohereChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createLamaChatModel(@NotNull ChatModel chatModel) {
        // TODO Refactor the deprecated class and use the new one
        return BedrockLlamaChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createAI21ChatModel(@NotNull ChatModel chatModel) {
        // TODO Refactor the deprecated class and use the new one
        return BedrockAI21LabsChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createStabilityChatModel(@NotNull ChatModel chatModel) {
        // TODO Refactor the deprecated class and use the new one
        return BedrockStabilityAIChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
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
    AwsCredentialsProvider getCredentialsProvider() {
        String accessKeyId = DevoxxGenieStateService.getInstance().getAwsAccessKeyId();
        String secretKey = DevoxxGenieStateService.getInstance().getAwsSecretKey();

        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKey));
    }

    /**
     * Retrieves the AWS region from the {@link DevoxxGenieStateService}.
     *
     * @return The AWS {@link Region}.
     */
    Region getRegion() {
        return Region.of(
                DevoxxGenieStateService.getInstance().getAwsRegion()
        );
    }
}
