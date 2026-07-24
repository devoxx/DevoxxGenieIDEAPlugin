package com.devoxx.genie.chatmodel.local.nativ;

import com.devoxx.genie.model.nativ.NativModelsResponseDTO;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NativModelServiceTest {

    private static class TestableModelService extends NativModelService {
        String testBuildModelsUrl(String baseUrl) {
            return buildModelsUrl(baseUrl);
        }
    }

    private final TestableModelService service = new TestableModelService();
    private final Gson gson = new Gson();

    @Test
    void buildModelsUrl_withTrailingSlash_appendsModels() {
        assertThat(service.testBuildModelsUrl("http://localhost:8080/v1/"))
                .isEqualTo("http://localhost:8080/v1/models");
    }

    @Test
    void buildModelsUrl_withoutTrailingSlash_insertsSeparator() {
        assertThat(service.testBuildModelsUrl("http://localhost:8080/v1"))
                .isEqualTo("http://localhost:8080/v1/models");
    }

    @Test
    void buildModelsUrl_withCustomPort_preservesHostAndPort() {
        assertThat(service.testBuildModelsUrl("http://127.0.0.1:9090/v1/"))
                .isEqualTo("http://127.0.0.1:9090/v1/models");
    }

    @Test
    void buildModelsUrl_withNullBaseUrl_returnsDefaultUrl() {
        assertThat(service.testBuildModelsUrl(null)).isEqualTo("http://localhost:8080/v1/models");
    }

    @Test
    void buildModelsUrl_withBlankBaseUrl_returnsDefaultUrl() {
        assertThat(service.testBuildModelsUrl("   ")).isEqualTo("http://localhost:8080/v1/models");
    }

    @Test
    void buildModelsUrl_withSurroundingWhitespace_trimsBeforeAppending() {
        assertThat(service.testBuildModelsUrl("  http://localhost:8080/v1/  "))
                .isEqualTo("http://localhost:8080/v1/models");
    }

    /**
     * Guards the Gson binding against Nativ's actual {@code /v1/models} payload shape
     * (as served by the MLX-VLM backend).
     */
    @Test
    void responseDto_parsesNativModelsPayload() {
        String json = """
                {
                  "object": "list",
                  "data": [
                    {"id": "mlx-community/Qwen2.5-Coder-7B-Instruct-4bit", "object": "model", "created": 1712345678},
                    {"id": "mlx-community/gemma-3-4b-it-4bit", "object": "model", "created": 1712345679}
                  ]
                }
                """;

        NativModelsResponseDTO response = gson.fromJson(json, NativModelsResponseDTO.class);

        assertThat(response.getObject()).isEqualTo("list");
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getId()).isEqualTo("mlx-community/Qwen2.5-Coder-7B-Instruct-4bit");
        assertThat(response.getData().get(0).getCreated()).isEqualTo(1712345678L);
        assertThat(response.getData().get(0).resolveDisplayName()).isEqualTo("Qwen2.5-Coder-7B-Instruct-4bit");
    }

    @Test
    void responseDto_withEmptyModelList_yieldsEmptyData() {
        NativModelsResponseDTO response = gson.fromJson("{\"object\":\"list\",\"data\":[]}", NativModelsResponseDTO.class);

        assertThat(response.getData()).isEmpty();
    }
}
