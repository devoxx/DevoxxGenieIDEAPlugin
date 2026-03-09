package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

import java.util.List;

public class BedrockService {

    private final BedrockAuthResolver authResolver = new BedrockAuthResolver();

    @NotNull
    public static BedrockService getInstance() {
        return ApplicationManager.getApplication().getService(BedrockService.class);
    }

    public @NotNull List<FoundationModelSummary> getModels() {
        try (BedrockClient bedrockClient = authResolver.configure(BedrockClient.builder()).build()) {

            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request ->
                    request.byInferenceType("ON_DEMAND")
            );

            return response.modelSummaries();
        }
    }
}
