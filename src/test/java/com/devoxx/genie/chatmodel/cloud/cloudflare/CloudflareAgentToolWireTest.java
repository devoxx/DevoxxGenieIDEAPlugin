package com.devoxx.genie.chatmodel.cloud.cloudflare;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Issue #1256: the agent's second round trip (assistant tool_calls + tool result) is rejected by
 * Cloudflare with a Workers AI schema error complaining that {@code messages[].content} is an
 * 'array' not a 'string' and that one message is missing 'role,content'.
 *
 * <p>This test characterises what the plugin actually puts on the wire for that exact round trip,
 * so we can tell a client-side bug from a gateway-side one. Workers AI's native message schema
 * requires every message to carry a string {@code content}
 * (<a href="https://developers.cloudflare.com/workers-ai/models/gpt-oss-20b/">gpt-oss-20b</a>).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudflareAgentToolWireTest {

    private static final String GPT_OSS_20B = "workers-ai/@cf/openai/gpt-oss-20b";

    private static final String CHAT_COMPLETION_RESPONSE = """
            {
              "id": "chatcmpl-2",
              "object": "chat.completion",
              "model": "workers-ai/@cf/openai/gpt-oss-20b",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "done"}, "finish_reason": "stop"}
              ],
              "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
            }
            """;

    private static final String CHAT_COMPLETION_STREAM_RESPONSE = """
            data: {"id":"chatcmpl-2","object":"chat.completion.chunk","model":"workers-ai/@cf/openai/gpt-oss-20b","choices":[{"index":0,"delta":{"role":"assistant","content":"done"}}]}

            data: {"id":"chatcmpl-2","object":"chat.completion.chunk","model":"workers-ai/@cf/openai/gpt-oss-20b","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

            data: [DONE]

            """;

    private MockWebServer server;
    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        CloudflareGatewayUrl.setRootForTests(server.url("/v1/").toString());

        DevoxxGenieStateService mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getCloudflareAccountId()).thenReturn("acct123");
        when(mockState.getCloudflareGatewayName()).thenReturn("default");
        when(mockState.getCloudflareKey()).thenReturn("cf-token");
        when(mockState.getCloudflareModelName()).thenReturn("");
        when(mockState.isCloudflareModelNameEnabled()).thenReturn(false);
        when(mockState.getAgentModeEnabled()).thenReturn(true);

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
    void everyMessageOfAToolRoundTripCarriesAStringContent() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_RESPONSE)
                .setHeader("Content-Type", "application/json"));

        ChatModel model = new CloudflareChatModelFactory().createChatModel(customChatModel());
        model.chat(searchFilesRoundTrip());

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertThat(request).as("no request reached the mock gateway").isNotNull();

        JsonArray messages = bodyOf(request).getAsJsonArray("messages");
        assertThat(messages).hasSize(4);

        for (int i = 0; i < messages.size(); i++) {
            JsonObject message = messages.get(i).getAsJsonObject();
            assertThat(message.has("role")).as("messages[%d] has no role", i).isTrue();

            JsonElement content = message.get("content");
            assertThat(content).as("messages[%d] has no content (Workers AI requires role+content)", i).isNotNull();
            assertThat(content.isJsonNull()).as("messages[%d] content is null", i).isFalse();
            assertThat(content.isJsonPrimitive() && content.getAsJsonPrimitive().isString())
                    .as("messages[%d] content is not a string but %s", i, content)
                    .isTrue();
        }
    }

    @Test
    void toolSpecificationsAreSentAsOpenAiTools() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_RESPONSE)
                .setHeader("Content-Type", "application/json"));

        ChatModel model = new CloudflareChatModelFactory().createChatModel(customChatModel());
        model.chat(searchFilesRoundTrip());

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertThat(request).isNotNull();

        JsonObject body = bodyOf(request);
        assertThat(body.getAsJsonArray("tools")).hasSize(1);
        assertThat(body.getAsJsonArray("tools").get(0).getAsJsonObject()
                .getAsJsonObject("function").get("name").getAsString()).isEqualTo("search_files");

        JsonObject assistant = body.getAsJsonArray("messages").get(2).getAsJsonObject();
        assertThat(assistant.get("role").getAsString()).isEqualTo("assistant");
        assertThat(assistant.getAsJsonArray("tool_calls")).hasSize(1);
        // langchain4j omits content here; the normalizer must add the empty string Workers AI wants.
        assertThat(assistant.get("content").getAsString()).isEmpty();

        JsonObject toolResult = body.getAsJsonArray("messages").get(3).getAsJsonObject();
        assertThat(toolResult.get("role").getAsString()).isEqualTo("tool");
        assertThat(toolResult.get("tool_call_id").getAsString()).isEqualTo("call_1");
    }

    @Test
    void streamedToolRoundTripIsNormalizedToo() throws InterruptedException {
        // Agent mode normally streams, so the SSE path needs the same reshaping as the sync one.
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_STREAM_RESPONSE)
                .setHeader("Content-Type", "text/event-stream"));

        StreamingChatModel model =
                new CloudflareChatModelFactory().createStreamingChatModel(customChatModel());
        CountDownLatch done = new CountDownLatch(1);
        model.chat(searchFilesRoundTrip(), new StreamingChatResponseHandler() {
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

        JsonObject assistant = bodyOf(request).getAsJsonArray("messages").get(2).getAsJsonObject();
        assertThat(assistant.getAsJsonArray("tool_calls")).hasSize(1);
        assertThat(assistant.get("content").getAsString()).isEmpty();
    }

    /** The exact agent turn from the report: 'find null warns' answered with a search_files call. */
    private static @org.jetbrains.annotations.NotNull ChatRequest searchFilesRoundTrip() {
        ToolExecutionRequest searchFiles = ToolExecutionRequest.builder()
                .id("call_1")
                .name("search_files")
                .arguments("{\"path\": \"\", \"pattern\": \"null\", \"file_pattern\": \"*.*\"}")
                .build();

        return ChatRequest.builder()
                .messages(
                        SystemMessage.from("You are a helpful coding assistant."),
                        UserMessage.from("find null warns"),
                        AiMessage.from(searchFiles),
                        ToolExecutionResultMessage.from(searchFiles, "src/Foo.java:12: null"))
                .toolSpecifications(ToolSpecification.builder()
                        .name("search_files")
                        .description("Search files for a pattern")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path")
                                .addStringProperty("pattern")
                                .addStringProperty("file_pattern")
                                .build())
                        .build())
                .build();
    }

    private static JsonObject bodyOf(RecordedRequest request) {
        return JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
    }

    private static CustomChatModel customChatModel() {
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName(CloudflareAgentToolWireTest.GPT_OSS_20B);
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(256);
        customChatModel.setMaxRetries(1);
        customChatModel.setTimeout(10);
        return customChatModel;
    }
}
