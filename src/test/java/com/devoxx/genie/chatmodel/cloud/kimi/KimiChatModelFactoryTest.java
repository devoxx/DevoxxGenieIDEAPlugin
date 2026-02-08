package com.devoxx.genie.chatmodel.cloud.kimi;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.model.chat.ChatModel;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KimiChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getKimiKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    void createChatModel() {
        var factory = new KimiChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("dummy-model");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(256);
        customChatModel.setMaxRetries(3);

        ChatModel result = factory.createChatModel(customChatModel);
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    public void testModelNames() {
        KimiChatModelFactory factory = new KimiChatModelFactory();
        Assertions.assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> modelNames = factory.getModels();
        Assertions.assertThat(modelNames).size().isGreaterThanOrEqualTo(4);
    }
}
