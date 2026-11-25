package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.service.trace.hf.HuggingFaceHubClient.UploadFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HuggingFaceHubClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TOKEN = "test-token-not-real";
    private static final HfDatasetTarget TARGET = new HfDatasetTarget("alice", "traces");

    private MockWebServer server;
    private HuggingFaceHubClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new HuggingFaceHubClient(new OkHttpClient(), server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }

    private RecordedRequest take() throws InterruptedException {
        RecordedRequest r = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(r).isNotNull();
        return r;
    }

    @Test
    void whoAmI_returnsUserName_andSendsBearerToken() throws Exception {
        server.enqueue(json(200, "{\"name\":\"alice\",\"type\":\"user\"}"));

        assertThat(client.whoAmI(TOKEN)).isEqualTo("alice");

        RecordedRequest r = take();
        assertThat(r.getPath()).isEqualTo("/api/whoami-v2");
        assertThat(r.getHeader("Authorization")).isEqualTo("Bearer " + TOKEN);
    }

    @Test
    void datasetExists_trueOn200_falseOn404And401() throws Exception {
        server.enqueue(json(200, "{}"));
        server.enqueue(json(404, "{\"error\":\"Repository not found\"}"));
        server.enqueue(json(401, "{}"));

        assertThat(client.datasetExists(TARGET, TOKEN)).isTrue();
        assertThat(client.datasetExists(TARGET, TOKEN)).isFalse();
        assertThat(client.datasetExists(TARGET, TOKEN)).isFalse();
        assertThat(take().getPath()).isEqualTo("/api/datasets/alice/traces");
    }

    @Test
    void createDataset_postsNameTypeOrganizationAndVisibility() throws Exception {
        server.enqueue(json(200, "{\"url\":\"https://huggingface.co/datasets/alice/traces\"}"));

        client.createDataset(TARGET, TOKEN, true);

        RecordedRequest r = take();
        assertThat(r.getMethod()).isEqualTo("POST");
        assertThat(r.getPath()).isEqualTo("/api/repos/create");
        JsonNode body = JSON.readTree(r.getBody().readUtf8());
        assertThat(body.get("name").asText()).isEqualTo("traces");
        assertThat(body.get("type").asText()).isEqualTo("dataset");
        assertThat(body.get("organization").asText()).isEqualTo("alice");
        assertThat(body.get("visibility").asText()).isEqualTo("private");
    }

    @Test
    void commit_regularFile_isSentInlineAsBase64() throws Exception {
        server.enqueue(json(200, "{\"files\":[{\"path\":\"devoxxgenie/1.jsonl\",\"uploadMode\":\"regular\"}]}"));
        server.enqueue(json(200, "{\"commitOid\":\"abc\",\"commitUrl\":\"https://hf/commit/abc\"}"));
        byte[] content = "{\"type\":\"session\"}\n".getBytes(StandardCharsets.UTF_8);

        HuggingFaceHubClient.CommitResult result = client.commit(TARGET, TOKEN,
                List.of(new UploadFile("devoxxgenie/1.jsonl", content)), "Upload trace");

        assertThat(result.commitOid()).isEqualTo("abc");
        RecordedRequest preupload = take();
        assertThat(preupload.getPath()).isEqualTo("/api/datasets/alice/traces/preupload/main");
        JsonNode pre = JSON.readTree(preupload.getBody().readUtf8()).get("files").get(0);
        assertThat(pre.get("path").asText()).isEqualTo("devoxxgenie/1.jsonl");
        assertThat(pre.get("size").asInt()).isEqualTo(content.length);
        assertThat(Base64.getDecoder().decode(pre.get("sample").asText())).isEqualTo(content);

        RecordedRequest commit = take();
        assertThat(commit.getPath()).isEqualTo("/api/datasets/alice/traces/commit/main");
        assertThat(commit.getHeader("Content-Type")).startsWith("application/x-ndjson");
        String[] lines = commit.getBody().readUtf8().split("\n");
        JsonNode header = JSON.readTree(lines[0]);
        assertThat(header.get("key").asText()).isEqualTo("header");
        assertThat(header.get("value").get("summary").asText()).isEqualTo("Upload trace");
        JsonNode file = JSON.readTree(lines[1]);
        assertThat(file.get("key").asText()).isEqualTo("file");
        assertThat(file.get("value").get("encoding").asText()).isEqualTo("base64");
        assertThat(Base64.getDecoder().decode(file.get("value").get("content").asText())).isEqualTo(content);
    }

    @Test
    void commit_lfsFile_goesThroughBatchAndPut_thenCommitsAPointer() throws Exception {
        byte[] content = "x".repeat(2000).getBytes(StandardCharsets.UTF_8);
        String oid = HuggingFaceHubClient.sha256(content);
        String putUrl = server.url("/s3/upload").toString();
        server.enqueue(json(200, "{\"files\":[{\"path\":\"t/big.jsonl\",\"uploadMode\":\"lfs\"}]}"));
        server.enqueue(json(200, "{\"transfer\":\"basic\",\"objects\":[{\"oid\":\"" + oid + "\",\"size\":2000,"
                + "\"actions\":{\"upload\":{\"href\":\"" + putUrl + "\"}}}]}"));
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(json(200, "{\"commitOid\":\"def\"}"));

        client.commit(TARGET, TOKEN, List.of(new UploadFile("t/big.jsonl", content)), "big");

        take(); // preupload
        RecordedRequest batch = take();
        assertThat(batch.getPath()).isEqualTo("/datasets/alice/traces.git/info/lfs/objects/batch");
        assertThat(batch.getHeader("Accept")).isEqualTo("application/vnd.git-lfs+json");
        JsonNode batchBody = JSON.readTree(batch.getBody().readUtf8());
        assertThat(batchBody.get("operation").asText()).isEqualTo("upload");
        assertThat(batchBody.get("transfers").toString()).isEqualTo("[\"basic\",\"multipart\"]");
        assertThat(batchBody.get("objects").get(0).get("oid").asText()).isEqualTo(oid);

        RecordedRequest put = take();
        assertThat(put.getMethod()).isEqualTo("PUT");
        assertThat(put.getPath()).isEqualTo("/s3/upload");
        assertThat(put.getHeader("Authorization")).isNull(); // presigned URL: never send the token there
        assertThat(put.getBody().readByteArray()).isEqualTo(content);

        String[] lines = take().getBody().readUtf8().split("\n");
        JsonNode pointer = JSON.readTree(lines[1]);
        assertThat(pointer.get("key").asText()).isEqualTo("lfsFile");
        assertThat(pointer.get("value").get("oid").asText()).isEqualTo(oid);
        assertThat(pointer.get("value").get("algo").asText()).isEqualTo("sha256");
        assertThat(pointer.get("value").get("size").asInt()).isEqualTo(2000);
    }

    @Test
    void commit_lfsObjectAlreadyStored_skipsTheUpload() throws Exception {
        byte[] content = "y".repeat(10).getBytes(StandardCharsets.UTF_8);
        String oid = HuggingFaceHubClient.sha256(content);
        server.enqueue(json(200, "{\"files\":[{\"path\":\"a\",\"uploadMode\":\"lfs\"}]}"));
        server.enqueue(json(200, "{\"objects\":[{\"oid\":\"" + oid + "\",\"size\":10}]}"));
        server.enqueue(json(200, "{\"commitOid\":\"x\"}"));

        client.commit(TARGET, TOKEN, List.of(new UploadFile("a", content)), "t");

        take();
        take();
        assertThat(take().getPath()).isEqualTo("/api/datasets/alice/traces/commit/main");
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void commit_multipartUpload_putsEachPart_andCompletesWithEtags() throws Exception {
        byte[] content = "abcdefghij".getBytes(StandardCharsets.UTF_8); // 10 bytes, chunk 4 -> 3 parts
        String oid = HuggingFaceHubClient.sha256(content);
        String p1 = server.url("/part/1").toString();
        String p2 = server.url("/part/2").toString();
        String p3 = server.url("/part/3").toString();
        String complete = server.url("/complete").toString();
        server.enqueue(json(200, "{\"files\":[{\"path\":\"m\",\"uploadMode\":\"lfs\"}]}"));
        server.enqueue(json(200, "{\"objects\":[{\"oid\":\"" + oid + "\",\"size\":10,\"actions\":{\"upload\":{\"href\":\""
                + complete + "\",\"header\":{\"chunk_size\":\"4\",\"00001\":\"" + p1 + "\",\"00002\":\"" + p2
                + "\",\"00003\":\"" + p3 + "\"}}}}]}"));
        server.enqueue(new MockResponse().setHeader("ETag", "e1"));
        server.enqueue(new MockResponse().setHeader("ETag", "e2"));
        server.enqueue(new MockResponse().setHeader("ETag", "e3"));
        server.enqueue(json(200, "{}"));
        server.enqueue(json(200, "{\"commitOid\":\"m\"}"));

        client.commit(TARGET, TOKEN, List.of(new UploadFile("m", content)), "t");

        take();
        take();
        assertThat(take().getBody().readUtf8()).isEqualTo("abcd");
        assertThat(take().getBody().readUtf8()).isEqualTo("efgh");
        assertThat(take().getBody().readUtf8()).isEqualTo("ij");
        JsonNode done = JSON.readTree(take().getBody().readUtf8());
        assertThat(done.get("oid").asText()).isEqualTo(oid);
        assertThat(done.get("parts").toString())
                .isEqualTo("[{\"partNumber\":1,\"etag\":\"e1\"},{\"partNumber\":2,\"etag\":\"e2\"},{\"partNumber\":3,\"etag\":\"e3\"}]");
    }

    @Test
    void errors_areReadable_andNeverContainTheToken() throws Exception {
        server.enqueue(json(401, "{\"error\":\"Invalid credentials in Authorization header\"}").setHeader("X-Request-Id", "req-1"));

        assertThatThrownBy(() -> client.whoAmI(TOKEN))
                .isInstanceOf(HubApiException.class)
                .hasMessageContaining("token is missing or invalid")
                .hasMessageContaining("Invalid credentials in Authorization header")
                .hasMessageContaining("req-1")
                .satisfies(e -> {
                    assertThat(((HubApiException) e).statusCode()).isEqualTo(401);
                    assertThat(e.getMessage()).doesNotContain(TOKEN);
                });
    }

    @Test
    void rateLimit_andConflict_haveSpecificHints() {
        server.enqueue(json(429, "{}"));
        server.enqueue(new MockResponse().setResponseCode(409).setBody("conflict"));

        assertThatThrownBy(() -> client.whoAmI(TOKEN)).hasMessageContaining("rate limit");
        assertThatThrownBy(() -> client.createDataset(TARGET, TOKEN, true)).hasMessageContaining("Conflict");
    }

    @Test
    void networkFailure_isAHubApiException() throws IOException {
        server.shutdown();
        assertThatThrownBy(() -> client.whoAmI(TOKEN))
                .isInstanceOf(HubApiException.class)
                .hasMessageStartingWith("Could not reach Hugging Face");
    }
}
