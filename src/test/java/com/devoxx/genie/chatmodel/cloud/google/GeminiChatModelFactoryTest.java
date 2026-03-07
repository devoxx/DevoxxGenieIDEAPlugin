package com.devoxx.genie.chatmodel.cloud.google;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.models.LLMModelRegistryService;
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

public class GeminiChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getGeminiKey()).thenReturn("dummy-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(
            ApplicationManager.getApplication(),
            DevoxxGenieStateService.class,
            settingsStateMock,
            getTestRootDisposable()
        );

        LLMModelRegistryService modelRegistryServiceMock = mock(LLMModelRegistryService.class);
        when(modelRegistryServiceMock.getModels()).thenReturn(List.of(
            model("gemini-2.5-pro"),
            model("gemini-2.5-flash-lite"),
            model("gemini-2.5-flash"),
            model("gemini-3-flash-preview"),
            model("gemini-3-pro-preview")
        ));
        ServiceContainerUtil.replaceService(
            ApplicationManager.getApplication(),
            LLMModelRegistryService.class,
            modelRegistryServiceMock,
            getTestRootDisposable()
        );
    }

    @Test
    void createChatModel() {
        // Instance of the class containing the method to be tested
        var factory = new GoogleChatModelFactory();

        // Create a dummy ChatModel
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("gemini-1.5-flash");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(256);
        customChatModel.setMaxRetries(3);

        // Call the method
        ChatModel result = factory.createChatModel(customChatModel);
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    public void testModelNames() {
        GoogleChatModelFactory factory = new GoogleChatModelFactory();
        Assertions.assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> modelNames = factory.getModels();
        Assertions.assertThat(modelNames).size().isGreaterThanOrEqualTo(5);
    }

    private static LanguageModel model(String modelName) {
        return LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName(modelName)
            .displayName(modelName)
            .inputCost(1)
            .outputCost(1)
            .inputMaxTokens(1_000_000)
            .apiKeyUsed(true)
            .build();
    }
}
