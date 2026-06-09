package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link ChromaEmbeddingService#init(Project)} wires the underlying
 * {@code ChromaEmbeddingStore} to use the ChromaDB <em>v2</em> REST API, not the
 * deprecated v1 API.
 *
 * <p>The test points the service at a {@link MockWebServer} and records every HTTP
 * request made during initialization so that paths can be asserted without a real
 * ChromaDB or Ollama instance.
 *
 * <p><strong>ChromaDB v2 initialization sequence (warm-start / all resources exist):</strong>
 * <ol>
 *   <li>{@code GET /api/v2/tenants/default_tenant} — check tenant</li>
 *   <li>{@code GET /api/v2/tenants/default_tenant/databases/default_database} — check database</li>
 *   <li>{@code GET /api/v2/tenants/default_tenant/databases/default_database/collections/{name}} — check collection</li>
 * </ol>
 *
 * <p><strong>Cold-start (resources must be created):</strong> each missing resource
 * causes a {@code POST} to create it before the next {@code GET}.
 */
@ExtendWith(MockitoExtension.class)
class ChromaEmbeddingServiceInitV2Test {

    private MockWebServer server;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private Project project;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getIndexerPort()).thenReturn(server.getPort());
        when(project.getName()).thenReturn("Test Project");
    }

    @AfterEach
    void tearDown() throws IOException {
        stateServiceStatic.close();
        server.shutdown();
    }

    /**
     * Enqueues MockWebServer responses for the warm-start case: tenant, database, and
     * collection all exist in ChromaDB already. Produces exactly 3 GET requests.
     */
    private void enqueueWarmStart() {
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"default_tenant\"}"));
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"default_database\",\"tenant\":\"default_tenant\"}"));
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"col-uuid-123\",\"name\":\"test-project\"," +
                         "\"tenant\":\"default_tenant\",\"database\":\"default_database\"}"));
    }

    /**
     * Enqueues MockWebServer responses for the cold-start case: tenant, database, and
     * collection must all be created. Produces 6 requests (3 GET + 3 POST).
     */
    private void enqueueColdStart() {
        // Tenant not found (ChromaDB returns 500 for missing tenant)
        server.enqueue(new MockResponse().setResponseCode(500));
        // Create tenant
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"default_tenant\"}"));
        // Database not found
        server.enqueue(new MockResponse().setResponseCode(500));
        // Create database
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"default_database\",\"tenant\":\"default_tenant\"}"));
        // Collection not found
        server.enqueue(new MockResponse().setResponseCode(500));
        // Create collection
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"new-col-uuid\",\"name\":\"test-project\"," +
                         "\"tenant\":\"default_tenant\",\"database\":\"default_database\"}"));
    }

    private List<RecordedRequest> drainRequests(int expectedCount) throws InterruptedException {
        List<RecordedRequest> requests = new ArrayList<>();
        for (int i = 0; i < expectedCount; i++) {
            RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
            assertThat(req).as("expected request %d of %d was not received", i + 1, expectedCount).isNotNull();
            requests.add(req);
        }
        return requests;
    }

    // ── Warm-start (all resources exist) ───────────────────────────────────────

    @Test
    void init_warmStart_allRequestsUseV2Api() throws Exception {
        enqueueWarmStart();

        new ChromaEmbeddingService().init(project);

        List<RecordedRequest> requests = drainRequests(3);
        for (RecordedRequest req : requests) {
            assertThat(req.getPath())
                    .as("init() must not call any deprecated /api/v1/ path")
                    .doesNotContain("/api/v1/");
            assertThat(req.getPath())
                    .as("init() must use the /api/v2/ prefix")
                    .startsWith("/api/v2/");
        }
    }

    @Test
    void init_warmStart_allRequestsReferenceDefaultTenant() throws Exception {
        enqueueWarmStart();

        new ChromaEmbeddingService().init(project);

        List<RecordedRequest> requests = drainRequests(3);
        assertThat(requests)
                .extracting(RecordedRequest::getPath)
                .as("every v2 request must scope to default_tenant")
                .allMatch(p -> p.contains("default_tenant"));
    }

    @Test
    void init_warmStart_databaseAndCollectionRequestsReferenceDefaultDatabase() throws Exception {
        enqueueWarmStart();

        new ChromaEmbeddingService().init(project);

        List<RecordedRequest> requests = drainRequests(3);
        // Requests 2 and 3 are database- and collection-scoped; request 1 is tenant-only.
        assertThat(requests.subList(1, 3))
                .extracting(RecordedRequest::getPath)
                .as("database and collection paths must reference default_database")
                .allMatch(p -> p.contains("default_database"));
    }

    @Test
    void init_warmStart_firstRequestChecksTenant() throws Exception {
        enqueueWarmStart();

        new ChromaEmbeddingService().init(project);

        RecordedRequest tenantRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(tenantRequest).isNotNull();
        assertThat(tenantRequest.getMethod()).isEqualTo("GET");
        assertThat(tenantRequest.getPath()).isEqualTo("/api/v2/tenants/default_tenant");
    }

    @Test
    void init_warmStart_secondRequestChecksDatabase() throws Exception {
        enqueueWarmStart();

        new ChromaEmbeddingService().init(project);

        server.takeRequest(5, TimeUnit.SECONDS); // skip tenant request
        RecordedRequest dbRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(dbRequest).isNotNull();
        assertThat(dbRequest.getMethod()).isEqualTo("GET");
        assertThat(dbRequest.getPath())
                .isEqualTo("/api/v2/tenants/default_tenant/databases/default_database");
    }

    @Test
    void init_warmStart_thirdRequestChecksCollection() throws Exception {
        enqueueWarmStart();

        new ChromaEmbeddingService().init(project);

        server.takeRequest(5, TimeUnit.SECONDS); // skip tenant
        server.takeRequest(5, TimeUnit.SECONDS); // skip database
        RecordedRequest collectionRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(collectionRequest).isNotNull();
        assertThat(collectionRequest.getMethod()).isEqualTo("GET");
        assertThat(collectionRequest.getPath())
                .startsWith("/api/v2/tenants/default_tenant/databases/default_database/collections/");
    }

    @Test
    void init_warmStart_embeddingStoreIsNonNullAfterInit() throws Exception {
        enqueueWarmStart();

        ChromaEmbeddingService service = new ChromaEmbeddingService();
        service.init(project);

        assertThat(service.getEmbeddingStore())
                .as("getEmbeddingStore() must return a non-null store after init()")
                .isNotNull();
    }

    @Test
    void init_warmStart_collectionPathContainsNormalisedProjectName() throws Exception {
        // Project name "Test Project" → normalised to "test-project" (lower-case, spaces → dashes)
        enqueueWarmStart();

        new ChromaEmbeddingService().init(project);

        List<RecordedRequest> requests = drainRequests(3);
        RecordedRequest collectionRequest = requests.get(2);
        assertThat(collectionRequest.getPath())
                .as("collection path must contain the normalised project name")
                .contains("test-project");
    }

    // ── Cold-start (all resources must be created) ─────────────────────────────

    @Test
    void init_coldStart_allRequestsUseV2Api() throws Exception {
        enqueueColdStart();

        new ChromaEmbeddingService().init(project);

        List<RecordedRequest> requests = drainRequests(6);
        for (RecordedRequest req : requests) {
            assertThat(req.getPath())
                    .as("cold-start init() must not call any deprecated /api/v1/ path")
                    .doesNotContain("/api/v1/");
            assertThat(req.getPath())
                    .as("cold-start init() must use the /api/v2/ prefix")
                    .startsWith("/api/v2/");
        }
    }

    @Test
    void init_coldStart_createsTenantAndDatabase() throws Exception {
        enqueueColdStart();

        new ChromaEmbeddingService().init(project);

        List<RecordedRequest> requests = drainRequests(6);

        // Request sequence: GET tenant → POST tenant → GET db → POST db → GET col → POST col
        assertThat(requests.get(0).getMethod()).as("1st: check tenant").isEqualTo("GET");
        assertThat(requests.get(1).getMethod()).as("2nd: create tenant").isEqualTo("POST");
        assertThat(requests.get(2).getMethod()).as("3rd: check database").isEqualTo("GET");
        assertThat(requests.get(3).getMethod()).as("4th: create database").isEqualTo("POST");
        assertThat(requests.get(4).getMethod()).as("5th: check collection").isEqualTo("GET");
        assertThat(requests.get(5).getMethod()).as("6th: create collection").isEqualTo("POST");
    }

    @Test
    void init_coldStart_createTenantUsesCorrectPath() throws Exception {
        enqueueColdStart();

        new ChromaEmbeddingService().init(project);

        List<RecordedRequest> requests = drainRequests(6);
        assertThat(requests.get(1).getPath())
                .as("create-tenant POST must target /api/v2/tenants")
                .isEqualTo("/api/v2/tenants");
    }

    @Test
    void init_coldStart_embeddingStoreIsNonNullAfterInit() throws Exception {
        enqueueColdStart();

        ChromaEmbeddingService service = new ChromaEmbeddingService();
        service.init(project);

        assertThat(service.getEmbeddingStore())
                .as("getEmbeddingStore() must return a non-null store even after cold-start creation")
                .isNotNull();
    }
}
