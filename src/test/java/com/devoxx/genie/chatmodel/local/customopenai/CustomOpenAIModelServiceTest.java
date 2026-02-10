package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.model.customopenai.CustomOpenAIModelEntryDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOpenAIModelServiceTest {

    private static class TestableModelService extends CustomOpenAIModelService {
        public String testBuildModelsUrl(String baseUrl) {
            return buildModelsUrl(baseUrl);
        }

        public CustomOpenAIModelEntryDTO[] testParseModelsResponse(String json) {
            return parseModelsResponse(json);
        }
    }

    // --- URL Building Tests ---

    @Test
    void buildModelsUrl_withV1TrailingSlash_appendsModels() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:3000/v1/");
        assertThat(url).isEqualTo("http://localhost:3000/v1/models");
    }

    @Test
    void buildModelsUrl_withV1NoTrailingSlash_appendsModels() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:3000/v1");
        assertThat(url).isEqualTo("http://localhost:3000/v1/models");
    }

    @Test
    void buildModelsUrl_withRootPath_appendsModels() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:3000/");
        assertThat(url).isEqualTo("http://localhost:3000/models");
    }

    @Test
    void buildModelsUrl_withNoPath_appendsModels() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:3000");
        assertThat(url).isEqualTo("http://localhost:3000/models");
    }

    @Test
    void buildModelsUrl_withCustomHost_preservesHost() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://docker-tools:3000/v1/");
        assertThat(url).isEqualTo("http://docker-tools:3000/v1/models");
    }

    @Test
    void buildModelsUrl_withHttps_preservesScheme() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("https://api.example.com/v1/");
        assertThat(url).isEqualTo("https://api.example.com/v1/models");
    }

    @Test
    void buildModelsUrl_withNull_returnsEmpty() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl(null);
        assertThat(url).isEmpty();
    }

    @Test
    void buildModelsUrl_withBlank_returnsEmpty() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("  ");
        assertThat(url).isEmpty();
    }

    // --- JSON Parsing Tests ---

    @Test
    void parseModelsResponse_standardOpenAIFormat() {
        TestableModelService service = new TestableModelService();
        String json = """
                {
                  "object": "list",
                  "data": [
                    {"id": "gpt-4", "object": "model", "owned_by": "openai"},
                    {"id": "gpt-3.5-turbo", "object": "model", "owned_by": "openai"}
                  ]
                }
                """;

        CustomOpenAIModelEntryDTO[] models = service.testParseModelsResponse(json);

        assertThat(models).hasSize(2);
        assertThat(models[0].getId()).isEqualTo("gpt-4");
        assertThat(models[1].getId()).isEqualTo("gpt-3.5-turbo");
    }

    @Test
    void parseModelsResponse_modelsKeyFormat() {
        TestableModelService service = new TestableModelService();
        String json = """
                {
                  "models": [
                    {"id": "devstral:24b", "object": "model"},
                    {"id": "gemma3n:e4b", "object": "model"}
                  ]
                }
                """;

        CustomOpenAIModelEntryDTO[] models = service.testParseModelsResponse(json);

        assertThat(models).hasSize(2);
        assertThat(models[0].getId()).isEqualTo("devstral:24b");
        assertThat(models[1].getId()).isEqualTo("gemma3n:e4b");
    }

    @Test
    void parseModelsResponse_plainArrayFormat() {
        TestableModelService service = new TestableModelService();
        String json = """
                [
                  {"id": "model-a", "object": "model"},
                  {"id": "model-b", "object": "model"}
                ]
                """;

        CustomOpenAIModelEntryDTO[] models = service.testParseModelsResponse(json);

        assertThat(models).hasSize(2);
        assertThat(models[0].getId()).isEqualTo("model-a");
        assertThat(models[1].getId()).isEqualTo("model-b");
    }

    @Test
    void parseModelsResponse_emptyDataArray() {
        TestableModelService service = new TestableModelService();
        String json = """
                {
                  "object": "list",
                  "data": []
                }
                """;

        CustomOpenAIModelEntryDTO[] models = service.testParseModelsResponse(json);

        assertThat(models).isEmpty();
    }

    @Test
    void parseModelsResponse_unknownFormat_returnsEmpty() {
        TestableModelService service = new TestableModelService();
        String json = """
                {
                  "error": "not found"
                }
                """;

        CustomOpenAIModelEntryDTO[] models = service.testParseModelsResponse(json);

        assertThat(models).isEmpty();
    }

    @Test
    void parseModelsResponse_preservesOwnedBy() {
        TestableModelService service = new TestableModelService();
        String json = """
                {
                  "data": [
                    {"id": "my-model", "object": "model", "owned_by": "custom-org"}
                  ]
                }
                """;

        CustomOpenAIModelEntryDTO[] models = service.testParseModelsResponse(json);

        assertThat(models).hasSize(1);
        assertThat(models[0].getId()).isEqualTo("my-model");
        assertThat(models[0].getOwned_by()).isEqualTo("custom-org");
    }
}
