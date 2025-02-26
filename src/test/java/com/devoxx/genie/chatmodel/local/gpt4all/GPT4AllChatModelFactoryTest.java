package com.devoxx.genie.chatmodel.local.gpt4all;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
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

class GPT4AllChatModelFactoryTest extends BasePlatformTestCase {

    private GPT4AllChatModelFactory factory;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private GPT4AllModelService modelService;

    @Mock
    private ChatModel chatModel;

    // Test models
    private final Model model1 = createTestModel("model1", "Model One");
    private final Model model2 = createTestModel("model2", "Model Two");
    private final Model[] testModels = new Model[] { model1, model2 };

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);

        // Set up the factory
        factory = new GPT4AllChatModelFactory() {
            @Override
            protected String getModelUrl() {
                return "http://localhost:8080";
            }
        };

        // Setup ChatModel mock
        when(chatModel.getModelName()).thenReturn("gpt4all-model");
        when(chatModel.getMaxRetries()).thenReturn(3);
        when(chatModel.getTemperature()).thenReturn(0.7);
        when(chatModel.getMaxTokens()).thenReturn(4096);
        when(chatModel.getTimeout()).thenReturn(60);
        when(chatModel.getTopP()).thenReturn(0.95);
    }

    @Test
    public void testCreateChatModel() {
        ChatLanguageModel model = factory.createChatModel(chatModel);

        assertNotNull(model);
        assertTrue(model instanceof LocalAiChatModel);
    }

    @Test
    public void testCreateStreamingChatModel() {
        StreamingChatLanguageModel model = factory.createStreamingChatModel(chatModel);

        assertNotNull(model);
        assertTrue(model instanceof LocalAiStreamingChatModel);
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
                protected void handleModelFetchError(IOException e) {
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

            // Test caching - create spy to verify method calls
            GPT4AllChatModelFactory spyFactory = spy(testFactory);

            // Get models again - should use cached version
            List<LanguageModel> cachedModels = spyFactory.getModels();

            // Verify models are the same
            assertEquals(models, cachedModels);

            // Verify fetchModels was not called again
            verify(spyFactory, never()).fetchModels();
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
                protected void handleModelFetchError(IOException e) {
                    // No-op for testing
                }

                @Override
                protected void handleGeneralFetchError(IOException e) {
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
