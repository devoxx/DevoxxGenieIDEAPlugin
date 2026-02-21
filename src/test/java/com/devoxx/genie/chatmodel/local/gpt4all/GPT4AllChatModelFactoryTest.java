package com.devoxx.genie.chatmodel.local.gpt4all;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GPT4AllChatModelFactoryTest {

    private GPT4AllChatModelFactory factory;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private GPT4AllModelService modelService;

    @Mock
    private CustomChatModel customChatModel;

    // Test models
    private final Model model1 = createTestModel("model1", "Model One");
    private final Model model2 = createTestModel("model2", "Model Two");
    private final Model[] testModels = new Model[] { model1, model2 };

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up the factory
        factory = new GPT4AllChatModelFactory() {
            @Override
            protected String getModelUrl() {
                return "http://localhost:8080";
            }

            @Override
            public java.util.List<dev.langchain4j.model.chat.listener.ChatModelListener> getListener() {
                return java.util.List.of();
            }
        };

        // Setup ChatModel mock
        when(customChatModel.getModelName()).thenReturn("gpt4all-model");
        when(customChatModel.getMaxRetries()).thenReturn(3);
        when(customChatModel.getTemperature()).thenReturn(0.7);
        when(customChatModel.getMaxTokens()).thenReturn(4096);
        when(customChatModel.getTimeout()).thenReturn(60);
        when(customChatModel.getTopP()).thenReturn(0.95);
    }

    @Test
    public void testCreateChatModel() {
        ChatModel model = factory.createChatModel(customChatModel);

        assertNotNull(model);
        assertTrue(model instanceof OpenAiChatModel);
    }

    @Test
    public void testCreateStreamingChatModel() {
        StreamingChatModel model = factory.createStreamingChatModel(customChatModel);

        assertNotNull(model);
        assertTrue(model instanceof OpenAiStreamingChatModel);
    }

    @Test
    public void testGetModelUrl() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = mockStatic(DevoxxGenieStateService.class)) {
            mockedSettings.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getGpt4allModelUrl()).thenReturn("http://test-url:8080");

            // Create a new instance to use the real implementation
            GPT4AllChatModelFactory testFactory = new GPT4AllChatModelFactory();

            assertEquals("http://test-url:8080", testFactory.getModelUrl());
            verify(stateService).getGpt4allModelUrl();
        }
    }

    @Test
    public void testFetchModels() throws IOException {
        try (MockedStatic<GPT4AllModelService> mockedService = mockStatic(GPT4AllModelService.class)) {
            mockedService.when(GPT4AllModelService::getInstance).thenReturn(modelService);
            when(modelService.getModels()).thenReturn(Arrays.asList(testModels));

            Object[] fetchedModels = factory.fetchModels();

            assertNotNull(fetchedModels);
            assertEquals(2, fetchedModels.length);
            assertEquals(model1, fetchedModels[0]);
            assertEquals(model2, fetchedModels[1]);
            verify(modelService).getModels();
        }
    }

    @Test
    public void testBuildLanguageModel() {
        LanguageModel languageModel = factory.buildLanguageModel(model1);

        assertNotNull(languageModel);
        assertEquals(ModelProvider.GPT4All, languageModel.getProvider());
        assertEquals("model1", languageModel.getModelName());
        assertEquals("model1", languageModel.getDisplayName());
        assertEquals(0.0, languageModel.getInputCost());
        assertEquals(0.0, languageModel.getOutputCost());
        assertFalse(languageModel.isApiKeyUsed());
    }

    @Test
    public void testGetModels() throws IOException {
        try (MockedStatic<GPT4AllModelService> mockedService = mockStatic(GPT4AllModelService.class)) {
            mockedService.when(GPT4AllModelService::getInstance).thenReturn(modelService);
            when(modelService.getModels()).thenReturn(Arrays.asList(testModels));

            // Override notification handling method to avoid platform issues in tests
            GPT4AllChatModelFactory testFactory = new GPT4AllChatModelFactory() {
                @Override
                protected void handleModelFetchError(@NotNull IOException e) {
                    // No-op for testing
                }

                @Override
                protected void handleGeneralFetchError(IOException e) {
                    // No-op for testing
                }
            };

            List<LanguageModel> models = testFactory.getModels();

            assertNotNull(models);
            assertEquals(2, models.size());

            // Check that both models exist in the list, without assuming order
            boolean foundModel1 = false;
            boolean foundModel2 = false;

            for (LanguageModel model : models) {
                if ("model1".equals(model.getModelName())) {
                    foundModel1 = true;
                } else if ("model2".equals(model.getModelName())) {
                    foundModel2 = true;
                }
            }
            assertTrue(foundModel1);
            assertTrue(foundModel2);

            // Test caching - call getModels() again on the same instance
            // providerChecked=true so checkAndFetchModels() is NOT called again
            List<LanguageModel> cachedModels = testFactory.getModels();

            // Verify models are the same cached list (same object reference)
            assertEquals(models, cachedModels);
            assertSame(models, cachedModels);
        }
    }

    @Test
    public void testGetModelsHandlesException() throws IOException {
        try (MockedStatic<GPT4AllModelService> mockedService = mockStatic(GPT4AllModelService.class)) {
            mockedService.when(GPT4AllModelService::getInstance).thenReturn(modelService);
            when(modelService.getModels()).thenThrow(new IOException("Test exception"));

            // Override notification handling method to avoid platform issues in tests
            GPT4AllChatModelFactory testFactory = new GPT4AllChatModelFactory() {
                @Override
                protected void handleModelFetchError(@NotNull IOException e) {
                    // No-op for testing
                }

                @Override
                protected void handleGeneralFetchError(IOException e) {
                    // No-op for testing
                }

                @Override
                protected void handleProviderNotRunning() {
                    // No-op for testing
                }
            };

            List<LanguageModel> models = testFactory.getModels();

            assertNotNull(models);
            assertTrue(models.isEmpty());
            assertFalse(testFactory.providerRunning);
            assertTrue(testFactory.providerChecked);
        }
    }

    @Test
    public void testResetModels() throws IOException {
        try (MockedStatic<GPT4AllModelService> mockedService = mockStatic(GPT4AllModelService.class)) {
            mockedService.when(GPT4AllModelService::getInstance).thenReturn(modelService);
            when(modelService.getModels()).thenReturn(Arrays.asList(testModels));

            // Get models to populate cache
            factory.getModels();
            assertTrue(factory.providerChecked);
            assertTrue(factory.providerRunning);
            assertNotNull(factory.cachedModels);

            // Reset models
            factory.resetModels();

            // Verify state is reset
            assertNull(factory.cachedModels);
            assertFalse(factory.providerChecked);
            assertFalse(factory.providerRunning);
        }
    }

    // Helper method to create test models
    private Model createTestModel(String id, String name) {
        Model model = new Model();
        model.setId(id);
        model.setOwnedBy("test-owner");
        return model;
    }
}
