package com.devoxx.genie.chatmodel.local.lmstudio;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

public class LMStudioModelService implements LocalLLMProvider {

    @NotNull
    public static LMStudioModelService getInstance() {
        return ApplicationManager.getApplication().getService(LMStudioModelService.class);
    }

    @Override
    public LMStudioModelEntryDTO[] getModels() throws IOException {
        // LMStudio has separate API endpoints:
        // - /api/v1/models : Rich metadata (max_context_length, display_name, loaded_instances)
        // - /api/v1/chat   : Native LM Studio chat endpoint (recommended in LM Studio 0.4.0+)
        // - /v1/models     : OpenAI-compatible model list (basic metadata only)
        // - /v1/chat/completions : OpenAI-compatible chat (used by Langchain4J)
        //
        // The chat base URL is set to /v1/ for Langchain4J's OpenAI-compatible client,
        // but we always use /api/v1/models for model listing to get rich metadata.

        String baseUrl = DevoxxGenieStateService.getInstance().getLmstudioModelUrl();
        String modelsUrl = buildModelsUrl(baseUrl);

        return LocalLLMProviderUtil
                .getModelsFromUrl(modelsUrl, LMStudioModelEntryDTO[].class);
    }

    /**
     * Builds the full URL for the LM Studio rich models endpoint.
     * Always targets /api/v1/models regardless of the configured chat base URL path.
     *
     * @param baseUrl The configured LMStudio base URL (e.g., http://localhost:1234/v1/)
     * @return The full models endpoint URL (e.g., http://localhost:1234/api/v1/models)
     */
    protected String buildModelsUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:1234/api/v1/models";
        }

        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String authority = uri.getAuthority();
            if (authority == null) {
                return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/v1/models";
            }
            return scheme + "://" + authority + "/api/v1/models";
        } catch (IllegalArgumentException e) {
            // Fallback: strip path and append /api/v1/models
            return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/v1/models";
        }
    }
}
