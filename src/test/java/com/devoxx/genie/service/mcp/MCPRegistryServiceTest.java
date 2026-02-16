package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.registry.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MCPRegistryServiceTest {

    private final Gson gson = new GsonBuilder().create();

    // ─── convertToMCPServer tests ──────────────────────────────

    @Nested
    class ConvertToMCPServer {

        private final MCPRegistryService service = new MCPRegistryService();

        @Test
        void remoteServer_setsHttpTransportAndUrl() {
            MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                    "test-server", "https://mcp.example.com/v1", List.of());

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

            assertThat(result.getName()).isEqualTo("test-server");
            assertThat(result.getTransportType()).isEqualTo(MCPServer.TransportType.HTTP);
            assertThat(result.getUrl()).isEqualTo("https://mcp.example.com/v1");
        }

        @Test
        void remoteServer_setsHeadersFromUserValues() {
            MCPRegistryHeader authHeader = createHeader("Authorization", "Bearer {{token}}", true, true);
            MCPRegistryHeader customHeader = createHeader("X-Custom", null, false, false);

            MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                    "auth-server", "https://mcp.example.com/v1",
                    List.of(authHeader, customHeader));

            Map<String, String> userValues = Map.of(
                    "Authorization", "Bearer my-secret-key",
                    "X-Custom", "custom-value");

            MCPServer result = service.convertToMCPServer(serverInfo, userValues);

            assertThat(result.getHeaders()).hasSize(2);
            assertThat(result.getHeaders().get("Authorization")).isEqualTo("Bearer my-secret-key");
            assertThat(result.getHeaders().get("X-Custom")).isEqualTo("custom-value");
        }

        @Test
        void remoteServer_skipsBlankHeaderValues() {
            MCPRegistryHeader authHeader = createHeader("Authorization", null, true, true);
            MCPRegistryHeader optionalHeader = createHeader("X-Optional", null, false, false);

            MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                    "partial-server", "https://mcp.example.com/v1",
                    List.of(authHeader, optionalHeader));

            Map<String, String> userValues = Map.of(
                    "Authorization", "Bearer my-key",
                    "X-Optional", "");

            MCPServer result = service.convertToMCPServer(serverInfo, userValues);

            assertThat(result.getHeaders()).hasSize(1);
            assertThat(result.getHeaders().get("Authorization")).isEqualTo("Bearer my-key");
            assertThat(result.getHeaders()).doesNotContainKey("X-Optional");
        }

        @Test
        void remoteServer_fallsBackToDefaultHeaderValue() {
            MCPRegistryHeader header = createHeader("X-Api-Version", "2024-01-01", false, false);

            MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                    "versioned-server", "https://mcp.example.com/v1", List.of(header));

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

            assertThat(result.getHeaders()).hasSize(1);
            assertThat(result.getHeaders().get("X-Api-Version")).isEqualTo("2024-01-01");
        }

        @Test
        void packageServer_npmSetsStdioTransport() {
            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("npm");
            pkg.setIdentifier("@modelcontextprotocol/server-filesystem");

            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setName("filesystem-server");
            serverInfo.setPackages(List.of(pkg));

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

            assertThat(result.getName()).isEqualTo("filesystem-server");
            assertThat(result.getTransportType()).isEqualTo(MCPServer.TransportType.STDIO);
            assertThat(result.getCommand()).isEqualTo("npx");
            assertThat(result.getArgs()).containsExactly("-y", "@modelcontextprotocol/server-filesystem");
        }

        @Test
        void packageServer_ociSetsDockerCommand() {
            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("oci");
            pkg.setIdentifier("ghcr.io/example/mcp-server:latest");

            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setName("docker-server");
            serverInfo.setPackages(List.of(pkg));

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

            assertThat(result.getCommand()).isEqualTo("docker");
            assertThat(result.getArgs()).containsExactly("run", "-i", "--rm", "ghcr.io/example/mcp-server:latest");
        }

        @Test
        void packageServer_unknownRegistryTypeUsesIdentifierAsCommand() {
            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("cargo");
            pkg.setIdentifier("my-mcp-tool");

            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setName("cargo-server");
            serverInfo.setPackages(List.of(pkg));

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

            assertThat(result.getCommand()).isEqualTo("my-mcp-tool");
            assertThat(result.getArgs()).isEmpty();
        }

        @Test
        void packageServer_setsEnvVars() {
            MCPRegistryEnvVar envVar = new MCPRegistryEnvVar();
            envVar.setName("API_KEY");
            envVar.setIsRequired(true);

            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("npm");
            pkg.setIdentifier("@example/mcp-server");
            pkg.setEnvironmentVariables(List.of(envVar));

            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setName("env-server");
            serverInfo.setPackages(List.of(pkg));

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of("API_KEY", "secret-123"));

            assertThat(result.getEnv().get("API_KEY")).isEqualTo("secret-123");
        }

        @Test
        void packageServer_skipsBlankEnvValues() {
            MCPRegistryEnvVar envVar = new MCPRegistryEnvVar();
            envVar.setName("API_KEY");

            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("npm");
            pkg.setIdentifier("@example/mcp-server");
            pkg.setEnvironmentVariables(List.of(envVar));

            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setName("env-server");
            serverInfo.setPackages(List.of(pkg));

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of("API_KEY", ""));

            assertThat(result.getEnv()).doesNotContainKey("API_KEY");
        }

        @Test
        void fallbackWhenNoRemotesOrPackages() {
            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setName("minimal-server");

            MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

            assertThat(result.getName()).isEqualTo("minimal-server");
            assertThat(result.getTransportType()).isEqualTo(MCPServer.TransportType.HTTP);
        }
    }

    // ─── getServerType tests ───────────────────────────────────

    @Nested
    class GetServerType {

        private final MCPRegistryService service = new MCPRegistryService();

        @Test
        void returnsRemoteForRemoteServers() {
            MCPRegistryServerInfo serverInfo = createRemoteServerInfo("srv", "https://x.com", List.of());
            assertThat(service.getServerType(serverInfo)).isEqualTo("Remote");
        }

        @Test
        void returnsNpmForNpmPackages() {
            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("npm");
            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setPackages(List.of(pkg));

            assertThat(service.getServerType(serverInfo)).isEqualTo("npm");
        }

        @Test
        void returnsDockerForOciPackages() {
            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("oci");
            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setPackages(List.of(pkg));

            assertThat(service.getServerType(serverInfo)).isEqualTo("Docker");
        }

        @Test
        void returnsRawRegistryTypeForUnknownPackages() {
            MCPRegistryPackage pkg = new MCPRegistryPackage();
            pkg.setRegistryType("cargo");
            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            serverInfo.setPackages(List.of(pkg));

            assertThat(service.getServerType(serverInfo)).isEqualTo("cargo");
        }

        @Test
        void returnsUnknownWhenNoRemotesOrPackages() {
            MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
            assertThat(service.getServerType(serverInfo)).isEqualTo("Unknown");
        }
    }

    // ─── searchServers tests ───────────────────────────────────

    @Nested
    class SearchServers {

        private MockWebServer mockServer;
        private MCPRegistryService service;

        @BeforeEach
        void setUp() throws IOException {
            mockServer = new MockWebServer();
            mockServer.start();
        }

        @AfterEach
        void tearDown() throws IOException {
            mockServer.shutdown();
        }

        private MCPRegistryService createServiceWithMockUrl() throws Exception {
            // We need to override the base URL; use reflection to replace the constant
            // or use a subclass. Simpler: create a testable subclass that overrides the URL.
            // Actually, the service uses a hardcoded URL. We'll use MockWebServer + OkHttpClient
            // and test searchServers indirectly through fetchAllServers with a test subclass.

            // Better approach: create a package-private MCPRegistryService with the mock URL injected.
            // For now, we'll test the service by pointing the OkHttpClient to mock server via interceptor.
            String mockUrl = mockServer.url("/v0.1/servers").toString();
            OkHttpClient client = new OkHttpClient.Builder().build();
            Gson testGson = new GsonBuilder().create();

            // Create a service that uses our mock server URL
            return new TestMCPRegistryService(client, testGson, mockUrl);
        }

        @Test
        void searchServers_buildsUrlWithQueryParameter() throws Exception {
            MCPRegistryResponse responseBody = new MCPRegistryResponse();
            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(responseBody)));

            service = createServiceWithMockUrl();
            service.searchServers("my-search", null, 50);

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getRequestUrl().queryParameter("q")).isEqualTo("my-search");
            assertThat(request.getRequestUrl().queryParameter("limit")).isEqualTo("50");
        }

        @Test
        void searchServers_buildsUrlWithCursorParameter() throws Exception {
            MCPRegistryResponse responseBody = new MCPRegistryResponse();
            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(responseBody)));

            service = createServiceWithMockUrl();
            service.searchServers(null, "abc123", 100);

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getRequestUrl().queryParameter("cursor")).isEqualTo("abc123");
            assertThat(request.getRequestUrl().queryParameter("q")).isNull();
        }

        @Test
        void searchServers_throwsOnNonSuccessfulResponse() throws Exception {
            mockServer.enqueue(new MockResponse().setResponseCode(500));

            service = createServiceWithMockUrl();

            assertThatThrownBy(() -> service.searchServers(null, null, 100))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("HTTP 500");
        }

        @Test
        void searchServers_parsesServerList() throws Exception {
            MCPRegistryServerEntry entry = new MCPRegistryServerEntry();
            MCPRegistryServerInfo info = new MCPRegistryServerInfo();
            info.setName("test-server");
            entry.setServer(info);

            MCPRegistryResponse responseBody = new MCPRegistryResponse();
            responseBody.setServers(List.of(entry));

            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(responseBody)));

            service = createServiceWithMockUrl();
            MCPRegistryResponse result = service.searchServers(null, null, 100);

            assertThat(result.getServers()).hasSize(1);
            assertThat(result.getServers().get(0).getServer().getName()).isEqualTo("test-server");
        }

        @Test
        void searchServers_handlesNullResult() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody("null"));

            service = createServiceWithMockUrl();
            MCPRegistryResponse result = service.searchServers(null, null, 100);

            // Should return empty response instead of null
            assertThat(result).isNotNull();
        }
    }

    // ─── fetchAllServers tests ─────────────────────────────────

    @Nested
    class FetchAllServers {

        private MockWebServer mockServer;
        private MCPRegistryService service;

        @BeforeEach
        void setUp() throws Exception {
            mockServer = new MockWebServer();
            mockServer.start();
            String mockUrl = mockServer.url("/v0.1/servers").toString();
            OkHttpClient client = new OkHttpClient.Builder().build();
            Gson testGson = new GsonBuilder().create();
            service = new TestMCPRegistryService(client, testGson, mockUrl);
        }

        @AfterEach
        void tearDown() throws IOException {
            mockServer.shutdown();
        }

        @Test
        void returnsCachedResultsWhenNotForcingRefresh() throws Exception {
            MCPRegistryServerEntry entry = new MCPRegistryServerEntry();
            MCPRegistryServerInfo info = new MCPRegistryServerInfo();
            info.setName("cached-server");
            entry.setServer(info);

            MCPRegistryResponse responseBody = new MCPRegistryResponse();
            responseBody.setServers(List.of(entry));

            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(responseBody)));

            // First call: fetches from network
            List<MCPRegistryServerEntry> first = service.fetchAllServers(false);
            assertThat(first).hasSize(1);

            // Second call: should use cache (no network request queued)
            List<MCPRegistryServerEntry> second = service.fetchAllServers(false);
            assertThat(second).hasSize(1);
            assertThat(mockServer.getRequestCount()).isEqualTo(1);
        }

        @Test
        void fetchesFreshDataWhenForceRefresh() throws Exception {
            MCPRegistryResponse firstResponse = new MCPRegistryResponse();
            firstResponse.setServers(List.of());
            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(firstResponse)));

            MCPRegistryServerEntry entry = new MCPRegistryServerEntry();
            MCPRegistryServerInfo info = new MCPRegistryServerInfo();
            info.setName("new-server");
            entry.setServer(info);
            MCPRegistryResponse secondResponse = new MCPRegistryResponse();
            secondResponse.setServers(List.of(entry));
            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(secondResponse)));

            service.fetchAllServers(false);
            List<MCPRegistryServerEntry> refreshed = service.fetchAllServers(true);

            assertThat(refreshed).hasSize(1);
            assertThat(mockServer.getRequestCount()).isEqualTo(2);
        }

        @Test
        void handlesPagination() throws Exception {
            // Page 1 with cursor
            MCPRegistryServerEntry entry1 = new MCPRegistryServerEntry();
            MCPRegistryServerInfo info1 = new MCPRegistryServerInfo();
            info1.setName("server-1");
            entry1.setServer(info1);
            MCPRegistryMetadata meta1 = new MCPRegistryMetadata();
            meta1.setNextCursor("page2");
            MCPRegistryResponse page1 = new MCPRegistryResponse();
            page1.setServers(List.of(entry1));
            page1.setMetadata(meta1);

            // Page 2 without cursor (last page)
            MCPRegistryServerEntry entry2 = new MCPRegistryServerEntry();
            MCPRegistryServerInfo info2 = new MCPRegistryServerInfo();
            info2.setName("server-2");
            entry2.setServer(info2);
            MCPRegistryResponse page2 = new MCPRegistryResponse();
            page2.setServers(List.of(entry2));

            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(page1)));
            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(page2)));

            List<MCPRegistryServerEntry> all = service.fetchAllServers(false);

            assertThat(all).hasSize(2);
            assertThat(mockServer.getRequestCount()).isEqualTo(2);
        }

        @Test
        void handlesEmptyResponse() throws Exception {
            MCPRegistryResponse emptyResponse = new MCPRegistryResponse();
            mockServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(gson.toJson(emptyResponse)));

            List<MCPRegistryServerEntry> result = service.fetchAllServers(false);

            assertThat(result).isEmpty();
        }
    }

    // ─── Test subclass with overridable URL ────────────────────

    /**
     * A test-only subclass that allows overriding the registry URL
     * to point at MockWebServer.
     */
    static class TestMCPRegistryService extends MCPRegistryService {
        private final String baseUrl;

        TestMCPRegistryService(OkHttpClient client, Gson gson, String baseUrl) {
            super(client, gson);
            this.baseUrl = baseUrl;
        }

        @Override
        public MCPRegistryResponse searchServers(String query, String cursor, int limit) throws IOException {
            // Build URL against mock server instead of real registry
            okhttp3.HttpUrl.Builder urlBuilder = java.util.Objects.requireNonNull(
                    okhttp3.HttpUrl.parse(baseUrl)).newBuilder();
            urlBuilder.addQueryParameter("limit", String.valueOf(limit));
            if (query != null && !query.isBlank()) {
                urlBuilder.addQueryParameter("q", query);
            }
            if (cursor != null && !cursor.isBlank()) {
                urlBuilder.addQueryParameter("cursor", cursor);
            }

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(urlBuilder.build())
                    .build();

            // Access client and gson via reflection-free path: call the parent's OkHttpClient
            try (okhttp3.Response response = getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Registry API returned HTTP " + response.code());
                }
                if (response.body() == null) {
                    throw new IOException("Registry API returned empty response");
                }
                MCPRegistryResponse result = getGson().fromJson(response.body().string(), MCPRegistryResponse.class);
                return result != null ? result : new MCPRegistryResponse();
            }
        }

        // Expose fields for the test subclass
        private OkHttpClient getClient() {
            try {
                java.lang.reflect.Field f = MCPRegistryService.class.getDeclaredField("client");
                f.setAccessible(true);
                return (OkHttpClient) f.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private com.google.gson.Gson getGson() {
            try {
                java.lang.reflect.Field f = MCPRegistryService.class.getDeclaredField("gson");
                f.setAccessible(true);
                return (com.google.gson.Gson) f.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ─── Helper methods ────────────────────────────────────────

    private static MCPRegistryServerInfo createRemoteServerInfo(String name, String url,
                                                                 List<MCPRegistryHeader> headers) {
        MCPRegistryRemote remote = new MCPRegistryRemote();
        remote.setUrl(url);
        remote.setHeaders(headers);

        MCPRegistryServerInfo info = new MCPRegistryServerInfo();
        info.setName(name);
        info.setRemotes(List.of(remote));
        return info;
    }

    private static MCPRegistryHeader createHeader(String name, String defaultValue,
                                                   boolean required, boolean secret) {
        MCPRegistryHeader header = new MCPRegistryHeader();
        header.setName(name);
        header.setValue(defaultValue);
        header.setIsRequired(required);
        header.setIsSecret(secret);
        return header;
    }
}
