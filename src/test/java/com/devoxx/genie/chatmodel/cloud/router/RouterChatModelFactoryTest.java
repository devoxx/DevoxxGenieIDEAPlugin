package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.chatmodel.catalog.CatalogFields;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Plain JUnit 5: no live network, no sleeps; concurrent cases are ordered with latches. */
class RouterChatModelFactoryTest {

    private static final String BASE = "https://router.test/v1";
    private static final String MODELS = "/models";
    private static final String OTHER = "/other";
    private static final String MODELS_URL = BASE + MODELS;
    private static final String OTHER_URL = BASE + OTHER;
    private static final String CATALOG = catalogOf("a", "b");
    private static final String EMPTY_CATALOG = "{\"data\":[]}";
    private static final long TIMEOUT_SECONDS = 10;

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicReference<String> key = new AtomicReference<>("key-a");
    private final AtomicInteger keyReads = new AtomicInteger();
    private final AtomicLong clock = new AtomicLong(1_000);
    private final AtomicInteger notifications = new AtomicInteger();
    private final FakeClient client = new FakeClient();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    @AfterEach
    void shutdownPool() {
        pool.shutdownNow();
    }

    private static String catalogOf(String... ids) {
        StringBuilder json = new StringBuilder("{\"data\":[");
        for (int i = 0; i < ids.length; i++) {
            json.append(i == 0 ? "" : ",").append("{\"id\":\"").append(ids[i]).append("\"}");
        }
        return json.append("]}").toString();
    }

    private static RouterDefinition def(String baseUrl, Integer maxTokens, String... endpoints) {
        return new RouterDefinition(ModelProvider.OpenRouter, "Test Router", baseUrl, List.of(endpoints),
                "subKey", "https://router.test/keys", null, null, maxTokens);
    }

    private RouterChatModelFactory factory(String... endpoints) {
        return factoryFor(def(BASE + "/", null, endpoints), client);
    }

    private RouterChatModelFactory factoryFor(RouterDefinition definition, RouterCatalogClient catalogClient) {
        return new RouterChatModelFactory(definition, enabled::get, () -> {
            keyReads.incrementAndGet();
            return key.get();
        }, catalogClient, clock::get) {
            @Override
            protected void notifyFetchFailure() {
                notifications.incrementAndGet();
            }
        };
    }

    private static List<String> ids(List<LanguageModel> models) {
        return models.stream().map(LanguageModel::getModelName).toList();
    }

    private Future<List<LanguageModel>> loadOnPool(RouterChatModelFactory factory) {
        return pool.submit(() -> factory.getModels());
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("latch released in time").isTrue();
    }

    // --- base URL ---

    @Test
    void getModels_fetchesFromTheConfiguredBaseUrl_notTheDefinitionDefault() {
        String override = "https://eu.router.test/v1";
        client.respond(override + MODELS, CATALOG);
        RouterChatModelFactory factory = new RouterChatModelFactory(def(BASE + "/", null, MODELS),
                enabled::get, key::get, () -> override, client, clock::get);

        assertThat(ids(factory.getModels())).containsExactly("a", "b");
        assertThat(client.calls).extracting(Call::url).containsExactly(override + MODELS);
    }

    // --- caching ---

    @Test
    void getCachedModels_neverFetches() {
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);

        assertThat(factory.getCachedModels()).isEmpty();

        assertThat(client.calls).isEmpty();
        assertThat(keyReads).hasValue(0);
    }

    @Test
    void getCachedModels_returnsWhatIsLoaded_withoutReadingTheKeyAgain() {
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);
        factory.getModels();
        keyReads.set(0);

        assertThat(ids(factory.getCachedModels())).containsExactly("a", "b");

        assertThat(keyReads).hasValue(0);
        assertThat(client.calls).hasSize(1);
    }

    @Test
    void success_isCached() {
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);

        assertThat(ids(factory.getModels())).containsExactly("a", "b");
        assertThat(ids(factory.getModels())).containsExactly("a", "b");

        assertThat(client.calls).hasSize(1);
        assertThat(keyReads).as("a cache hit reads no credentials").hasValue(1);
    }

    @Test
    void joinsUrl_withoutDoubleSlash_andSendsTheKey() {
        client.respond(MODELS_URL, CATALOG);

        factory(MODELS).getModels();

        assertThat(client.calls).containsExactly(new Call(MODELS_URL, "key-a"));
    }

    @Test
    void joinsUrl_whenBaseUrlHasNoTrailingSlash() {
        client.respond(MODELS_URL, CATALOG);

        factoryFor(def(BASE, null, MODELS), client).getModels();

        assertThat(client.calls).containsExactly(new Call(MODELS_URL, "key-a"));
    }

    @Test
    void partialFailure_cachesSuccessfulEndpoint() {
        client.fail(MODELS_URL, new IOException("HTTP 500"));
        client.respond(OTHER_URL, catalogOf("c"));
        RouterChatModelFactory factory = factory(MODELS, OTHER);

        assertThat(ids(factory.getModels())).containsExactly("c");
        assertThat(ids(factory.getModels())).containsExactly("c");

        assertThat(client.calls).hasSize(2);
        assertThat(notifications).as("a partial result is not a failure").hasValue(0);
    }

    @Test
    void merge_firstEndpointWinsById() {
        client.respond(MODELS_URL, "{\"data\":[{\"id\":\"same\",\"name\":\"first\"},{\"id\":\"a\"}]}");
        client.respond(OTHER_URL, "{\"data\":[{\"id\":\"same\",\"name\":\"second\"},{\"id\":\"b\"}]}");
        CatalogFields fields = new CatalogFields("name", List.of(), null, null, 1, null, null);
        RouterDefinition definition = new RouterDefinition(ModelProvider.OpenRouter, "Test Router", BASE,
                List.of(MODELS, OTHER), "subKey", "https://router.test/keys", null, fields, null);

        List<LanguageModel> models = factoryFor(definition, client).getModels();

        assertThat(ids(models)).containsExactly("same", "a", "b");
        assertThat(models.get(0).getDisplayName()).isEqualTo("first");
    }

    // --- backoff ---

    @Test
    void totalFailure_backsOffFor60Seconds() {
        client.fail(MODELS_URL, new IOException("HTTP 500"));
        RouterChatModelFactory factory = factory(MODELS);

        assertThat(factory.getModels()).isEmpty();
        assertThat(client.calls).hasSize(1);
        assertThat(notifications).hasValue(1);

        clock.addAndGet(59_999);
        keyReads.set(0);
        assertThat(factory.getModels()).isEmpty();
        assertThat(client.calls).hasSize(1);
        assertThat(keyReads).as("a backed-off call reads no credentials").hasValue(0);
        assertThat(notifications).as("a backed-off call doesn't notify again").hasValue(1);

        clock.addAndGet(1);
        assertThat(factory.getModels()).isEmpty();
        assertThat(client.calls).hasSize(2);
        assertThat(notifications).as("a retry that fails again notifies again").hasValue(2);
    }

    @Test
    void emptyCatalog_backsOffFor60Seconds_withoutClaimingTheRouterIsUnreachable() {
        client.respond(MODELS_URL, EMPTY_CATALOG);
        RouterChatModelFactory factory = factory(MODELS);

        assertThat(factory.getModels()).isEmpty();
        clock.addAndGet(59_999);
        assertThat(factory.getModels()).isEmpty();
        assertThat(client.calls).hasSize(1);

        clock.addAndGet(1);
        client.respond(MODELS_URL, CATALOG);
        assertThat(ids(factory.getModels())).containsExactly("a", "b");
        assertThat(client.calls).hasSize(2);
        assertThat(notifications).hasValue(0);
    }

    @Test
    void recoveryAfterBackoff_isCached() {
        client.fail(MODELS_URL, new IOException("HTTP 500"));
        RouterChatModelFactory factory = factory(MODELS);
        factory.getModels();

        clock.addAndGet(60_000);
        client.respond(MODELS_URL, CATALOG);

        assertThat(ids(factory.getModels())).containsExactly("a", "b");
        assertThat(ids(factory.getModels())).containsExactly("a", "b");
        assertThat(client.calls).hasSize(2);
    }

    @Test
    void resetModels_clearsBackoffAndCache() {
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);
        factory.getModels();

        factory.resetModels();
        assertThat(factory.getCachedModels()).isEmpty();

        client.fail(MODELS_URL, new IOException("HTTP 500"));
        factory.getModels();
        assertThat(client.calls).hasSize(2);

        factory.resetModels();
        client.respond(MODELS_URL, catalogOf("fresh"));
        assertThat(ids(factory.getModels())).containsExactly("fresh");
        assertThat(client.calls).hasSize(3);
    }

    // --- keys ---

    @Test
    void noKey_sendsNoRequest() {
        key.set("");
        RouterChatModelFactory factory = factory(MODELS);

        assertThat(factory.getModels()).isEmpty();

        assertThat(client.calls).isEmpty();
        assertThat(notifications).hasValue(0);
    }

    @Test
    void nullKey_sendsNoRequest() {
        key.set(null);

        assertThat(factory(MODELS).getModels()).isEmpty();

        assertThat(client.calls).isEmpty();
    }

    @Test
    void whitespaceKey_sendsNoRequest() {
        key.set(" \t\n ");

        assertThat(factory(MODELS).getModels()).isEmpty();

        assertThat(client.calls).isEmpty();
    }

    @Test
    void noKey_recordsNoBackoff_soAKeyAddedLaterFetchesAtOnce() {
        key.set("");
        RouterChatModelFactory factory = factory(MODELS);
        factory.getModels();

        key.set("key-a");
        client.respond(MODELS_URL, CATALOG);

        assertThat(ids(factory.getModels())).containsExactly("a", "b");
    }

    @Test
    void key_isTrimmed() {
        key.set("  key-a\n");
        client.respond(MODELS_URL, CATALOG);

        factory(MODELS).getModels();

        assertThat(client.calls).containsExactly(new Call(MODELS_URL, "key-a"));
    }

    // --- disabled ---

    @Test
    void disabled_readsNoKeyAndSendsNoRequest() {
        enabled.set(false);
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);

        assertThat(factory.getModels()).isEmpty();

        assertThat(keyReads).hasValue(0);
        assertThat(client.calls).isEmpty();
        assertThat(notifications).hasValue(0);

        enabled.set(true);
        assertThat(ids(factory.getModels())).as("no backoff was recorded").containsExactly("a", "b");
    }

    @Test
    void queuedLoad_executedAfterDisabling_sendsNoRequest() {
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);
        List<Runnable> heldExecutor = new ArrayList<>();
        FutureTask<List<LanguageModel>> load = new FutureTask<>(() -> factory.getModels());
        heldExecutor.add(load);

        enabled.set(false);
        heldExecutor.forEach(Runnable::run);

        assertThat(load.isDone()).isTrue();
        assertThat(keyReads).hasValue(0);
        assertThat(client.calls).isEmpty();
    }

    @Test
    void disabled_keepsCachedModelsOutOfGetModels() {
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);
        assertThat(factory.getModels()).hasSize(2);

        enabled.set(false);

        assertThat(factory.getModels()).isEmpty();
    }

    // --- bad responses (Review Focus 1) ---

    @Test
    void html200_and_401_returnEmptyAndNotifyOnce() throws Exception {
        String secret = "sk-secret-value";
        key.set(secret);
        HttpServer server = loopbackServer();
        respondWith(server, "/html", 200, "<html><body>Welcome " + secret + "</body></html>");
        respondWith(server, "/unauthorized", 401, "{\"error\":\"bad key " + secret + "\"}");
        respondWith(server, "/invalid", 200, "{not json " + secret);
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            RouterCatalogClient real = new RouterCatalogClient.OkHttp();
            for (String path : List.of("/html", "/unauthorized", "/invalid")) {
                notifications.set(0);

                List<LanguageModel> models = factoryFor(def(base, null, path), real).getModels();

                assertThat(models).as(path).isEmpty();
                assertThat(notifications).as(path).hasValue(1);
            }

            assertThatThrownBy(() -> real.fetch(base + "/unauthorized", secret))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("401")
                    .message().doesNotContain(secret);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void multipleFailedEndpoints_notifyOnce() {
        client.fail(MODELS_URL, new IOException("HTTP 500"));
        client.fail(OTHER_URL, new IOException("HTTP 500"));

        assertThat(factory(MODELS, OTHER).getModels()).isEmpty();

        assertThat(client.calls).hasSize(2);
        assertThat(notifications).hasValue(1);
    }

    @Test
    void keyThatCannotBeAHeader_doesNotThrowOrLeak() {
        String secret = "sk-line\nbreak";
        key.set(secret);
        RouterCatalogClient real = new RouterCatalogClient.OkHttp();

        assertThat(factoryFor(def(BASE, null, MODELS), real).getModels()).isEmpty();

        assertThat(notifications).hasValue(1);
        assertThatThrownBy(() -> real.fetch(MODELS_URL, secret))
                .isInstanceOf(IllegalArgumentException.class)
                .message().doesNotContain("sk-line");
    }

    @Test
    void safeMessage_scrubsTheKey() {
        String message = RouterChatModelFactory.safeMessage(new IOException("rejected key-a today"), "key-a");

        assertThat(message).contains("IOException").contains("rejected").doesNotContain("key-a");
    }

    // --- stale results ---

    @Test
    void staleFetch_doesNotRepopulate() throws Exception {
        BlockingCall blocked = new BlockingCall(CATALOG);
        client.handler = blocked;
        RouterChatModelFactory factory = factory(MODELS);

        Future<List<LanguageModel>> stale = loadOnPool(factory);
        await(blocked.started);
        factory.resetModels();
        key.set("key-b");
        blocked.release.countDown();

        assertThat(await(stale)).as("the stale caller gets the current snapshot").isEmpty();
        assertThat(factory.getCachedModels()).as("the key A result was dropped").isEmpty();

        client.handler = null;
        client.respond(MODELS_URL, catalogOf("b-only"));
        assertThat(ids(factory.getModels())).containsExactly("b-only");
        assertThat(client.calls).containsExactly(new Call(MODELS_URL, "key-a"), new Call(MODELS_URL, "key-b"));
    }

    @Test
    void staleFetch_finishingAfterANewerFetch_returnsTheNewerSnapshot() throws Exception {
        BlockingCall blocked = new BlockingCall(CATALOG);
        client.handler = blocked;
        RouterChatModelFactory factory = factory(MODELS);

        Future<List<LanguageModel>> stale = loadOnPool(factory);
        await(blocked.started);
        factory.resetModels();
        key.set("key-b");
        client.handler = null;
        client.respond(MODELS_URL, catalogOf("b-only"));
        assertThat(ids(factory.getModels())).containsExactly("b-only");
        blocked.release.countDown();

        assertThat(ids(await(stale))).containsExactly("b-only");
        assertThat(ids(factory.getCachedModels())).containsExactly("b-only");
    }

    @Test
    void staleFailure_doesNotStartBackoff() throws Exception {
        BlockingCall blocked = new BlockingCall(new IOException("HTTP 500"));
        client.handler = blocked;
        RouterChatModelFactory factory = factory(MODELS);

        Future<List<LanguageModel>> stale = loadOnPool(factory);
        await(blocked.started);
        factory.resetModels();
        blocked.release.countDown();

        assertThat(await(stale)).isEmpty();
        assertThat(notifications).as("a stale failure isn't reported").hasValue(0);

        client.handler = null;
        client.respond(MODELS_URL, CATALOG);
        assertThat(ids(factory.getModels())).as("no backoff was recorded").containsExactly("a", "b");
    }

    @Test
    void resetDuringFetch_stopsRequestingRemainingEndpoints() throws Exception {
        BlockingCall blocked = new BlockingCall(CATALOG);
        client.handler = blocked;
        RouterChatModelFactory factory = factory(MODELS, OTHER);

        Future<List<LanguageModel>> stale = loadOnPool(factory);
        await(blocked.started);
        factory.resetModels();
        blocked.release.countDown();
        await(stale);

        assertThat(client.calls).containsExactly(new Call(MODELS_URL, "key-a"));
    }

    @Test
    void resetRacingTheKeyRead_cannotPublishTheOldKeysCatalog() {
        // A reset landing right after the generation capture but before the key read: the fetch
        // sees the new key, and its result is still dropped because its generation is old.
        client.respond(MODELS_URL, CATALOG);
        AtomicReference<RouterChatModelFactory> self = new AtomicReference<>();
        RouterChatModelFactory factory = new RouterChatModelFactory(def(BASE, null, MODELS), enabled::get, () -> {
            self.get().resetModels();
            return "key-b";
        }, client, clock::get) {
            @Override
            protected void notifyFetchFailure() {
                notifications.incrementAndGet();
            }
        };
        self.set(factory);

        assertThat(factory.getModels()).isEmpty();

        assertThat(factory.getCachedModels()).isEmpty();
    }

    // --- concurrency ---

    @Test
    void concurrentFailures_notifyOnce() throws Exception {
        CountDownLatch bothInFlight = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        client.handler = (url, apiKey) -> {
            bothInFlight.countDown();
            await(release);
            throw new IOException("HTTP 500");
        };
        RouterChatModelFactory factory = factory(MODELS);

        Future<List<LanguageModel>> first = loadOnPool(factory);
        Future<List<LanguageModel>> second = loadOnPool(factory);
        await(bothInFlight);
        release.countDown();

        assertThat(await(first)).isEmpty();
        assertThat(await(second)).isEmpty();
        assertThat(notifications).hasValue(1);
    }

    @Test
    void failureFinishingAfterAConcurrentSuccess_keepsTheCatalog() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger callIndex = new AtomicInteger();
        client.handler = (url, apiKey) -> {
            if (callIndex.getAndIncrement() == 0) {
                firstStarted.countDown();
                await(releaseFirst);
                return CATALOG;
            }
            secondStarted.countDown();
            await(releaseSecond);
            throw new IOException("HTTP 500");
        };
        RouterChatModelFactory factory = factory(MODELS);

        Future<List<LanguageModel>> succeeding = loadOnPool(factory);
        await(firstStarted);
        Future<List<LanguageModel>> failing = loadOnPool(factory);
        await(secondStarted);
        releaseFirst.countDown();
        assertThat(ids(await(succeeding))).containsExactly("a", "b");
        releaseSecond.countDown();

        assertThat(ids(await(failing))).as("the loser sees the published catalog").containsExactly("a", "b");
        assertThat(notifications).hasValue(0);
        assertThat(ids(factory.getModels())).containsExactly("a", "b");
        assertThat(callIndex).as("no retry: the catalog stayed cached").hasValue(2);
    }

    @Test
    void fetchRunsWithoutHoldingTheLock_soCachedReadsDoNotBlock() throws Exception {
        BlockingCall blocked = new BlockingCall(CATALOG);
        client.handler = blocked;
        RouterChatModelFactory factory = factory(MODELS);

        Future<List<LanguageModel>> load = loadOnPool(factory);
        await(blocked.started);

        assertThat(await(pool.submit(factory::getCachedModels))).isEmpty();
        await(pool.submit(factory::resetModels));

        blocked.release.countDown();
        await(load);
    }

    @Test
    void concurrentReaders_seeCompleteSnapshots() throws Exception {
        int size = 500;
        String[] catalogIds = IntStream.range(0, size).mapToObj(i -> "m" + i).toArray(String[]::new);
        String bigCatalog = catalogOf(catalogIds);
        client.handler = (url, apiKey) -> bigCatalog;
        RouterChatModelFactory factory = factory(MODELS);

        List<Integer> seenSizes = new CopyOnWriteArrayList<>();
        CountDownLatch readersReady = new CountDownLatch(4);
        AtomicBoolean done = new AtomicBoolean();
        List<Future<?>> readers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            readers.add(pool.submit(() -> {
                readersReady.countDown();
                while (!done.get()) {
                    int observed = factory.getCachedModels().size();
                    if (observed != 0 && observed != size) {
                        seenSizes.add(observed);
                    }
                }
            }));
        }
        await(readersReady);
        for (int round = 0; round < 200; round++) {
            assertThat(factory.getModels()).hasSize(size);
            factory.resetModels();
        }
        done.set(true);
        for (Future<?> reader : readers) {
            await(reader);
        }

        assertThat(seenSizes).as("sizes other than empty or complete").isEmpty();
    }

    @Test
    void nothingIsPublishedUntilEveryEndpointFinished() {
        AtomicReference<RouterChatModelFactory> self = new AtomicReference<>();
        List<Integer> seenDuringFetch = new ArrayList<>();
        client.handler = (url, apiKey) -> {
            seenDuringFetch.add(self.get().getCachedModels().size());
            return url.equals(MODELS_URL) ? catalogOf("a") : catalogOf("b");
        };
        RouterChatModelFactory factory = factory(MODELS, OTHER);
        self.set(factory);

        assertThat(factory.getModels()).hasSize(2);

        assertThat(seenDuringFetch).containsExactly(0, 0);
    }

    @Test
    void returnedLists_areUnmodifiable_andStableAcrossReset() {
        client.respond(MODELS_URL, CATALOG);
        RouterChatModelFactory factory = factory(MODELS);
        LanguageModel extra = LanguageModel.builder().modelName("x").build();

        List<LanguageModel> loaded = factory.getModels();
        List<LanguageModel> cached = factory.getCachedModels();
        assertThatThrownBy(() -> loaded.add(extra)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> cached.add(extra)).isInstanceOf(UnsupportedOperationException.class);

        factory.resetModels();
        assertThat(ids(loaded)).containsExactly("a", "b");
        assertThatThrownBy(() -> factory.getCachedModels().add(extra))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- chat models ---

    @Test
    void chatAndStreaming_callBaseUrlWithBearerKey() throws Exception {
        key.set("  chat-key ");
        Queue<CapturedRequest> requests = new ConcurrentLinkedQueue<>();
        HttpServer server = loopbackServer();
        server.createContext("/", exchange -> {
            // langchain4j pretty-prints its JSON
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s", "");
            requests.add(new CapturedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders(), body));
            boolean streaming = body.contains("\"stream\":true");
            String reply = streaming ? STREAM_REPLY : CHAT_REPLY;
            exchange.getResponseHeaders().add("Content-Type", streaming ? "text/event-stream" : "application/json");
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try (MockedStatic<DevoxxGenieStateService> state = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<MCPService> mcp = mockStatic(MCPService.class)) {
            state.when(DevoxxGenieStateService::getInstance).thenReturn(mock(DevoxxGenieStateService.class));
            mcp.when(MCPService::isMCPEnabled).thenReturn(false);

            String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
            RouterChatModelFactory factory = factoryFor(def(base, null, MODELS), client);

            ChatModel chat = factory.createChatModel(customChatModel());
            assertThat(chat.chat("hello")).isEqualTo("hi");

            StreamingChatModel streaming = factory.createStreamingChatModel(customChatModel());
            CompletableFuture<ChatResponse> completed = new CompletableFuture<>();
            streaming.chat("hello", new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    // only the completed response matters here
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    completed.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    completed.completeExceptionally(error);
                }
            });
            assertThat(await(completed).aiMessage().text()).isEqualTo("hi");
        } finally {
            server.stop(0);
        }

        assertThat(requests).hasSize(2);
        for (CapturedRequest request : requests) {
            assertThat(request.method).isEqualTo("POST");
            assertThat(request.path).isEqualTo("/chat/completions");
            assertThat(request.headers.getFirst("Authorization")).isEqualTo("Bearer chat-key");
            assertThat(request.headers).doesNotContainKeys("Http-referer", "X-title", "X-openrouter-title",
                    "HTTP-Referer", "X-Title");
            assertThat(request.body).contains("\"model\":\"test-model\"");
        }
    }

    @Test
    void maxTokens_fixedByDefinition_elseFromCustomChatModel() {
        try (MockedStatic<DevoxxGenieStateService> state = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<MCPService> mcp = mockStatic(MCPService.class)) {
            state.when(DevoxxGenieStateService::getInstance).thenReturn(mock(DevoxxGenieStateService.class));
            mcp.when(MCPService::isMCPEnabled).thenReturn(false);

            RouterChatModelFactory fixed = factoryFor(def(BASE, 4000, MODELS), client);
            RouterChatModelFactory fromSettings = factoryFor(def(BASE, null, MODELS), client);

            assertThat(fixed.createChatModel(customChatModel()).defaultRequestParameters().maxOutputTokens())
                    .isEqualTo(4000);
            assertThat(fixed.createStreamingChatModel(customChatModel()).defaultRequestParameters().maxOutputTokens())
                    .isEqualTo(4000);
            assertThat(fromSettings.createChatModel(customChatModel()).defaultRequestParameters().maxOutputTokens())
                    .isEqualTo(1234);
            assertThat(fromSettings.createStreamingChatModel(customChatModel()).defaultRequestParameters().maxOutputTokens())
                    .isEqualTo(1234);
        }
    }

    // --- defaults ---

    @Test
    void defaultConstructor_usesEnabledStateAndProviderKey() {
        RouterDefinition definition = def(BASE, null, MODELS);
        DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
        LLMProviderService providerService = mock(LLMProviderService.class);
        when(providerService.getApiKey(ModelProvider.OpenRouter)).thenReturn("");

        try (MockedStatic<DevoxxGenieStateService> state = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<LLMProviderService> providers = mockStatic(LLMProviderService.class)) {
            state.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            providers.when(LLMProviderService::getInstance).thenReturn(providerService);

            RouterChatModelFactory factory = new RouterChatModelFactory(definition);

            when(stateService.isRouterEnabled(ModelProvider.OpenRouter)).thenReturn(false);
            assertThat(factory.getModels()).isEmpty();
            verifyNoInteractions(providerService);

            when(stateService.isRouterEnabled(ModelProvider.OpenRouter)).thenReturn(true);
            assertThat(factory.getModels()).isEmpty();
            verify(providerService).getApiKey(ModelProvider.OpenRouter);
        }
    }

    @Test
    void defaultConstructor_touchesNoPlatformServices() {
        try (MockedStatic<DevoxxGenieStateService> state = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<LLMProviderService> providers = mockStatic(LLMProviderService.class)) {
            RouterChatModelFactory factory = new RouterChatModelFactory(def(BASE, null, MODELS));

            assertThat(factory.getCachedModels()).isEmpty();

            state.verifyNoInteractions();
            providers.verifyNoInteractions();
        }
    }

    @Test
    void defaultNotification_namesTheRouter() {
        ProjectManager projectManager = mock(ProjectManager.class);
        Project project = mock(Project.class);
        when(projectManager.getDefaultProject()).thenReturn(project);

        try (MockedStatic<ProjectManager> manager = mockStatic(ProjectManager.class);
             MockedStatic<NotificationUtil> notification = mockStatic(NotificationUtil.class)) {
            manager.when(ProjectManager::getInstance).thenReturn(projectManager);

            new RouterChatModelFactory(def(BASE, null, MODELS)).notifyFetchFailure();

            notification.verify(() -> NotificationUtil.sendNotification(project,
                    "Unable to reach Test Router, please try again later."), times(1));
        }
    }

    // --- helpers ---

    private static final String CHAT_REPLY = """
            {"id":"1","object":"chat.completion","created":1,"model":"test-model",
             "choices":[{"index":0,"message":{"role":"assistant","content":"hi"},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}""";

    private static final String STREAM_REPLY = """
            data: {"id":"1","object":"chat.completion.chunk","created":1,"model":"test-model","choices":[{"index":0,"delta":{"role":"assistant","content":"hi"},"finish_reason":null}]}

            data: {"id":"1","object":"chat.completion.chunk","created":1,"model":"test-model","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

            data: [DONE]

            """;

    private static CustomChatModel customChatModel() {
        CustomChatModel model = mock(CustomChatModel.class);
        when(model.getModelName()).thenReturn("test-model");
        when(model.getTemperature()).thenReturn(0.5);
        when(model.getTopP()).thenReturn(0.9);
        when(model.getMaxTokens()).thenReturn(1234);
        when(model.getMaxRetries()).thenReturn(0);
        when(model.getTimeout()).thenReturn(10);
        return model;
    }

    private static HttpServer loopbackServer() throws IOException {
        return HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    }

    private static void respondWith(HttpServer server, String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    private record CapturedRequest(String method, String path, Headers headers, String body) {
    }

    private record Call(String url, String key) {
    }

    /** What the fake client does for one call; may block on a latch. */
    private interface Handler {
        String handle(String url, String apiKey) throws IOException, InterruptedException;
    }

    /** Blocks the first call until released, then behaves as configured; later calls never block. */
    private static final class BlockingCall implements Handler {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        private final String response;
        private final IOException failure;

        BlockingCall(String response) {
            this.response = response;
            this.failure = null;
        }

        BlockingCall(IOException failure) {
            this.response = null;
            this.failure = failure;
        }

        @Override
        public String handle(String url, String apiKey) throws IOException, InterruptedException {
            started.countDown();
            await(release);
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }

    private static final class FakeClient implements RouterCatalogClient {
        final List<Call> calls = new CopyOnWriteArrayList<>();
        private final Map<String, Object> outcomes = new HashMap<>();
        volatile Handler handler;

        void respond(String url, String body) {
            outcomes.put(url, body);
        }

        void fail(String url, IOException failure) {
            outcomes.put(url, failure);
        }

        @Override
        public String fetch(String url, String apiKey) throws IOException {
            calls.add(new Call(url, apiKey));
            Handler current = handler;
            if (current != null) {
                try {
                    return current.handle(url, apiKey);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
            Object outcome = outcomes.get(url);
            if (outcome instanceof IOException failure) {
                throw failure;
            }
            if (outcome == null) {
                throw new IOException("no outcome configured for " + url);
            }
            return (String) outcome;
        }
    }
}
