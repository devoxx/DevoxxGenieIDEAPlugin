package com.devoxx.genie.chatmodel.cloud.bedrock;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class BedrockServiceIT {

    @Test
    public void test_getActiveAWSModels() {

        Dotenv dotenv = Dotenv.load();
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_REGION");

        if (accessKeyId == null || secretAccessKey == null || region == null) {
            throw new IllegalArgumentException("AWS credentials not found in .env file");
        }

        // Given
        StaticCredentialsProvider credentialsProvider =
                StaticCredentialsProvider.create(AwsBasicCredentials.create(secretAccessKey, accessKeyId));

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

//            foundationModelSummaries.stream()
//                    .filter(model -> model.modelId().equals("bedrock-1.0-pro-exp-02-05"))
        }
    }
}
