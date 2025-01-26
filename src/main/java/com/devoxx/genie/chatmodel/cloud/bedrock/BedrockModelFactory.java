package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.commons.lang3.NotImplementedException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.util.List;

/**
 * This factory supports creating models from different providers available on AWS Bedrock.
 */
public class BedrockModelFactory implements ChatModelFactory {
    private static final String MODEL_PREFIX_ANTHROPIC = "anthropic.";

    private final ModelProvider MODEL_PROVIDER = ModelProvider.Bedrock;

    /**
     * Creates a {@link ChatLanguageModel} based on the provided {@link ChatModel}.
     *
     * @param chatModel The configuration for the chat model.
     * @return An instance of {@link ChatLanguageModel} configured with the provided settings.
     * @throws NotImplementedException if the requested model is not supported.
     */
    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        final String modelName = chatModel.getModelName();

        if (modelName.startsWith(MODEL_PREFIX_ANTHROPIC)) {
            return createAnthropicChatModel(chatModel);
        } else {
            throw new NotImplementedException("Support for model %s isn't implemented yet", modelName);
        }
    }

    /**
     * Creates a {@link ChatLanguageModel} for Anthropic models on AWS Bedrock.
     *
     * @param chatModel The configuration for the chat model.
     * @return An instance of {@link ChatLanguageModel} configured for Anthropic models.
     */
    private ChatLanguageModel createAnthropicChatModel(ChatModel chatModel) {
        return BedrockAnthropicMessageChatModel.builder()
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
        return getModels(MODEL_PROVIDER);
    }

    /**
     * Creates an {@link AwsCredentialsProvider} using the configured AWS access key and secret key
     * from the {@link DevoxxGenieStateService}.
     *
     * @return An {@link AwsCredentialsProvider} for authenticating with AWS.
     */
    AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                DevoxxGenieStateService.getInstance().getAwsAccessKeyId(),
                DevoxxGenieStateService.getInstance().getAwsAccessKey()
        ));
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
