package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ChromaDBService} Retrofit endpoints use the ChromaDB v2 REST API
 * ({@code /api/v2/tenants/default_tenant/databases/default_database/...}) and not the
 * deprecated v1 API ({@code /api/v1/...}).
 *
 * <p>Each test wires a real Retrofit instance against a {@link MockWebServer} and checks
 * both the outgoing request path and the deserialized response.
 */
class ChromaDBServiceV2ApiTest {

    private static final String COLLECTION_JSON =
            "[{\"id\":\"uuid-abc\",\"name\":\"my-project\",\"tenant\":\"default_tenant\",\"database\":\"default_database\"}]";

    private MockWebServer server;
    private ChromaDBService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(ChromaDBService.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ── getCollections ──────────────────────────────────────────────────────────

    @Test
    void getCollections_usesV2Path() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        service.getCollections().execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath())
                .isEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections");
    }

    @Test
    void getCollections_pathDoesNotContainV1() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        service.getCollections().execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath())
                .as("must not use the deprecated /api/v1/ prefix")
                .doesNotContain("/api/v1/");
    }

    @Test
    void getCollections_parsesCollectionFields() throws IOException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(COLLECTION_JSON));

        Response<ChromaCollection[]> response = service.getCollections().execute();

        assertThat(response.isSuccessful()).isTrue();
        ChromaCollection[] collections = response.body();
        assertThat(collections).hasSize(1);
        assertThat(collections[0].id()).isEqualTo("uuid-abc");
        assertThat(collections[0].name()).isEqualTo("my-project");
        assertThat(collections[0].tenant()).isEqualTo("default_tenant");
        assertThat(collections[0].database()).isEqualTo("default_database");
    }

    @Test
    void getCollections_emptyArrayParsesCorrectly() throws IOException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        Response<ChromaCollection[]> response = service.getCollections().execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEmpty();
    }

    // ── getCount ───────────────────────────────────────────────────────────────

    @Test
    void getCount_usesV2PathWithCollectionId() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("42"));

        service.getCount("col-uuid-123").execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo(
                "/api/v2/tenants/default_tenant/databases/default_database/collections/col-uuid-123/count");
    }

    @Test
    void getCount_pathDoesNotContainV1() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("0"));

        service.getCount("any-id").execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).doesNotContain("/api/v1/");
    }

    @Test
    void getCount_substitutesCollectionIdInPath() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("7"));

        service.getCount("specific-uuid-xyz").execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).contains("specific-uuid-xyz");
    }

    @Test
    void getCount_parsesIntegerResponse() throws IOException {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("99"));

        Response<Integer> response = service.getCount("col-uuid-123").execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo(99);
    }

    // ── deleteCollection ────────────────────────────────────────────────────────

    @Test
    void deleteCollection_usesV2DeletePath() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        service.deleteCollection("my-project").execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo(
                "/api/v2/tenants/default_tenant/databases/default_database/collections/my-project");
    }

    @Test
    void deleteCollection_pathDoesNotContainV1() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        service.deleteCollection("any-collection").execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).doesNotContain("/api/v1/");
    }

    @Test
    void deleteCollection_substitutesCollectionNameInPath() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        service.deleteCollection("my-specific-project").execute();

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).contains("my-specific-project");
    }

    // ── Cross-endpoint regression guard ────────────────────────────────────────

    @Test
    void noEndpointUsesDeprecatedV1Api() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody("[]"));
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody("0"));
        server.enqueue(new MockResponse().setResponseCode(200));

        service.getCollections().execute();
        service.getCount("any-id").execute();
        service.deleteCollection("any-name").execute();

        for (int i = 0; i < 3; i++) {
            RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getPath())
                    .as("endpoint %d must not use the deprecated /api/v1/ path", i + 1)
                    .doesNotContain("/api/v1/");
        }
    }

    @Test
    void allEndpointsUseDefaultTenantAndDatabase() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody("[]"));
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody("0"));
        server.enqueue(new MockResponse().setResponseCode(200));

        service.getCollections().execute();
        service.getCount("any-id").execute();
        service.deleteCollection("any-name").execute();

        for (int i = 0; i < 3; i++) {
            RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getPath())
                    .as("endpoint %d must reference default_tenant", i + 1)
                    .contains("default_tenant");
            assertThat(request.getPath())
                    .as("endpoint %d must reference default_database", i + 1)
                    .contains("default_database");
        }
    }
}
