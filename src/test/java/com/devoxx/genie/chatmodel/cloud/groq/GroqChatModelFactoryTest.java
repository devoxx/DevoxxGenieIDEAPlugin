package com.devoxx.genie.chatmodel.cloud.groq;

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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroqChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getGroqKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());

        LLMModelRegistryService modelRegistryServiceMock = mock(LLMModelRegistryService.class);
        when(modelRegistryServiceMock.getModels()).thenReturn(List.of(
            model("llama-3.3-70b-versatile"),
            model("llama-3.1-8b-instant"),
            model("mixtral-8x7b-32768"),
            model("gemma2-9b-it"),
            model("deepseek-r1-distill-llama-70b"),
            model("qwen-qwq-32b")
        ));
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), LLMModelRegistryService.class, modelRegistryServiceMock, getTestRootDisposable());
    }

    @Test
    void createChatModel() {
        // Instance of the class containing the method to be tested
        GroqChatModelFactory factory = new GroqChatModelFactory();

        // Create a dummy ChatModel
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setBaseUrl("http://localhost:8080");

        // Call the method
        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    public void testModelNames() {
        GroqChatModelFactory factory = new GroqChatModelFactory();
        Assertions.assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> modelNames = factory.getModels();
        Assertions.assertThat(modelNames).size().isGreaterThan(5);
    }

    private static LanguageModel model(String modelName) {
        return LanguageModel.builder()
            .provider(ModelProvider.Groq)
            .modelName(modelName)
            .displayName(modelName)
            .inputCost(1)
            .outputCost(1)
            .inputMaxTokens(128_000)
            .apiKeyUsed(true)
            .build();
    }
}
