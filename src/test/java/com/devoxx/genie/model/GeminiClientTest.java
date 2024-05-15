package com.devoxx.genie.model;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.gemini.*;
import com.devoxx.genie.model.gemini.model.Content;
import com.devoxx.genie.model.gemini.model.Part;
import com.devoxx.genie.ui.SettingsState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeminiClientTest extends AbstractLightPlatformTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        SettingsState settingsStateMock = mock(SettingsState.class);
        when(settingsStateMock.getGeminiKey()).thenReturn("AIzaSyD0QtB3GciA5VVWODSMZEeLCWBKGEh27jY");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SettingsState.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    public void testGeminiRequest() {
        String baseUrl = "https://generativelanguage.googleapis.com";
        String apiKey = "AIzaSyD0QtB3GciA5VVWODSMZEeLCWBKGEh27jY";
        String modelName = "gemini-pro";

        GeminiClient geminiClient = new GeminiClient(baseUrl, apiKey, modelName, Duration.ofSeconds(60));

        GeminiMessageRequest completionRequest = GeminiMessageRequest.builder()
            .contents(List.of(
                Content.builder()
                    .role("user")
                    .parts(List.of(Part.builder()
                        .text("Tell me more about Google Gemini")
                        .build()))
                    .build()))
            .build();

        GeminiCompletionResponse completionResponse = geminiClient.completion(completionRequest);

        assertThat(completionResponse).isNotNull();
    }
}
