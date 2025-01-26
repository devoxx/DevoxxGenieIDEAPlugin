package com.devoxx.genie.chatmodel.openai;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.chatmodel.cloud.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenAiChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getOpenAIKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    public void createChatModel() {
        OpenAIChatModelFactory factory = new OpenAIChatModelFactory();
        ChatModel chatModel = new ChatModel();
        chatModel.setModelName("gpt-3.5-turbo");
        chatModel.setTemperature(0.7);
        chatModel.setMaxTokens(100);

        ChatLanguageModel result = factory.createChatModel(chatModel);

        assertThat(result).isNotNull();
    }

    @Test
    public void getModels() {
        OpenAIChatModelFactory factory = new OpenAIChatModelFactory();
        assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> models = factory.getModels();
        assertThat(models).size().isGreaterThan(7);
    }
}
