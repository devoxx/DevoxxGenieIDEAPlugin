package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class FetchPageToolExecutorTest {

    private MockWebServer server;
    private FetchPageToolExecutor executor;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        executor = new FetchPageToolExecutor(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void missingUrl_returnsError() {
        String result = executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{}").build(),
                null);
        assertThat(result).contains("Error").contains("'url' parameter is required");
    }

    @Test
    void emptyUrl_returnsError() {
        String result = executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{\"url\":\"\"}").build(),
                null);
        assertThat(result).contains("Error").contains("'url' parameter is required");
    }

    @Test
    void invalidScheme_returnsError() {
        String result = executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{\"url\":\"ftp://example.com\"}").build(),
                null);
        assertThat(result).contains("Error").contains("http://").contains("https://");
    }

    @Test
    void simpleHtml_returnsTextOnly() {
        server.enqueue(new MockResponse()
                .setBody("<html><body><h1>Hello</h1><p>World</p></body></html>")
                .setHeader("Content-Type", "text/html"));

        String url = server.url("/page").toString();
        String result = executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{\"url\":\"" + url + "\"}").build(),
                null);

        assertThat(result).contains("Hello").contains("World");
        assertThat(result).doesNotContain("<h1>").doesNotContain("<p>");
    }

    @Test
    void htmlWithScriptAndStyle_stripsNonVisibleContent() {
        server.enqueue(new MockResponse()
                .setBody("<html><head><style>body{color:red}</style></head>" +
                        "<body><script>alert('xss')</script><p>Visible text</p>" +
                        "<noscript>Enable JS</noscript></body></html>")
                .setHeader("Content-Type", "text/html"));

        String url = server.url("/page").toString();
        String result = executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{\"url\":\"" + url + "\"}").build(),
                null);

        assertThat(result).contains("Visible text");
        assertThat(result).doesNotContain("alert").doesNotContain("color:red").doesNotContain("Enable JS");
    }

    @Test
    void http404_returnsError() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        String url = server.url("/missing").toString();
        String result = executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{\"url\":\"" + url + "\"}").build(),
                null);

        assertThat(result).contains("Error").contains("HTTP 404");
    }

    @Test
    void largeContent_isTruncated() {
        String bigText = "word ".repeat(30_000); // ~150K chars
        server.enqueue(new MockResponse()
                .setBody("<html><body><p>" + bigText + "</p></body></html>")
                .setHeader("Content-Type", "text/html"));

        String url = server.url("/big").toString();
        String result = executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{\"url\":\"" + url + "\"}").build(),
                null);

        assertThat(result).contains("[Content truncated at 100000 characters]");
        assertThat(result.length()).isLessThan(100_100);
    }
}
