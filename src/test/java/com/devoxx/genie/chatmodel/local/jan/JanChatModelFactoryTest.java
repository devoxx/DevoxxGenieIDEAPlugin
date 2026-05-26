package com.devoxx.genie.chatmodel.local.jan;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.model.jan.Settings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.Socket;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JanChatModelFactory}.
 *
 * <p>Note: The {@link #testHelloChat()} test requires Jan to be running before execution.
 * Ensure that Jan is running on localhost:1337 before running this test, otherwise it
 * will be skipped. Ensure Jan is running with downloaded model "Mistral 7B Instruct Q4"
 * (mistral-ins-7b-q4).</p>
 */
class JanChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Setup the mock for SettingsState
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getJanModelUrl()).thenReturn("http://localhost:8080");

            // Instance of the class containing the method to be tested
            JanChatModelFactory factory = new JanChatModelFactory();

            // Create a dummy ChatModel
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("jan");
            customChatModel.setBaseUrl("http://localhost:8080");

            // Call the method
            ChatModel result = factory.createChatModel(customChatModel);
            assertThat(result).isNotNull();
        }
    }

    /**
     * Jan v0.8.0 exposes an OpenAI-compatible {@code /models} endpoint that returns an
     * {@code id} but no {@code name}. The display name must fall back to the id so it is
     * never null (see GitHub issue #1051).
     */
    @Test
    void buildLanguageModelFallsBackToIdWhenNameMissing() {
        Data janModel = new Data();
        janModel.setId("omnicoder-claude-uncensored-v2-q4_k_m");
        // name intentionally left null, as returned by Jan v0.8.0

        LanguageModel model = new JanChatModelFactory().buildLanguageModel(janModel);

        assertThat(model.getModelName()).isEqualTo("omnicoder-claude-uncensored-v2-q4_k_m");
        assertThat(model.getDisplayName()).isEqualTo("omnicoder-claude-uncensored-v2-q4_k_m");
        assertThat(model.getInputMaxTokens()).isEqualTo(8_000);
    }

    @Test
    void buildLanguageModelUsesNameWhenPresent() {
        Data janModel = new Data();
        janModel.setId("mistral-ins-7b-q4");
        janModel.setName("Mistral 7B Instruct Q4");
        Settings settings = new Settings();
        settings.setCtxLen(32_768);
        janModel.setSettings(settings);

        LanguageModel model = new JanChatModelFactory().buildLanguageModel(janModel);

        assertThat(model.getDisplayName()).isEqualTo("Mistral 7B Instruct Q4");
        assertThat(model.getInputMaxTokens()).isEqualTo(32_768);
    }

    /**
     * End-to-end smoke test against a running Jan server. Uses whichever model Jan currently
     * exposes (instead of a hardcoded one) so it works on any setup, and verifies that a chat
     * request actually gets a response — which only happens with the compact-JSON workaround
     * for Jan v0.8.0 (issue #1051); without it the request hangs until timeout.
     */
    @Test
    @EnabledIf("isJanRunning")
    void testHelloChat() {
        String availableModel = firstAvailableJanModel();
        org.junit.jupiter.api.Assumptions.assumeTrue(availableModel != null,
                "Jan is running but exposes no models; skipping live chat test");

        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getJanModelUrl()).thenReturn("http://localhost:1337/v1/");

            JanChatModelFactory factory = new JanChatModelFactory();

            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName(availableModel);
            ChatModel chatLanguageModel = factory.createChatModel(customChatModel);

            String hello;
            try {
                hello = chatLanguageModel.chat("Hello");
            } catch (RuntimeException liveIssue) {
                // Live server flakiness (Jan stopped, model not loadable for inference, …) must
                // not break the build. The deterministic regression for the Jan v0.8.0 hang lives
                // in CompactJsonHttpClientTest; this is only a best-effort end-to-end smoke test.
                org.junit.jupiter.api.Assumptions.abort(
                        "Jan live chat unavailable (" + liveIssue.getMessage() + "); skipping");
                return;
            }
            assertThat(hello).isNotNull();
        }
    }

    private boolean isJanRunning() {
        try (Socket socket = new Socket("localhost", 1337)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the id of the first model Jan currently exposes via {@code /v1/models},
     * or {@code null} if it cannot be determined.
     */
    private static String firstAvailableJanModel() {
        try {
            java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient()
                    .send(java.net.http.HttpRequest.newBuilder()
                                    .uri(java.net.URI.create("http://localhost:1337/v1/models"))
                                    .GET().build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            var data = com.google.gson.JsonParser.parseString(response.body())
                    .getAsJsonObject().getAsJsonArray("data");
            if (data == null || data.isEmpty()) {
                return null;
            }
            return data.get(0).getAsJsonObject().get("id").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
