package com.devoxx.genie.chatmodel.local.rapidmlx;

import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.rapidmlx.RapidMLXModelEntryDTO;
import com.devoxx.genie.model.rapidmlx.RapidMLXModelsResponseDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RapidMLXModelServiceTest {

    private static class TestableModelService extends RapidMLXModelService {
        String testBuildModelsUrl(String baseUrl) {
            return buildModelsUrl(baseUrl);
        }
    }

    private final TestableModelService service = new TestableModelService();
    private final Gson gson = new Gson();

    @Test
    void buildModelsUrl_withTrailingSlash_appendsModels() {
        assertThat(service.testBuildModelsUrl("http://localhost:8000/v1/"))
                .isEqualTo("http://localhost:8000/v1/models");
    }

    @Test
    void buildModelsUrl_withoutTrailingSlash_insertsSeparator() {
        assertThat(service.testBuildModelsUrl("http://localhost:8000/v1"))
                .isEqualTo("http://localhost:8000/v1/models");
    }

    @Test
    void buildModelsUrl_withCustomPort_preservesHostAndPort() {
        assertThat(service.testBuildModelsUrl("http://127.0.0.1:9090/v1/"))
                .isEqualTo("http://127.0.0.1:9090/v1/models");
    }

    @Test
    void buildModelsUrl_withNullBaseUrl_returnsDefaultUrl() {
        assertThat(service.testBuildModelsUrl(null)).isEqualTo("http://localhost:8000/v1/models");
    }

    @Test
    void buildModelsUrl_withBlankBaseUrl_returnsDefaultUrl() {
        assertThat(service.testBuildModelsUrl("   ")).isEqualTo("http://localhost:8000/v1/models");
    }

    @Test
    void buildModelsUrl_withSurroundingWhitespace_trimsBeforeAppending() {
        assertThat(service.testBuildModelsUrl("  http://localhost:8000/v1/  "))
                .isEqualTo("http://localhost:8000/v1/models");
    }

    @Test
    void responseDto_parsesRapidMLXModelsPayload() {
        String json = """
                {
                  "object": "list",
                  "data": [
                    {"id": "mlx-community/Qwen3.5-4B-MLX-4bit", "object": "model", "created": 1712345678, "context_window": 262144, "owned_by": "rapid-mlx"},
                    {"id": "qwen3.5-4b-4bit", "object": "model", "created": 1712345679}
                  ]
                }
                """;

        RapidMLXModelsResponseDTO response = gson.fromJson(json, RapidMLXModelsResponseDTO.class);

        assertThat(response.getObject()).isEqualTo("list");
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getId()).isEqualTo("mlx-community/Qwen3.5-4B-MLX-4bit");
        assertThat(response.getData().get(0).getCreated()).isEqualTo(1712345678L);
        assertThat(response.getData().get(0).getContextWindow()).isEqualTo(262144);
        assertThat(response.getData().get(1).getContextWindow()).isNull();
        assertThat(response.getData().get(0).resolveDisplayName()).isEqualTo("Qwen3.5-4B-MLX-4bit");
    }

    @Test
    void responseDto_withEmptyModelList_yieldsEmptyData() {
        RapidMLXModelsResponseDTO response = gson.fromJson("{\"object\":\"list\",\"data\":[]}", RapidMLXModelsResponseDTO.class);

        assertThat(response.getData()).isEmpty();
    }

    @Test
    void getModels_usesConfiguredEndpointAndReturnsServedModels() throws Exception {
        try (MockedStatic<DevoxxGenieStateService> settings = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<LocalLLMProviderUtil> http = mockStatic(LocalLLMProviderUtil.class)) {
            DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
            settings.when(DevoxxGenieStateService::getInstance).thenReturn(state);
            when(state.getRapidMLXModelUrl()).thenReturn("http://localhost:8477/v1");
            RapidMLXModelsResponseDTO response = new RapidMLXModelsResponseDTO();
            RapidMLXModelEntryDTO entry = new RapidMLXModelEntryDTO();
            entry.setId("mlx-community/Qwen3.5-4B-MLX-4bit");
            response.setData(List.of(entry));
            http.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    "http://localhost:8477/v1/models", RapidMLXModelsResponseDTO.class)).thenReturn(response);

            assertThat(service.getModels()).containsExactly(entry);
        }
    }

    @Test
    void getModels_handlesNullResponseAndMissingData() throws Exception {
        try (MockedStatic<DevoxxGenieStateService> settings = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<LocalLLMProviderUtil> http = mockStatic(LocalLLMProviderUtil.class)) {
            DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
            settings.when(DevoxxGenieStateService::getInstance).thenReturn(state);
            when(state.getRapidMLXModelUrl()).thenReturn("http://localhost:8000/v1");
            http.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    "http://localhost:8000/v1/models", RapidMLXModelsResponseDTO.class))
                    .thenReturn(null, new RapidMLXModelsResponseDTO());

            assertThat(service.getModels()).isEmpty();
            assertThat(service.getModels()).isEmpty();
        }
    }

}
