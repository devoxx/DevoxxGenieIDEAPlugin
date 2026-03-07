package com.devoxx.genie.chatmodel.cloud.openai;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.model.chat.ChatModel;

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

        LLMModelRegistryService modelRegistryServiceMock = mock(LLMModelRegistryService.class);
        when(modelRegistryServiceMock.getModels()).thenReturn(List.of(
            model("gpt-4.1"),
            model("gpt-4.1-mini"),
            model("gpt-4.1-nano"),
            model("o3-mini"),
            model("o1"),
            model("gpt-4"),
            model("gpt-4o"),
            model("gpt-4o-mini")
        ));
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), LLMModelRegistryService.class, modelRegistryServiceMock, getTestRootDisposable());
    }

    @Test
    void createChatModel() {
        OpenAIChatModelFactory factory = new OpenAIChatModelFactory();
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("gpt-3.5-turbo");
        customChatModel.setTemperature(0.7);
        customChatModel.setMaxTokens(100);

        ChatModel result = factory.createChatModel(customChatModel);

        assertThat(result).isNotNull();
    }

    @Test
    void getModels() {
        OpenAIChatModelFactory factory = new OpenAIChatModelFactory();
        assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> models = factory.getModels();
        assertThat(models).size().isGreaterThan(7);
    }

    private static LanguageModel model(String modelName) {
        return LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName(modelName)
            .displayName(modelName)
            .inputCost(1)
            .outputCost(1)
            .inputMaxTokens(1_000_000)
            .apiKeyUsed(true)
            .build();
    }
}
