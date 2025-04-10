package com.devoxx.genie.chatmodel.cloud.bedrock;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;

import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.service.AiServices;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
@EnabledIfEnvironmentVariable(named = "AWS_SECRET_ACCESS_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".+")
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
            assertThat(foundationModelSummaries.size()).isPositive();
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

            assertThat(foundationModels.size()).isPositive();
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

            assertThat(foundationModels.size()).isZero();
        }
    }

    @Test
    void test_ClaudeSonnet37InUsRegion() {
        String secretAccessKey = Dotenv.load().get("AWS_SECRET_ACCESS_KEY");
        String accessKeyId = Dotenv.load().get("AWS_ACCESS_KEY_ID");

        assert secretAccessKey != null;
        assert accessKeyId != null;

        if (secretAccessKey.isEmpty() || accessKeyId.isEmpty()) {
            return;
        }

        ChatLanguageModel model = BedrockChatModel.builder()
                .modelId("us.anthropic.claude-3-7-sonnet-20250219-v1:0")
                .maxRetries(1)
        .region(Region.US_EAST_1)
        .logRequests(true)
        .logResponses(true)
        .defaultRequestParameters(ChatRequestParameters.builder()
                .topP(1.0)
                .temperature(1.0)
                .maxOutputTokens(2000)
                .build())
        .build();

        String joke = model.chat("Tell me a joke about Java");
        assertThat(joke).isNotNull();
    }

    @Test
    void test_ClaudeSonnet37InEURegion() {
        String secretAccessKey = Dotenv.load().get("AWS_SECRET_ACCESS_KEY");
        String accessKeyId = Dotenv.load().get("AWS_ACCESS_KEY_ID");

        assert secretAccessKey != null;
        assert accessKeyId != null;

        if (secretAccessKey.isEmpty() || accessKeyId.isEmpty()) {
            return;
        }

        ChatLanguageModel model = BedrockChatModel.builder()
                .modelId("eu.anthropic.claude-3-7-sonnet-20250219-v1:0")
                .maxRetries(1)
                .region(Region.EU_CENTRAL_1)
                .logRequests(true)
                .logResponses(true)
                .defaultRequestParameters(ChatRequestParameters.builder()
                        .topP(1.0)
                        .temperature(1.0)
                        .maxOutputTokens(2000)
                        .build())
                .build();

        String joke = model.chat("Tell me a joke about Java");
        assertThat(joke).isNotNull();
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
