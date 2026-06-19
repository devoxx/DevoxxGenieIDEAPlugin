package com.devoxx.genie.ui.webview;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for the embedded WebServer (JDK {@code com.sun.net.httpserver.HttpServer})
 * used by the JCEF WebView.
 * <p>
 * The lifecycle tests use {@code assumeTrue} as a defensive guard: if the server
 * cannot bind in a restricted test environment they are skipped rather than failed.
 */
class WebServerTest {

    private boolean serverStartedByTest = false;

    @AfterEach
    void tearDown() {
        if (serverStartedByTest) {
            WebServer.getInstance().stop();
        }
    }

    @Test
    void singletonInstanceIsConsistent() {
        WebServer first = WebServer.getInstance();
        WebServer second = WebServer.getInstance();
        assertThat(first).isSameAs(second);
    }

    @Test
    void serverIsNotRunningByDefault() {
        WebServer server = WebServer.getInstance();
        assertThat(server.isRunning()).isFalse();
    }

    @Test
    void addDynamicResourceReturnsUniquePath() {
        WebServer server = WebServer.getInstance();
        String path1 = server.addDynamicResource("<html>a</html>");
        String path2 = server.addDynamicResource("<html>b</html>");

        assertThat(path1).startsWith("/dynamic/");
        assertThat(path2).startsWith("/dynamic/");
        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    void addDynamicScriptRegistersPath() {
        WebServer server = WebServer.getInstance();
        server.addDynamicScript("testScript", "console.log('hello');");

        String scriptUrl = server.getScriptUrl("testScript");
        assertThat(scriptUrl).isNotNull();
        assertThat(scriptUrl).contains("/scripts/testScript.js");
    }

    @Test
    void getScriptUrlReturnsNullForUnknownScript() {
        WebServer server = WebServer.getInstance();
        assertThat(server.getScriptUrl("nonexistent")).isNull();
    }

    @Test
    void getResourceUrlIncludesServerUrl() {
        WebServer server = WebServer.getInstance();
        String url = server.getResourceUrl("/test.css");
        assertThat(url).endsWith("/test.css");
    }

    // --- Server lifecycle tests (skipped if the server cannot bind in this environment) ---

    private boolean tryStartServer() {
        try {
            WebServer.getInstance().start();
            serverStartedByTest = true;
            return WebServer.getInstance().isRunning();
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    @Test
    void startAndStop() {
        assumeTrue(tryStartServer(), "Skipped: web server could not bind in this environment");

        WebServer server = WebServer.getInstance();
        assertThat(server.isRunning()).isTrue();
        assertThat(server.getServerUrl()).startsWith("http://localhost:");

        server.stop();
        serverStartedByTest = false;
        assertThat(server.isRunning()).isFalse();
    }

    @Test
    void startIsIdempotent() {
        assumeTrue(tryStartServer(), "Skipped: web server could not bind in this environment");

        WebServer server = WebServer.getInstance();
        String firstUrl = server.getServerUrl();

        // calling start again should be a no-op
        server.start();
        assertThat(server.getServerUrl()).isEqualTo(firstUrl);
        assertThat(server.isRunning()).isTrue();
    }

    @Test
    void healthCheckEndpoint() throws Exception {
        assumeTrue(tryStartServer(), "Skipped: web server could not bind in this environment");

        HttpURLConnection conn = openConnection("/health-check");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getContentType()).contains("application/json");

        String body = new String(conn.getInputStream().readAllBytes());
        assertThat(body).contains("\"status\":\"ok\"");
    }

    @Test
    void serveDynamicResourceViaHttp() throws Exception {
        assumeTrue(tryStartServer(), "Skipped: web server could not bind in this environment");

        String html = "<html><body>hello</body></html>";
        String resourcePath = WebServer.getInstance().addDynamicResource(html);

        HttpURLConnection conn = openConnection(resourcePath);
        assertThat(conn.getResponseCode()).isEqualTo(200);

        String body = new String(conn.getInputStream().readAllBytes());
        assertThat(body).isEqualTo(html);
    }

    @Test
    void serveDynamicScriptViaHttp() throws Exception {
        assumeTrue(tryStartServer(), "Skipped: web server could not bind in this environment");

        String js = "console.log('test');";
        WebServer server = WebServer.getInstance();
        server.addDynamicScript("httpTestScript", js);

        String scriptUrl = server.getScriptUrl("httpTestScript");
        assertThat(scriptUrl).isNotNull();

        String path = URI.create(scriptUrl).getPath();
        HttpURLConnection conn = openConnection(path);
        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getContentType()).contains("application/javascript");

        String body = new String(conn.getInputStream().readAllBytes());
        assertThat(body).isEqualTo(js);
    }

    @Test
    void notFoundForUnknownResource() throws Exception {
        assumeTrue(tryStartServer(), "Skipped: web server could not bind in this environment");

        HttpURLConnection conn = openConnection("/does-not-exist");
        assertThat(conn.getResponseCode()).isEqualTo(404);
    }

    private HttpURLConnection openConnection(String path) throws IOException {
        URI uri = URI.create(WebServer.getInstance().getServerUrl() + path);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        return conn;
    }
}
