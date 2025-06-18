package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
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
//        String awsAccessKey = instance.getAwsAccessKeyId();
//        String awsSecretKey = instance.getAwsSecretKey();
//        String awsSessionToken = instance.getAwsSessionToken();
        String profileName = instance.getAwsProfileName();
        try (BedrockClient bedrockClient = BedrockClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(ProfileCredentialsProvider.create(profileName)) //StaticCredentialsProvider.create(AwsSessionCredentials.create(awsAccessKey, awsSecretKey, awsSessionToken))
                .build()) {

            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request ->
                    request.byInferenceType("ON_DEMAND")
            );

            return response.modelSummaries();
        }
    }
}
