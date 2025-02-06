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
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final String MODEL_PREFIX_AMAZON = "amazon";     // Langchain4J doesn't support this yet

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private List<LanguageModel> cachedModels = null;

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
        return BedrockAnthropicMessageChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createMistralChatModel(@NotNull ChatModel chatModel) {
        return BedrockMistralAiChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createCohereChatModel(@NotNull ChatModel chatModel) {
        return BedrockCohereChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createLamaChatModel(@NotNull ChatModel chatModel) {
        return BedrockLlamaChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createAI21ChatModel(@NotNull ChatModel chatModel) {
        return BedrockAI21LabsChatModel.builder()
                .model(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .credentialsProvider(getCredentialsProvider())
                .region(getRegion())
                .build();
    }

    private ChatLanguageModel createStabilityChatModel(@NotNull ChatModel chatModel) {
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
        if (cachedModels != null) {
            return cachedModels;
        }

        List<LanguageModel> modelNames = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        List<FoundationModelSummary> models = BedrockService.getInstance().getModels();
        for (FoundationModelSummary foundationModelSummary : models) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // Currently, Langchain4J doesn't support Amazon models
                if (foundationModelSummary.providerName().equalsIgnoreCase(MODEL_PREFIX_AMAZON)) {
                    return;
                }
                LanguageModel languageModel = LanguageModel.builder()
                        .provider(ModelProvider.Bedrock)
                        .modelName(foundationModelSummary.modelId())
                        .displayName(foundationModelSummary.providerName() + " : " + foundationModelSummary.modelName())
                        .outputMaxTokens(0)
                        .inputMaxTokens(0)
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
        return modelNames;
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
