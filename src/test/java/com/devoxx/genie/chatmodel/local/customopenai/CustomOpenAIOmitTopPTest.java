package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Issue #1240: some OpenAI-compatible endpoints reject {@code top_p} with
 * {@code 400 "Unsupported parameter: 'top_p' is not supported with this model."} — the same class of
 * failure as issue #1225's {@code max_tokens}. Since a gateway can expose arbitrary model aliases,
 * the model name cannot be sniffed; the user opts out of sending {@code top_p} via a setting.
 *
 * <p>These tests assert the actual JSON on the wire, since the whole bug is whether the field is
 * serialised at all.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomOpenAIOmitTopPTest {

    private static final String CHAT_COMPLETION_RESPONSE = """
            {
              "id": "chatcmpl-1",
              "object": "chat.completion",
              "model": "custom-model",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "ok"}, "finish_reason": "stop"}
              ],
              "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
            }
            """;

    private static final String CHAT_COMPLETION_STREAM_RESPONSE = """
            data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model":"custom-model","choices":[{"index":0,"delta":{"role":"assistant","content":"ok"}}]}

            data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model":"custom-model","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

            data: [DONE]

            """;

    private MockWebServer server;
    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;
    private DevoxxGenieStateService mockState;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getCustomOpenAIUrl()).thenReturn(server.url("/v1/").toString());
        when(mockState.isCustomOpenAIApiKeyEnabled()).thenReturn(false);
        when(mockState.getCustomOpenAIApiKey()).thenReturn("");
        when(mockState.isCustomOpenAIModelNameEnabled()).thenReturn(false);
        when(mockState.getCustomOpenAIModelName()).thenReturn("");
        when(mockState.isCustomOpenAIForceHttp11()).thenReturn(false);
        when(mockState.isCustomOpenAIUseMaxCompletionTokens()).thenReturn(false);
        when(mockState.isCustomOpenAIOmitTopP()).thenReturn(false);
        when(mockState.getAgentModeEnabled()).thenReturn(false);

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(MCPService::isMCPEnabled).thenReturn(false);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedMCPService != null) mockedMCPService.close();
        try {
            server.shutdown();
        } catch (IOException e) {
            // Ignore shutdown errors from pending responses
        }
    }

    @Test
    void omitsTopPWhenSettingEnabled() throws InterruptedException {
        when(mockState.isCustomOpenAIOmitTopP()).thenReturn(true);

        JsonObject body = chat();

        // The whole point of the setting: its mere presence is what such a model rejects.
        assertThat(body.has("top_p")).isFalse();
        // Temperature is a separate parameter and must keep travelling.
        assertThat(body.has("temperature")).isTrue();
    }

    @Test
    void sendsTopPWhenSettingDisabled() throws InterruptedException {
        JsonObject body = chat();

        // Default stays unchanged so endpoints that accept top_p keep honouring the configured value.
        assertThat(body.has("top_p")).isTrue();
        assertThat(body.get("top_p").getAsDouble()).isEqualTo(0.9);
    }

    @Test
    void omitsTopPWhenSettingEnabledForStreaming() throws InterruptedException {
        when(mockState.isCustomOpenAIOmitTopP()).thenReturn(true);

        JsonObject body = streamingChat();

        // Issue #1240 was reported on the streaming path, so it needs the same treatment.
        assertThat(body.has("top_p")).isFalse();
        assertThat(body.has("temperature")).isTrue();
    }

    @Test
    void sendsTopPWhenSettingDisabledForStreaming() throws InterruptedException {
        JsonObject body = streamingChat();

        assertThat(body.has("top_p")).isTrue();
        assertThat(body.get("top_p").getAsDouble()).isEqualTo(0.9);
    }

    private JsonObject chat() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_RESPONSE)
                .setHeader("Content-Type", "application/json"));

        ChatModel model = new CustomOpenAIChatModelFactory().createChatModel(customChatModel());
        model.chat("hello");

        return recordedBody();
    }

    private JsonObject streamingChat() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_STREAM_RESPONSE)
                .setHeader("Content-Type", "text/event-stream"));

        StreamingChatModel model = new CustomOpenAIChatModelFactory().createStreamingChatModel(customChatModel());
        CountDownLatch done = new CountDownLatch(1);
        model.chat("hello", new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // Not asserted here — this test is about the request, not the response.
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

        return recordedBody();
    }

    private JsonObject recordedBody() throws InterruptedException {
        RecordedRequest recordedRequest = server.takeRequest(10, TimeUnit.SECONDS);
        assertThat(recordedRequest).as("no request reached the mock server").isNotNull();
        return JsonParser.parseString(recordedRequest.getBody().readUtf8()).getAsJsonObject();
    }

    private static CustomChatModel customChatModel() {
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(256);
        customChatModel.setMaxRetries(1);
        customChatModel.setTimeout(10);
        return customChatModel;
    }
}
