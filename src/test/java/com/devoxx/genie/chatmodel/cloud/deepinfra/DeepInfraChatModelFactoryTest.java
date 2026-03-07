package com.devoxx.genie.chatmodel.cloud.deepinfra;

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

public class DeepInfraChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getDeepInfraKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());

        LLMModelRegistryService modelRegistryServiceMock = mock(LLMModelRegistryService.class);
        when(modelRegistryServiceMock.getModels()).thenReturn(List.of(
            model("meta-llama/Meta-Llama-3.1-405B-Instruct"),
            model("meta-llama/Meta-Llama-3.1-70B-Instruct"),
            model("meta-llama/Meta-Llama-3.1-8B-Instruct"),
            model("mistralai/Mistral-Nemo-Instruct-2407"),
            model("mistralai/Mixtral-8x7B-Instruct-v0.1"),
            model("mistralai/Mixtral-8x22B-Instruct-v0.1"),
            model("mistralai/Mistral-7B-Instruct-v0.3"),
            model("microsoft/WizardLM-2-8x22B"),
            model("microsoft/WizardLM-2-7B"),
            model("openchat/openchat_3.5"),
            model("google/gemma-2-9b-it")
        ));
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), LLMModelRegistryService.class, modelRegistryServiceMock, getTestRootDisposable());
    }

    @Test
    void createChatModel() {
        // Instance of the class containing the method to be tested
        var factory = new DeepInfraChatModelFactory();

        // Create a dummy ChatModel
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("dummy-model");
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
        DeepInfraChatModelFactory factory = new DeepInfraChatModelFactory();
        Assertions.assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> modelNames = factory.getModels();
        Assertions.assertThat(modelNames).size().isGreaterThan(10);
    }

    private static LanguageModel model(String modelName) {
        return LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName(modelName)
            .displayName(modelName)
            .inputCost(1)
            .outputCost(1)
            .inputMaxTokens(128_000)
            .apiKeyUsed(true)
            .build();
    }
}
