package com.devoxx.genie.chatmodel.cloud.openrouter;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.openrouter.Data;
import com.devoxx.genie.model.openrouter.Pricing;
import com.devoxx.genie.model.openrouter.TopProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public void testGetModelsSuccess() throws IOException {
        // Set up mockStatic for OpenRouterService
        try (MockedStatic<OpenRouterService> mockedStatic = mockStatic(OpenRouterService.class)) {
            // Mock the getInstance method to return our mock
            mockedStatic.when(OpenRouterService::getInstance).thenReturn(openRouterService);

            // Prepare test data
            List<Data> testModels = createTestModels();
            when(openRouterService.getModels()).thenReturn(testModels);

            // Test the method
            List<LanguageModel> result = factory.getModels();

            // Verify results
            assertNotNull(result);
            assertEquals(2, result.size());

            // Verify first model
            LanguageModel firstModel = result.stream()
                    .filter(m -> m.getModelName().equals("model1"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(firstModel);
            assertEquals("Model One", firstModel.getDisplayName());
            assertEquals(ModelProvider.OpenRouter, firstModel.getProvider());
            assertEquals(10.0, firstModel.getInputCost());
            // assertEquals(20.0, firstModel.getOutputCost());
            assertEquals(4000, firstModel.getInputMaxTokens());
            assertTrue(firstModel.isApiKeyUsed());

            // Verify second model
            LanguageModel secondModel = result.stream()
                    .filter(m -> m.getModelName().equals("model2"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(secondModel);
            assertEquals("Model Two", secondModel.getDisplayName());
            assertEquals(5000, secondModel.getInputMaxTokens());

            // Verify the service was called
            verify(openRouterService, times(1)).getModels();
        }
    }

    @Test
    public void testGetModelsCached() throws IOException {
        // Set up mockStatic for OpenRouterService
        try (MockedStatic<OpenRouterService> mockedStatic = mockStatic(OpenRouterService.class)) {
            // Mock the getInstance method to return our mock
            mockedStatic.when(OpenRouterService::getInstance).thenReturn(openRouterService);

            // Prepare test data
            List<Data> testModels = createTestModels();
            when(openRouterService.getModels()).thenReturn(testModels);

            // Call once to cache
            List<LanguageModel> firstResult = factory.getModels();
            assertNotNull(firstResult);

            // Call again - should use cache
            List<LanguageModel> secondResult = factory.getModels();

            // Verify service was only called once
            verify(openRouterService, times(1)).getModels();

            // Verify both results are the same instance
            assertSame(firstResult, secondResult);
        }
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

    @Test
    public void testConvertAndScalePrice() throws Exception {
        // Reset cached models to ensure we can test the price conversion
        Field cachedModelsField = OpenRouterChatModelFactory.class.getDeclaredField("cachedModels");
        cachedModelsField.setAccessible(true);
        cachedModelsField.set(factory, null);

        // Access the private method using reflection
        Method convertMethod = OpenRouterChatModelFactory.class.getDeclaredMethod("convertAndScalePrice", double.class);
        convertMethod.setAccessible(true);

        // Test with various inputs
        double result1 = (double) convertMethod.invoke(factory, 0.00001);
        assertEquals(10.0, result1, 0.000001);

        double result2 = (double) convertMethod.invoke(factory, 0.00002);
        assertEquals(20.0, result2, 0.000001);

        // Test with a value that requires rounding
        double result3 = (double) convertMethod.invoke(factory, 0.0000123456);

        // Expected: 0.0000123456 * 1,000,000 = 12.3456, rounded to 6 decimal places
        BigDecimal expected = BigDecimal.valueOf(0.0000123456)
                .multiply(BigDecimal.valueOf(1_000_000))
                .setScale(6, RoundingMode.HALF_UP);
        assertEquals(expected.doubleValue(), result3, 0.000001);
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
