package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class FetchPageToolExecutorTest {

    private MockWebServer server;
    private FetchPageToolExecutor executor;
    private final AtomicLong now = new AtomicLong(1_000_000);

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        executor = new FetchPageToolExecutor(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                new PageUrlPolicy(false, true),
                new FetchedPageCache(Duration.ofMinutes(15), 32, now::get));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private String fetch(String url) {
        return executor.execute(
                ToolExecutionRequest.builder().name("fetch_page").arguments("{\"url\":\"" + url + "\"}").build(),
                null);
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
        String bigText = "word ".repeat(30_000);
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

    @Test
    void htmlIsReturnedAsMarkdownWithCodeBlocksAndTables() {
        server.enqueue(new MockResponse()
                .setBody("<html><body><h1>Migrate</h1><pre><code class=\"language-java\">a();\nb();</code></pre>"
                        + "<table><tr><th>Old</th><th>New</th></tr><tr><td>connect()</td><td>open()</td></tr></table>"
                        + "</body></html>")
                .setHeader("Content-Type", "text/html; charset=utf-8"));

        assertThat(fetch(server.url("/guide").toString()))
                .isEqualTo("# Migrate\n\n```java\na();\nb();\n```\n\n| Old | New |\n| --- | --- |\n| connect() | open() |");
    }

    @Test
    void plainTextJsonAndMarkdownAreReturnedAsIs() {
        server.enqueue(new MockResponse().setBody("line one\n  line two").setHeader("Content-Type", "text/plain"));
        server.enqueue(new MockResponse().setBody("{\"a\": [1, 2]}").setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("# Title\n\n- item").setHeader("Content-Type", "text/markdown"));

        assertThat(fetch(server.url("/a.txt").toString())).isEqualTo("line one\n  line two");
        assertThat(fetch(server.url("/a.json").toString())).isEqualTo("{\"a\": [1, 2]}");
        assertThat(fetch(server.url("/a.md").toString())).isEqualTo("# Title\n\n- item");
    }

    @Test
    void markdownIsRequestedBeforeHtml() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("# Doc").setHeader("Content-Type", "text/markdown"));

        fetch(server.url("/doc").toString());

        assertThat(server.takeRequest().getHeader("Accept")).startsWith("text/markdown");
    }

    @Test
    void textUsesTheCharsetFromTheContentType() {
        server.enqueue(new MockResponse()
                .setBody(new Buffer().write("café".getBytes(StandardCharsets.ISO_8859_1)))
                .setHeader("Content-Type", "text/plain; charset=ISO-8859-1"));

        assertThat(fetch(server.url("/latin1").toString())).isEqualTo("café");
    }

    @Test
    void binaryContentIsDescribedInsteadOfDecoded() {
        server.enqueue(new MockResponse()
                .setBody(new Buffer().write(new byte[]{'%', 'P', 'D', 'F', 0, (byte) 0xFF, 1, 2}))
                .setHeader("Content-Type", "application/pdf"));

        assertThat(fetch(server.url("/file.pdf").toString()))
                .isEqualTo("[Binary content (application/pdf, 1 KB) not shown: fetch_page only returns text.]");
    }

    @Test
    void redirectOnTheSameHostIsFollowed() {
        server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "/new-place"));
        server.enqueue(new MockResponse().setBody("<p>Moved content</p>").setHeader("Content-Type", "text/html"));

        assertThat(fetch(server.url("/old-place").toString())).isEqualTo("Moved content");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void redirectToAnotherHostIsReportedAndNotFollowed() {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "https://other.example.com/page"));

        var result = fetch(server.url("/out").toString());

        assertThat(result).contains("different host").contains("https://other.example.com/page");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void endlessRedirectsStopAfterTen() {
        for (int i = 0; i < 12; i++) {
            server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/loop" + i));
        }

        assertThat(fetch(server.url("/loop").toString())).isEqualTo("Error: More than 10 redirects fetching "
                + server.url("/loop"));
    }

    @Test
    void samePageWithinFifteenMinutesComesFromTheCache() {
        server.enqueue(new MockResponse().setBody("<p>First</p>").setHeader("Content-Type", "text/html"));
        server.enqueue(new MockResponse().setBody("<p>Second</p>").setHeader("Content-Type", "text/html"));
        var url = server.url("/cached").toString();

        var first = fetch(url);
        now.addAndGet(Duration.ofMinutes(14).toMillis());
        var second = fetch(url);

        assertThat(first).isEqualTo("First");
        assertThat(second).isEqualTo("First");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void cachedPageIsFetchedAgainAfterFifteenMinutes() {
        server.enqueue(new MockResponse().setBody("<p>First</p>").setHeader("Content-Type", "text/html"));
        server.enqueue(new MockResponse().setBody("<p>Second</p>").setHeader("Content-Type", "text/html"));
        var url = server.url("/expiring").toString();

        fetch(url);
        now.addAndGet(Duration.ofMinutes(16).toMillis());

        assertThat(fetch(url)).isEqualTo("Second");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void errorsAreNotCached() {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setBody("<p>Back</p>").setHeader("Content-Type", "text/html"));
        var url = server.url("/flaky").toString();

        assertThat(fetch(url)).isEqualTo("Error: HTTP 503 fetching " + url);
        assertThat(fetch(url)).isEqualTo("Back");
    }
}
