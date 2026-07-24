package com.devoxx.genie.chatmodel.local.nativ;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.nativ.NativModelEntryDTO;
import com.devoxx.genie.model.nativ.NativModelsResponseDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Lists the MLX models Nativ has available locally.
 * <p>
 * Nativ exposes a plain OpenAI-compatible {@code /v1/models} endpoint, so the configured chat
 * base URL ({@code http://localhost:8080/v1/} by default) already points at the right place and
 * only the {@code models} segment has to be appended — unlike LMStudio, which serves its rich
 * metadata from a separate {@code /api/v1/models} path.
 */
public class NativModelService implements LocalLLMProvider {

    @NotNull
    public static NativModelService getInstance() {
        return ApplicationManager.getApplication().getService(NativModelService.class);
    }

    @Override
    public List<NativModelEntryDTO> getModels() throws IOException {
        NativModelsResponseDTO response = LocalLLMProviderUtil
                .getModelsFromUrl(buildModelsUrl(DevoxxGenieStateService.getInstance().getNativModelUrl()),
                        NativModelsResponseDTO.class);

        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    /**
     * Appends {@code models} to the configured base URL, tolerating a missing trailing slash.
     *
     * @param baseUrl the configured Nativ base URL, e.g. {@code http://localhost:8080/v1/}
     * @return the full models endpoint, e.g. {@code http://localhost:8080/v1/models}
     */
    protected String buildModelsUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080/v1/models";
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed + "models" : trimmed + "/models";
    }
}
