package com.devoxx.genie.chatmodel.cloud.openrouter;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.openrouter.Data;
import com.devoxx.genie.model.openrouter.Pricing;
import com.devoxx.genie.model.openrouter.TopProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class OpenRouterChatModelFactoryTest extends BasePlatformTestCase {

    private OpenRouterChatModelFactory factory;

    @Mock
    private OpenRouterService openRouterService;

    @Mock
    private CustomChatModel customChatModel;

    @Mock
    private Project defaultProject;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);

        // Mock DevoxxGenieStateService
        DevoxxGenieStateService stateServiceMock = mock(DevoxxGenieStateService.class);
        when(stateServiceMock.getOpenRouterKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(
                ApplicationManager.getApplication(),
                DevoxxGenieStateService.class,
                stateServiceMock,
                getTestRootDisposable()
        );

        // Initialize factory
        factory = new OpenRouterChatModelFactory();

        // Set up common mocks for ChatModel
        when(customChatModel.getModelName()).thenReturn("test-model");
        when(customChatModel.getMaxRetries()).thenReturn(3);
        when(customChatModel.getTemperature()).thenReturn(0.7);
        when(customChatModel.getTimeout()).thenReturn(60);
        when(customChatModel.getTopP()).thenReturn(0.95);

        // Mock ProjectManager
        try (MockedStatic<ProjectManager> mockedProjectManager = mockStatic(ProjectManager.class)) {
            mockedProjectManager.when(ProjectManager::getInstance).thenReturn(mock(ProjectManager.class));
            when(ProjectManager.getInstance().getDefaultProject()).thenReturn(defaultProject);
        }
    }

    @Test
    public void testCreateStreamingChatModel() {
        StreamingChatModel model = factory.createStreamingChatModel(customChatModel);

        assertNotNull(model);
        assertTrue(model instanceof OpenAiStreamingChatModel);
    }

    @Test
    public void testGetModelsHandlesException() throws IOException {
        // Create a test-specific factory subclass that doesn't use notifications
        OpenRouterChatModelFactory testFactory = new OpenRouterChatModelFactory() {
            @Override
            protected void handleModelFetchError(IOException e) {
                // Do nothing here to prevent notification error
            }
        };

        // Reset cached models in our test factory
        try {
            Field cachedModelsField = OpenRouterChatModelFactory.class.getDeclaredField("cachedModels");
            cachedModelsField.setAccessible(true);
            cachedModelsField.set(testFactory, null);
        } catch (Exception e) {
            fail("Failed to reset cached models: " + e.getMessage());
        }

        // Set up mockStatic for OpenRouterService
        try (MockedStatic<OpenRouterService> mockedService = mockStatic(OpenRouterService.class)) {
            // Mock the OpenRouterService instance
            mockedService.when(OpenRouterService::getInstance).thenReturn(openRouterService);

            // Mock service to throw IOException
            when(openRouterService.getModels()).thenThrow(new IOException("Network error"));

            // Test the method
            List<LanguageModel> result = testFactory.getModels();

            // Verify results
            assertNotNull(result);
            assertTrue(result.isEmpty());

            // Verify service was called
            verify(openRouterService, times(1)).getModels();
        }
    }

    // Helper method to create test data
    private List<Data> createTestModels() {
        List<Data> models = new ArrayList<>();

        // Create first model
        Data model1 = new Data();
        model1.setId("model1");
        model1.setName("Model One");
        model1.setContextLength(4000);

        Pricing pricing1 = new Pricing();
        pricing1.setPrompt("0.00001"); // Should become 10.0 after scaling
        pricing1.setCompletion("0.00002"); // Should become 20.0 after scaling
        model1.setPricing(pricing1);

        // Create second model with null contextLength to test the fallback
        Data model2 = new Data();
        model2.setId("model2");
        model2.setName("Model Two");
        model2.setContextLength(null);

        TopProvider topProvider = new TopProvider();
        topProvider.setContextLength(5000);
        model2.setTopProvider(topProvider);

        Pricing pricing2 = new Pricing();
        pricing2.setPrompt("0.000005"); // Should become 5.0 after scaling
        pricing2.setCompletion("0.000015"); // Should become 15.0 after scaling
        model2.setPricing(pricing2);

        models.add(model1);
        models.add(model2);

        return models;
    }
}
