package com.devoxx.genie.service.openrouter;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.openrouter.Data;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

public class OpenRouterServiceTest extends AbstractLightPlatformTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getOpenRouterKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    public void testGetModels() throws IOException {
        OpenRouterService openRouterService = new OpenRouterService();
        List<Data> models = openRouterService.getModels();
        assertThat(models).isNotEmpty();

        models.forEach(model -> {
            assertThat(model).isNotNull();
            assertThat(model.getId()).isNotNull();
            assertThat(model.getName()).isNotNull();
            assertThat(model.getDescription()).isNotNull();
            assertThat(model.getPricing()).isNotNull();
            assertThat(model.getPricing().getPrompt().floatValue()).isNotNull();
            assertThat(model.getPricing().getCompletion().floatValue()).isNotNull();
            assertThat(model.getContextLength()).isNotNull();
            assertThat(model.getTopProvider()).isNotNull();
        });
    }
}
