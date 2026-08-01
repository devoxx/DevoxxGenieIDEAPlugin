package com.devoxx.genie.chatmodel.cloud.cloudflare;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.error.ProviderErrorTranslator;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Wire-level regression tests for Workers AI models on the Cloudflare AI Gateway. The gateway root
 * is pointed at a local mock server so the requests run through the real factory, URL assembly and
 * langchain4j HTTP stack, and the tests assert what Cloudflare would actually receive.
 *
 * <p>Issue #1254 established that Workers AI ids need provider routing (a bare {@code @cf/...} id
 * fails with 400 {@code AiGatewayError} 2005/2008). Issue #1256 showed that the gateway-level
 * {@code /compat} endpoint is still the wrong route for them: it forwards OpenAI tool-calling
 * shapes (assistant {@code tool_calls}, {@code role:"tool"} results) to the Workers AI native
 * model schema, which rejects them with 400 {@code AiError} 5006 ("Bad input ... oneOf at '/' not
 * met"). Workers AI models must therefore hit
 * {@code /v1/{account}/{gateway}/workers-ai/v1/chat/completions} — Workers AI's own
 * OpenAI-compatibility layer — with the bare {@code @cf/...} model id, while non-Workers-AI models
 * stay on {@code /compat}.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudflareCompatWireTest {

    private static final String GPT_OSS_20B = "@cf/openai/gpt-oss-20b";
    private static final String GLM_4_7_FLASH = "@cf/zai-org/glm-4.7-flash";

    private static final String EXPECTED_COMPAT_PATH = "/v1/acct123/default/compat/chat/completions";
    private static final String EXPECTED_WORKERS_AI_PATH = "/v1/acct123/default/workers-ai/v1/chat/completions";

    private static final String CHAT_COMPLETION_RESPONSE = """
            {
              "id": "chatcmpl-1",
              "object": "chat.completion",
              "model": "workers-ai/@cf/openai/gpt-oss-20b",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "ok"}, "finish_reason": "stop"}
              ],
              "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
            }
            """;

    private static final String CHAT_COMPLETION_STREAM_RESPONSE = """
            data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model":"workers-ai/@cf/zai-org/glm-4.7-flash","choices":[{"index":0,"delta":{"role":"assistant","content":"ok"}}]}

            data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model":"workers-ai/@cf/zai-org/glm-4.7-flash","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

            data: [DONE]

            """;

    /** The exact 400 body from issue #1254 ("Failed to get response from provider"). */
    private static final String CLOUDFLARE_2005_BODY =
            "{\"success\":false,\"result\":[],\"messages\":[],\"error\":[{\"code\":2005,"
            + "\"message\":\"Failed to get response from provider\"}],\"name\":\"AiGatewayError\","
            + "\"httpCode\":400,\"internalCode\":2005,\"message\":\"Failed to get response from provider\","
            + "\"description\":\"Failed to get response from provider\"}";

    private MockWebServer server;
    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;
    private DevoxxGenieStateService mockState;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        CloudflareGatewayUrl.setRootForTests(server.url("/v1/").toString());

        mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getCloudflareAccountId()).thenReturn("acct123");
        when(mockState.getCloudflareGatewayName()).thenReturn("default");
        when(mockState.getCloudflareKey()).thenReturn("cf-token");
        when(mockState.getCloudflareModelName()).thenReturn("");
        when(mockState.isCloudflareModelNameEnabled()).thenReturn(false);
        when(mockState.getAgentModeEnabled()).thenReturn(false);

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(MCPService::isMCPEnabled).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        CloudflareGatewayUrl.setRootForTests(null);
        if (mockedStateService != null) mockedStateService.close();
        if (mockedMCPService != null) mockedMCPService.close();
        try {
            server.shutdown();
        } catch (IOException e) {
            // Ignore shutdown errors from pending responses
        }
    }

    @Test
    void gptOss20bIsSentToWorkersAiEndpointWithBareId() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_RESPONSE)
                .setHeader("Content-Type", "application/json"));

        ChatModel model = new CloudflareChatModelFactory().createChatModel(customChatModel(GPT_OSS_20B));
        model.chat("hello");

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertThat(request).as("no request reached the mock gateway").isNotNull();
        assertThat(request.getPath()).isEqualTo(EXPECTED_WORKERS_AI_PATH);
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer cf-token");
        assertThat(bodyOf(request).get("model").getAsString())
                .isEqualTo("@cf/openai/gpt-oss-20b");
    }

    @Test
    void nonWorkersAiModelStaysOnCompatEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_RESPONSE)
                .setHeader("Content-Type", "application/json"));

        ChatModel model = new CloudflareChatModelFactory().createChatModel(customChatModel("openai/gpt-4o-mini"));
        model.chat("hello");

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertThat(request).as("no request reached the mock gateway").isNotNull();
        assertThat(request.getPath()).isEqualTo(EXPECTED_COMPAT_PATH);
        assertThat(bodyOf(request).get("model").getAsString()).isEqualTo("openai/gpt-4o-mini");
    }

    /**
     * Issue #1256: the agent loop's follow-up request after an approved tool call — assistant
     * message carrying {@code tool_calls}, then a {@code role:"tool"} result — must reach the
     * workers-ai/v1 endpoint (whose OpenAI-compatibility layer accepts these shapes), not
     * {@code /compat}, where the Workers AI native schema 400s with {@code AiError} 5006.
     */
    @Test
    void agentToolRoundTripIsSentToWorkersAiEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_RESPONSE)
                .setHeader("Content-Type", "application/json"));

        ChatModel model = new CloudflareChatModelFactory().createChatModel(customChatModel(GLM_4_7_FLASH));
        List<ChatMessage> toolRoundTrip = List.of(
                SystemMessage.from("You are a code assistant."),
                UserMessage.from("find null warns"),
                AiMessage.from(ToolExecutionRequest.builder()
                        .id("call_1")
                        .name("search_files")
                        .arguments("{\"path\": \"\", \"pattern\": \"null\", \"file_pattern\": \"*.*\"}")
                        .build()),
                ToolExecutionResultMessage.from("call_1", "search_files", "src/Main.java:12: null check"));
        model.chat(toolRoundTrip);

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertThat(request).as("no request reached the mock gateway").isNotNull();
        assertThat(request.getPath()).isEqualTo(EXPECTED_WORKERS_AI_PATH);

        JsonObject body = bodyOf(request);
        assertThat(body.get("model").getAsString()).isEqualTo("@cf/zai-org/glm-4.7-flash");
        JsonArray messages = body.getAsJsonArray("messages");
        assertThat(messages).hasSize(4);
        JsonObject assistant = messages.get(2).getAsJsonObject();
        assertThat(assistant.get("role").getAsString()).isEqualTo("assistant");
        assertThat(assistant.getAsJsonArray("tool_calls")).hasSize(1);
        JsonObject toolResult = messages.get(3).getAsJsonObject();
        assertThat(toolResult.get("role").getAsString()).isEqualTo("tool");
        assertThat(toolResult.get("content").isJsonPrimitive()).isTrue();
    }

    @Test
    void glm47FlashIsStreamedToWorkersAiEndpointWithBareId() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_STREAM_RESPONSE)
                .setHeader("Content-Type", "text/event-stream"));

        StreamingChatModel model =
                new CloudflareChatModelFactory().createStreamingChatModel(customChatModel(GLM_4_7_FLASH));
        CountDownLatch done = new CountDownLatch(1);
        model.chat("hello", new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // This test is about the request, not the response.
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                done.countDown();
            }

            @Override
            public void onError(Throwable error) {
                done.countDown();
            }
        });
        done.await(10, TimeUnit.SECONDS);

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertThat(request).as("no request reached the mock gateway").isNotNull();
        assertThat(request.getPath()).isEqualTo(EXPECTED_WORKERS_AI_PATH);
        assertThat(bodyOf(request).get("model").getAsString())
                .isEqualTo("@cf/zai-org/glm-4.7-flash");
    }

    @Test
    void cloudflare2005ResponseIsTranslatedIntoWorkersAiGuidance() {
        // Replay the exact issue-#1254 error for a request that already carries the correct
        // prefix: the surviving cause is gateway-side (token permission / model availability),
        // and the translated message must say so instead of leaking the raw JSON.
        for (int i = 0; i < 4; i++) { // cover langchain4j retries, if any
            server.enqueue(new MockResponse()
                    .setResponseCode(400)
                    .setBody(CLOUDFLARE_2005_BODY)
                    .setHeader("Content-Type", "application/json"));
        }

        ChatModel model = new CloudflareChatModelFactory().createChatModel(customChatModel(GPT_OSS_20B));
        Throwable failure = catchThrowable(() -> model.chat("hello"));
        assertThat(failure).isNotNull();

        Optional<String> translated =
                ProviderErrorTranslator.translate(failure, "workers-ai/" + GPT_OSS_20B);

        assertThat(translated).isPresent();
        assertThat(translated.get())
                .contains("2005")
                .contains("Failed to get response from provider")
                .contains("Workers AI")
                .contains("workers-ai/@cf/openai/gpt-oss-20b")
                .doesNotContain("{").doesNotContain("httpCode");
    }

    private static JsonObject bodyOf(RecordedRequest request) {
        return JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
    }

    private static CustomChatModel customChatModel(String modelName) {
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName(modelName);
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(256);
        customChatModel.setMaxRetries(1);
        customChatModel.setTimeout(10);
        return customChatModel;
    }
}
