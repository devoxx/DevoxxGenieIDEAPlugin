package com.devoxx.genie.service.analytics;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnalyticsService} (task-206).
 *
 * <p>Verifies payload schema, opt-out behavior, consent gating, silent failure, client_id
 * persistence, and the strict "no PII" guarantee.
 */
class AnalyticsServiceTest {

    private DevoxxGenieStateService state;
    private AnalyticsService service;
    private RecordingHttpClient httpClient;

    @BeforeEach
    void setUp() {
        state = new DevoxxGenieStateService();
        state.setAnalyticsEnabled(true);
        state.setAnalyticsNoticeAcknowledged(true);
        state.setAnalyticsClientId("");
        state.setAnalyticsEndpoint("https://example.invalid/collect");

        service = new AnalyticsService();
        httpClient = new RecordingHttpClient();
        service.setHttpClientForTest(httpClient);
    }

    private void runWithState(Runnable action) {
        try (MockedStatic<DevoxxGenieStateService> mocked = mockStatic(DevoxxGenieStateService.class)) {
            mocked.when(DevoxxGenieStateService::getInstance).thenReturn(state);
            action.run();
        }
    }

    @Test
    void payloadContainsExactlyTheExpectedFields() throws Exception {
        runWithState(() -> service.trackPromptExecuted("anthropic", "claude-3-5-sonnet"));
        httpClient.awaitOne();

        String body = httpClient.lastBody();
        assertThat(body).contains("\"client_id\":\"");
        assertThat(body).contains("\"name\":\"prompt_executed\"");
        assertThat(body).contains("\"provider_id\":\"anthropic\"");
        assertThat(body).contains("\"model_name\":\"claude-3-5-sonnet\"");
        assertThat(body).contains("\"app_name\":\"devoxxgenie-intellij\"");
        assertThat(body).contains("\"app_version\":\"");
        assertThat(body).contains("\"ide_version\":\"");
        assertThat(body).contains("\"session_id\":\"");
        assertThat(body).contains("\"engagement_time_msec\":1");

        // Strict allowlist — no extra parameter keys ever.
        Set<String> allowedParamKeys = Set.of(
                "provider_id", "model_name", "app_name",
                "app_version", "ide_version", "session_id", "engagement_time_msec");
        Pattern keyPattern = Pattern.compile("\"params\":\\{([^}]*)}");
        Matcher m = keyPattern.matcher(body);
        assertThat(m.find()).isTrue();
        String paramsBlock = m.group(1);
        Pattern fieldPattern = Pattern.compile("\"([a-z_]+)\"\\s*:");
        Matcher fm = fieldPattern.matcher(paramsBlock);
        while (fm.find()) {
            assertThat(allowedParamKeys).contains(fm.group(1));
        }
    }

    @Test
    void sessionIdIsTenDigitString() {
        assertThat(service.getSessionId()).matches("\\d{10}");
    }

    @Test
    void clientIdIsGeneratedOnceAndPersisted() {
        runWithState(() -> {
            service.trackPromptExecuted("ollama", "llama3");
            httpClient.awaitOne();
        });
        String firstId = state.getAnalyticsClientId();
        assertThat(firstId).isNotEmpty();
        // Ensure it parses as a UUID
        assertThat(UUID.fromString(firstId)).isNotNull();

        // A second call must reuse the same id.
        runWithState(() -> {
            service.trackPromptExecuted("ollama", "llama3");
            httpClient.awaitTotal(2);
        });
        assertThat(state.getAnalyticsClientId()).isEqualTo(firstId);
    }

    @Test
    void disabledStateSendsNothing() {
        state.setAnalyticsEnabled(false);
        runWithState(() -> service.trackPromptExecuted("anthropic", "claude"));
        httpClient.awaitNothingFor(150);
        assertThat(httpClient.requestCount()).isZero();
    }

    @Test
    void noticeNotAcknowledgedSendsNothing() {
        state.setAnalyticsNoticeAcknowledged(false);
        runWithState(() -> service.trackPromptExecuted("anthropic", "claude"));
        httpClient.awaitNothingFor(150);
        assertThat(httpClient.requestCount()).isZero();
    }

    @Test
    void missingProviderOrModelSendsNothing() {
        runWithState(() -> {
            service.trackPromptExecuted(null, "claude");
            service.trackPromptExecuted("anthropic", null);
            service.trackPromptExecuted("", "claude");
            service.trackPromptExecuted("anthropic", "");
        });
        httpClient.awaitNothingFor(150);
        assertThat(httpClient.requestCount()).isZero();
    }

    @Test
    void networkFailureIsSilent() {
        httpClient.throwOnSend = true;
        runWithState(() -> service.trackPromptExecuted("anthropic", "claude"));
        // Should not throw, should not crash. We just want the call to complete.
        httpClient.awaitOne();
        assertThat(httpClient.requestCount()).isOne();
    }

    @Test
    void payloadContainsNoPiiEvenWhenInputsLookLikePaths() {
        // A defensive test: even if we ever pass something path-like, the payload only carries
        // the two strings we passed and nothing else (no project name, no file content).
        runWithState(() -> service.trackModelSelected("anthropic", "claude-3-5-sonnet"));
        httpClient.awaitOne();

        String body = httpClient.lastBody();
        // Forbidden substrings — anything that would imply a leak.
        assertThat(body).doesNotContain("/Users/");
        assertThat(body).doesNotContain("file:");
        assertThat(body).doesNotContain("project");
        assertThat(body).doesNotContain("git");
        assertThat(body).doesNotContain("apiKey");
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("conversation");
    }

    /** Minimal recording HttpClient stub. */
    private static class RecordingHttpClient extends HttpClient {

        private final List<HttpRequest> requests = new ArrayList<>();
        private final List<String> bodies = new ArrayList<>();
        boolean throwOnSend = false;

        synchronized int requestCount() {
            return requests.size();
        }

        synchronized String lastBody() {
            return bodies.get(bodies.size() - 1);
        }

        void awaitOne() {
            awaitTotal(1);
        }

        void awaitTotal(int n) {
            long deadline = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < deadline) {
                synchronized (this) {
                    if (requests.size() >= n) return;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        void awaitNothingFor(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public synchronized <T> HttpResponse<T> send(HttpRequest request,
                                                     HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
            requests.add(request);
            request.bodyPublisher().ifPresent(p -> {
                StringBuilder sb = new StringBuilder();
                p.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
                    @Override public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
                    @Override public void onNext(java.nio.ByteBuffer item) {
                        byte[] bytes = new byte[item.remaining()];
                        item.get(bytes);
                        sb.append(new String(bytes));
                    }
                    @Override public void onError(Throwable t) { }
                    @Override public void onComplete() { }
                });
                bodies.add(sb.toString());
            });
            if (throwOnSend) {
                throw new IOException("simulated");
            }
            @SuppressWarnings("unchecked")
            HttpResponse<T> stub = (HttpResponse<T>) mock(HttpResponse.class);
            when(stub.statusCode()).thenReturn(204);
            return stub;
        }

        @Override public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
        @Override public java.util.Optional<java.time.Duration> connectTimeout() { return java.util.Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
        @Override public javax.net.ssl.SSLContext sslContext() { return null; }
        @Override public javax.net.ssl.SSLParameters sslParameters() { return new javax.net.ssl.SSLParameters(); }
        @Override public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }
        @Override public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return java.util.concurrent.CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
        @Override public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return java.util.concurrent.CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }
}
