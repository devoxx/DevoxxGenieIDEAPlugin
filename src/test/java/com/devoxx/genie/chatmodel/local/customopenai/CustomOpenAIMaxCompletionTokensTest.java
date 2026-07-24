package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.model.chat.ChatModel;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Issue #1225: models of the reasoning family (o1/o3/GPT-5) reject the legacy {@code max_tokens}
 * field with {@code 400 Unsupported parameter: 'max_tokens' is not supported with this model.
 * Use 'max_completion_tokens' instead}. A LiteLLM gateway fronting such a model (reported case:
 * {@code gpt-chat-latest}) surfaces the same 400 while its {@code gpt-4o} / {@code gpt-4.1} models
 * keep working.
 *
 * <p>These tests assert the actual JSON on the wire, since the whole bug is which field name gets
 * serialised.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomOpenAIMaxCompletionTokensTest {

    private static final String CHAT_COMPLETION_RESPONSE = """
            {
              "id": "chatcmpl-1",
              "object": "chat.completion",
              "model": "gpt-chat-latest",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "ok"}, "finish_reason": "stop"}
              ],
              "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
            }
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
    void sendsMaxCompletionTokensWhenSettingEnabled() throws InterruptedException {
        when(mockState.isCustomOpenAIUseMaxCompletionTokens()).thenReturn(true);

        JsonObject body = chat("gpt-chat-latest", 256);

        // The whole point of the setting: the cap travels under the new name...
        assertThat(body.has("max_completion_tokens")).isTrue();
        assertThat(body.get("max_completion_tokens").getAsInt()).isEqualTo(256);
        // ...and the legacy name is absent, since its mere presence is what the model rejects.
        assertThat(body.has("max_tokens")).isFalse();
    }

    @Test
    void sendsLegacyMaxTokensWhenSettingDisabled() throws InterruptedException {
        JsonObject body = chat("gpt-4o", 256);

        // Default stays on the legacy field so endpoints that only understand it keep working.
        assertThat(body.has("max_tokens")).isTrue();
        assertThat(body.get("max_tokens").getAsInt()).isEqualTo(256);
        assertThat(body.has("max_completion_tokens")).isFalse();
    }

    private JsonObject chat(String modelName, int maxTokens) throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(CHAT_COMPLETION_RESPONSE)
                .setHeader("Content-Type", "application/json"));

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName(modelName);
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(maxTokens);
        customChatModel.setMaxRetries(1);
        customChatModel.setTimeout(10);

        ChatModel model = new CustomOpenAIChatModelFactory().createChatModel(customChatModel);
        model.chat("hello");

        RecordedRequest recordedRequest = server.takeRequest();
        return JsonParser.parseString(recordedRequest.getBody().readUtf8()).getAsJsonObject();
    }
}
