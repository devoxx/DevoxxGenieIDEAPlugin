package com.devoxx.genie.chatmodel.local.lmstudio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LMStudioModelServiceTest {

    private static class TestableModelService extends LMStudioModelService {
        public String testBuildModelsUrl(String baseUrl) {
            return buildModelsUrl(baseUrl);
        }
    }

    @Test
    void buildModelsUrl_withV1BaseUrl_returnsApiV1ModelsUrl() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:1234/v1/");
        assertThat(url).isEqualTo("http://localhost:1234/api/v1/models");
    }

    @Test
    void buildModelsUrl_withApiV1BaseUrl_returnsApiV1ModelsUrl() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:1234/api/v1/");
        assertThat(url).isEqualTo("http://localhost:1234/api/v1/models");
    }

    @Test
    void buildModelsUrl_withNullBaseUrl_returnsDefaultUrl() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl(null);
        assertThat(url).isEqualTo("http://localhost:1234/api/v1/models");
    }

    @Test
    void buildModelsUrl_withBlankBaseUrl_returnsDefaultUrl() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("  ");
        assertThat(url).isEqualTo("http://localhost:1234/api/v1/models");
    }

    @Test
    void buildModelsUrl_withCustomPort_preservesHostAndPort() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:5678/v1/");
        assertThat(url).isEqualTo("http://localhost:5678/api/v1/models");
    }

    @Test
    void buildModelsUrl_withCustomHost_preservesHost() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://myserver:1234/v1/");
        assertThat(url).isEqualTo("http://myserver:1234/api/v1/models");
    }

    @Test
    void buildModelsUrl_withoutTrailingSlash_stillWorks() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:1234/v1");
        assertThat(url).isEqualTo("http://localhost:1234/api/v1/models");
    }

    @Test
    void buildModelsUrl_withRootPath_extractsHostCorrectly() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("http://localhost:1234/");
        assertThat(url).isEqualTo("http://localhost:1234/api/v1/models");
    }

    @Test
    void buildModelsUrl_withHttps_preservesScheme() {
        TestableModelService service = new TestableModelService();
        String url = service.testBuildModelsUrl("https://lmstudio.example.com:1234/v1/");
        assertThat(url).isEqualTo("https://lmstudio.example.com:1234/api/v1/models");
    }
}
