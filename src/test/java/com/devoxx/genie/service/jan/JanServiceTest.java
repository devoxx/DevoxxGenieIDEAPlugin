package com.devoxx.genie.service.jan;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.jan.Data;
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

public class JanServiceTest extends AbstractLightPlatformTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getJanModelUrl()).thenReturn("http://localhost:1337/v1/");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    public void testGetModels() throws IOException {
        JanService janService = new JanService();
        List<Data> models = janService.getModels();
        assertThat(models).isNotEmpty();

        models.forEach(model -> {
            assertThat(model).isNotNull();
            assertThat(model.getId()).isNotNull();
            assertThat(model.getName()).isNotNull();
            assertThat(model.getDescription()).isNotNull();
            assertThat(model.getSettings()).isNotNull();
            assertThat(model.getSettings().getCtxLen()).isNotNull();
        });
    }
}

