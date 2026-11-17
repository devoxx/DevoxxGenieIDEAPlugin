package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Reproduces <a href="https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1421">#1421</a>:
 * RAG indexing never starts when the project name has no ASCII letters (e.g. Cyrillic), because
 * every non-ASCII character is replaced by {@code -} and ChromaDB rejects the resulting
 * collection name {@code "---------"}.
 *
 * <p>Each test drives the real {@link ChromaEmbeddingService#init(Project)} against a
 * {@link MockWebServer} that plays a cold-start ChromaDB, captures the collection name sent in
 * the create-collection {@code POST}, and checks it against ChromaDB's own naming rules.
 */
class ChromaCollectionNameTest {

    /** The rules from ChromaDB's {@code InvalidArgumentError}, quoted in the issue. */
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{1,61}[A-Za-z0-9]");
    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern NAME_IN_BODY = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"");

    private MockWebServer server;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getIndexerPort()).thenReturn(server.getPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        stateServiceStatic.close();
        server.shutdown();
    }

    // ── The reported case and its relatives: names Chroma rejects ──────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Кириллица",            // the issue: all Cyrillic → "---------"
            "项目",                  // CJK, and only 2 chars → too short as well
            "Ελληνικά",             // Greek
            "Мой project",          // starts with a non-ASCII word → leading '-'
            "project Мой",          // ends with a non-ASCII word → trailing '-'
            "_internal",            // leading underscore → '-'
            "my project!",          // trailing punctuation → '-'
            "ab",                   // ASCII but shorter than 3 chars
            "x",                    // single char
            "192.168.0.1",          // looks like an IPv4 address
    })
    void collectionName_isAcceptedByChroma(String projectName) throws Exception {
        String collection = collectionNameSentFor(projectName);

        assertThat(violation(collection))
                .as("collection name '%s' derived from project '%s' must satisfy ChromaDB's rules",
                        collection, projectName)
                .isNull();
    }

    @Test
    void collectionName_longProjectName_isAcceptedByChroma() throws Exception {
        String collection = collectionNameSentFor("a-very-long-project-name-".repeat(4)); // 100 chars

        assertThat(violation(collection))
                .as("collection name '%s' must satisfy ChromaDB's rules", collection)
                .isNull();
    }

    // ── Distinct projects must not share one index ─────────────────────────────

    @Test
    void differentNonAsciiProjects_getDifferentCollections() throws Exception {
        // Both are 9 Cyrillic letters, so today both map to "---------" and would share an index.
        String first = collectionNameSentFor("Кириллица");
        String second = collectionNameSentFor("Латиницаа");

        assertThat(first).as("two different projects must not share a collection").isNotEqualTo(second);
    }

    // ── Existing indexes must keep working ─────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"Test Project", "DevoxxGenieIDEAPlugin", "my-app", "spring_boot 3"})
    void validAsciiProjectNames_keepTheirCurrentCollectionName(String projectName) throws Exception {
        // A project indexed by an earlier release must find its collection again after the fix.
        String legacy = projectName.toLowerCase().replaceAll("[^a-z0-9-]", "-");

        assertThat(collectionNameSentFor(projectName)).isEqualTo(legacy);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** Runs init() against a cold-start Chroma and returns the name in the create-collection POST. */
    private String collectionNameSentFor(String projectName) throws Exception {
        Project project = mock(Project.class);
        when(project.getName()).thenReturn(projectName);

        server.enqueue(new MockResponse().setResponseCode(404));          // GET tenant: missing
        server.enqueue(json("{\"name\":\"default_tenant\"}"));            // POST tenant
        server.enqueue(new MockResponse().setResponseCode(404));          // GET database: missing
        server.enqueue(json("{\"name\":\"default_database\",\"tenant\":\"default_tenant\"}"));
        server.enqueue(new MockResponse().setResponseCode(400));          // GET collection: missing
        server.enqueue(json("{\"id\":\"col-uuid\",\"name\":\"x\"," +      // POST collection
                "\"tenant\":\"default_tenant\",\"database\":\"default_database\"}"));

        new ChromaEmbeddingService().init(project);

        for (int i = 0; i < 5; i++) {
            assertThat(server.takeRequest(5, TimeUnit.SECONDS)).isNotNull();
        }
        RecordedRequest create = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(create).isNotNull();
        assertThat(create.getMethod()).isEqualTo("POST");
        Matcher m = NAME_IN_BODY.matcher(create.getBody().readUtf8());
        assertThat(m.find()).as("create-collection body must carry a name").isTrue();
        return m.group(1);
    }

    private static MockResponse json(String body) {
        return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
    }

    /** Returns which ChromaDB rule {@code name} breaks, or {@code null} when it is valid. */
    private static String violation(String name) {
        if (name.length() < 3 || name.length() > 63) return "(1) must contain 3-63 characters";
        if (!ALLOWED.matcher(name).matches())
            return "(2)/(3) must start and end alphanumeric and contain only alphanumerics, '_', '-', '.'";
        if (name.contains("..")) return "(4) must not contain '..'";
        if (IPV4.matcher(name).matches()) return "(5) must not be an IPv4 address";
        return null;
    }
}
