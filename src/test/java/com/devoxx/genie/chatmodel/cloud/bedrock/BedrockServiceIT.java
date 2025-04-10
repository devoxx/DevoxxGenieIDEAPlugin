package com.devoxx.genie.chatmodel.cloud.bedrock;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServices;
import io.github.cdimascio.dotenv.Dotenv;
import kotlinx.html.B;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

/**
 * Integration tests for {@link BedrockServiceIT}.
 *
 * <p>Note: These tests require specific environment variables to be set before execution in the .env
 * at src/test/resources/.env </p>
 *
 * <ul>
 *     <li>{@code AWS_ACCESS_KEY_ID}</li>
 *     <li>{@code AWS_SECRET_ACCESS_KEY}</li>
 *     <li>{@code AWS_REGION}</li>
 * </ul>
 */
class BedrockServiceIT {

    @Test
    void test_getActiveAWSModels() {
        String secretAccessKey = Dotenv.load().get("AWS_SECRET_ACCESS_KEY");
        String accessKeyId = Dotenv.load().get("AWS_ACCESS_KEY_ID");

        assert secretAccessKey != null;
        assert accessKeyId != null;

        if (secretAccessKey.isEmpty() || accessKeyId.isEmpty()) {
            return;
        }

        // Given
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);

        try (BedrockClient bedrockClient = BedrockClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build()) {

            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request ->
                    request.byInferenceType("ON_DEMAND")
            );

            // When
            List<FoundationModelSummary> foundationModelSummaries = response.modelSummaries();

            // Then
            assertThat(foundationModelSummaries).isNotEmpty();
            assertThat(foundationModelSummaries).allMatch(model -> model.modelId() != null);

            foundationModelSummaries.stream()
                    .filter(model -> model.modelId().equals("bedrock-1.0-pro-exp-02-05"));
        }
    }

    @Test
    void test_tellAJoke() {
        String secretAccessKey = Dotenv.load().get("AWS_SECRET_ACCESS_KEY");
        String accessKeyId = Dotenv.load().get("AWS_ACCESS_KEY_ID");

        assert secretAccessKey != null;
        assert accessKeyId != null;

        if (secretAccessKey.isEmpty() || accessKeyId.isEmpty()) {
            return;
        }

        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);
        BedrockRuntimeClientBuilder bedrockRuntimeClientBuilder = BedrockRuntimeClient.builder().credentialsProvider(credentialsProvider);

        try (BedrockClient bedrockClient = BedrockClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build()) {

            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request ->
                    request.byInferenceType("ON_DEMAND")
            );

            List<FoundationModelSummary> foundationModelSummaries = response.modelSummaries();
            List<FoundationModelSummary> foundationModels = foundationModelSummaries
                    .stream()
                    .filter(foundationModelSummary ->
                            foundationModelSummary.modelName().contains("3.5")).toList();

            assert foundationModels.size() > 0;
            String modelId = foundationModels.get(0).modelId();

            System.out.println("Use model id : " + modelId);

            BedrockChatModel model = BedrockChatModel.builder()
                    .modelId(modelId)
                    .region(Region.US_EAST_1)
                    .client(bedrockRuntimeClientBuilder.build())
                    .build();

            Assistant build = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .systemMessageProvider(m -> "You are a helpful assistant")
                    .build();

            String chat = build.chat("Tell a joke");

            assert chat != null;
            assert !chat.isEmpty();
        }
    }

    @Test
    void test_ClaudeSonnet37NotAvailableForTheEU() {
        String secretAccessKey = Dotenv.load().get("AWS_SECRET_ACCESS_KEY");
        String accessKeyId = Dotenv.load().get("AWS_ACCESS_KEY_ID");

        assert secretAccessKey != null;
        assert accessKeyId != null;

        if (secretAccessKey.isEmpty() || accessKeyId.isEmpty()) {
            return;
        }

        String regionName = "eu-central-1";

        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);
        BedrockRuntimeClientBuilder bedrockRuntimeClientBuilder = BedrockRuntimeClient.builder().credentialsProvider(credentialsProvider);

        try (BedrockClient bedrockClient = BedrockClient.builder()
                .region(Region.of(regionName))
                .credentialsProvider(credentialsProvider)
                .build()) {

            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request ->
                    request.byInferenceType("ON_DEMAND")
            );

            List<FoundationModelSummary> foundationModelSummaries = response.modelSummaries();
            List<FoundationModelSummary> foundationModels = foundationModelSummaries
                    .stream()
                    .filter(foundationModelSummary ->
                            foundationModelSummary.modelName().contains("3-7")).toList();

            assert foundationModels.size() == 0;
        }
    }

    @Test
    void test_loadFoundationModelsForEURegion() {
        String secretAccessKey = Dotenv.load().get("AWS_SECRET_ACCESS_KEY");
        String accessKeyId = Dotenv.load().get("AWS_ACCESS_KEY_ID");

        assert secretAccessKey != null;
        assert accessKeyId != null;

        if (secretAccessKey.isEmpty() || accessKeyId.isEmpty()) {
            return;
        }

        String regionName = "eu-central-1";

        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);
        BedrockRuntimeClientBuilder bedrockRuntimeClientBuilder = BedrockRuntimeClient.builder().credentialsProvider(credentialsProvider);

        try (BedrockClient bedrockClient = BedrockClient.builder()
                .region(Region.of(regionName))
                .credentialsProvider(credentialsProvider)
                .build()) {

            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request ->
                    request.byInferenceType("ON_DEMAND")
            );

            List<FoundationModelSummary> foundationModelSummaries = response.modelSummaries();

            assertThat(foundationModelSummaries.size()).isPositive();
        }
    }

    interface Assistant {
        String chat(String query);
    }
}
