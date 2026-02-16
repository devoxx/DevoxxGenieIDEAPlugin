package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

import java.util.List;

public class BedrockService {

    @NotNull
    public static BedrockService getInstance() {
        return ApplicationManager.getApplication().getService(BedrockService.class);
    }

    public @NotNull List<FoundationModelSummary> getModels() {
        DevoxxGenieStateService instance = DevoxxGenieStateService.getInstance();

        String awsRegion = instance.getAwsRegion();
        String awsAccessKey = instance.getAwsAccessKeyId();
        String awsSecretKey = instance.getAwsSecretKey();

        boolean shouldPowerFromProfile = instance.getShouldPowerFromAWSProfile();
        String profileName = instance.getAwsProfileName();

        AwsCredentialsProvider credentialsProvider = shouldPowerFromProfile
                ? ProfileCredentialsProvider.create(profileName) : StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKey, awsSecretKey));

        try (BedrockClient bedrockClient = BedrockClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build()) {

            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request ->
                    request.byInferenceType("ON_DEMAND")
            );

            return response.modelSummaries();
        }
    }
}
