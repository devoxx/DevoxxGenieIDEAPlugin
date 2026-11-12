package com.devoxx.genie.chatmodel.local.rapidmlx;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.rapidmlx.RapidMLXModelEntryDTO;
import com.devoxx.genie.model.rapidmlx.RapidMLXModelsResponseDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/** Lists the models served by Rapid-MLX through its OpenAI-compatible models endpoint. */
public class RapidMLXModelService implements LocalLLMProvider {

    @NotNull
    public static RapidMLXModelService getInstance() {
        return ApplicationManager.getApplication().getService(RapidMLXModelService.class);
    }

    @Override
    public List<RapidMLXModelEntryDTO> getModels() throws IOException {
        RapidMLXModelsResponseDTO response = LocalLLMProviderUtil
                .getModelsFromUrl(buildModelsUrl(DevoxxGenieStateService.getInstance().getRapidMLXModelUrl()),
                        RapidMLXModelsResponseDTO.class);

        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    /**
     * Appends {@code models} to the configured base URL, tolerating a missing trailing slash.
     *
     * @param baseUrl the configured RapidMLX base URL, e.g. {@code http://localhost:8000/v1/}
     * @return the full models endpoint, e.g. {@code http://localhost:8000/v1/models}
     */
    protected String buildModelsUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8000/v1/models";
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed + "models" : trimmed + "/models";
    }
}
