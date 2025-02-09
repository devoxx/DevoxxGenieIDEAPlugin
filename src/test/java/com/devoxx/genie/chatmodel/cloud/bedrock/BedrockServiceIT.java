package com.devoxx.genie.chatmodel.cloud.bedrock;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AWS_SECRET_ACCESS_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AWS_REGION", matches = ".+")
class BedrockServiceIT {

    @Test
    void test_getActiveAWSModels() {
        // Given
        StaticCredentialsProvider credentialsProvider =
                StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        System.getenv("AWS_SECRET_ACCESS_KEY"),
                        System.getenv("AWS_ACCESS_KEY_ID")));

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
