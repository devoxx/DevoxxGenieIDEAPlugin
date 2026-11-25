package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.TraceEvent;
import com.devoxx.genie.service.trace.TraceScanStatus;
import com.devoxx.genie.service.trace.TraceSettings;
import com.devoxx.genie.service.trace.TruffleHogMode;
import com.devoxx.genie.service.trace.TruffleHogOnFindings;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TraceUploadServiceTest {

    private MockWebServer server;
    private TraceUploadService service;
    private final Map<String, List<StoredTraceEvent>> traces = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        HuggingFaceHubClient client = new HuggingFaceHubClient(new OkHttpClient(), server.url("/").toString());
        service = new TraceUploadService(client, id -> traces.getOrDefault(id, List.of()));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private static Conversation conversation(String id, boolean withMessages) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setTitle("Conversation " + id);
        c.setTimestamp("2026-11-25T10:00");
        List<ChatMessage> messages = new ArrayList<>();
        if (withMessages) {
            messages.add(new ChatMessage(true, "q", null));
            messages.add(new ChatMessage(false, "a", null));
        }
        c.setMessages(messages);
        return c;
    }

    private static TraceSettings trace(TruffleHogMode mode) {
        return new TraceSettings(true, 0, false, List.of(), mode, TruffleHogOnFindings.REDACT, "", false);
    }

    private HfUploadSettings upload() {
        return new HfUploadSettings(new HfDatasetTarget("alice", "traces"), true, "devoxxgenie", true, true, "");
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private void traceFor(String id, TraceScanStatus status) {
        traces.put(id, List.of(new StoredTraceEvent(1, id, "t", TraceEvent.user("hi", null, 1), status)));
    }

    @Test
    void createsMissingDataset_andCommitsOneFilePerConversation() throws Exception {
        traceFor("1", TraceScanStatus.CLEAN);
        server.enqueue(new MockResponse().setResponseCode(404));           // dataset exists?
        server.enqueue(json("{\"url\":\"u\"}"));                            // create
        server.enqueue(json("{\"files\":[{\"path\":\"devoxxgenie/1.jsonl\",\"uploadMode\":\"regular\"},"
                + "{\"path\":\"devoxxgenie/2.jsonl\",\"uploadMode\":\"regular\"}]}"));
        server.enqueue(json("{\"commitOid\":\"c\",\"commitUrl\":\"https://hf/c\"}"));

        TraceUploadService.Outcome outcome = service.upload(
                List.of(conversation("1", true), conversation("2", true)), "tok", upload(),
                trace(TruffleHogMode.AUTO), "proj", () -> false);

        assertThat(outcome.createdDataset()).isTrue();
        assertThat(outcome.uploadedPaths()).containsExactly("devoxxgenie/1.jsonl", "devoxxgenie/2.jsonl");
        assertThat(outcome.heldBack()).isEmpty();
        assertThat(outcome.firstFileUrl()).endsWith("/datasets/alice/traces/blob/main/devoxxgenie/1.jsonl");
        assertThat(outcome.commitUrl()).isEqualTo("https://hf/c");

        server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest create = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(create.getBody().readUtf8()).contains("\"visibility\":\"private\"");
        server.takeRequest(5, TimeUnit.SECONDS);
        String commit = server.takeRequest(5, TimeUnit.SECONDS).getBody().readUtf8();
        assertThat(commit).contains("Upload 2 DevoxxGenie traces");
    }

    @Test
    void existingDataset_isNotCreatedAgain() throws Exception {
        server.enqueue(json("{}"));
        server.enqueue(json("{\"files\":[]}"));
        server.enqueue(json("{\"commitOid\":\"c\"}"));

        TraceUploadService.Outcome outcome = service.upload(List.of(conversation("1", true)), "tok", upload(),
                trace(TruffleHogMode.OFF), null, () -> false);

        assertThat(outcome.createdDataset()).isFalse();
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void requiredTruffleHog_holdsBackUnscannedTraces_andSkipsTheHubWhenNothingIsLeft() throws Exception {
        traceFor("1", TraceScanStatus.NOT_SCANNED);

        TraceUploadService.Outcome outcome = service.upload(List.of(conversation("1", true)), "tok", upload(),
                trace(TruffleHogMode.REQUIRED), null, () -> false);

        assertThat(outcome.uploadedPaths()).isEmpty();
        assertThat(outcome.heldBack()).singleElement()
                .satisfies(h -> assertThat(h.reason()).contains("without a scan"));
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void emptyConversation_isHeldBack() throws Exception {
        TraceUploadService.Outcome outcome = service.upload(List.of(conversation("1", false)), "tok", upload(),
                trace(TruffleHogMode.OFF), null, () -> false);

        assertThat(outcome.heldBack()).singleElement().satisfies(h -> assertThat(h.reason()).contains("nothing"));
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void cancelledBeforeStart_contactsNothing() throws Exception {
        service.upload(List.of(conversation("1", true)), "tok", upload(), trace(TruffleHogMode.OFF), null, () -> true);
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void filePath_isSanitised() {
        HfUploadSettings s = new HfUploadSettings(null, true, " /a//../b/ ", true, true, "");
        assertThat(s.pathInRepo()).isEqualTo("a/_/b");
        assertThat(s.filePath("17 64/x")).isEqualTo("a/_/b/17_64_x.jsonl");
        assertThat(new HfUploadSettings(null, true, "", true, true, "").filePath("1")).isEqualTo("1.jsonl");
    }
}
