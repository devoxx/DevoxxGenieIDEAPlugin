package com.devoxx.genie.chatmodel.local;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
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
import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LocalChatModelFactoryTest extends BasePlatformTestCase {

    // Concrete implementation of the abstract class for testing
    private static class TestLocalChatModelFactory extends LocalChatModelFactory {
        // Model data for testing
        private final Object[] modelData;
        private final boolean throwOnFetch;

        public TestLocalChatModelFactory(boolean throwOnFetch) {
            super(ModelProvider.GPT4All);
            this.throwOnFetch = throwOnFetch;
            this.modelData = new Object[] {
                    "model1",
                    "model2",
                    "model3"
            };
        }

        @Override
        public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
            return createOpenAiChatModel(customChatModel);
        }

        @Override
        public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
            return createOpenAiStreamingChatModel(customChatModel);
        }

        @Override
        protected String getModelUrl() {
            return "http://localhost:8080";
        }

        @Override
        protected Object[] fetchModels() throws IOException {
            if (throwOnFetch) {
                throw new IOException("Test exception");
            }
            return modelData;
        }

        @Override
        protected LanguageModel buildLanguageModel(Object model) throws IOException {
            String modelName = (String) model;
            if (modelName.equals("model2")) {
                throw new IOException("Error building model2");
            }

            return LanguageModel.builder()
                    .provider(modelProvider)
                    .modelName(modelName)
                    .displayName(modelName.toUpperCase())
                    .inputCost(0.0)
                    .outputCost(0.0)
                    .inputMaxTokens(4096)
                    .apiKeyUsed(false)
                    .build();
        }

        // Overridden to avoid notification calls in tests
        @Override
        protected void handleModelFetchError(@NotNull IOException e) {
            // Do nothing in test
        }

        // Overridden to avoid notification calls in tests
        @Override
        protected void handleGeneralFetchError(IOException e) {
            // Do nothing in test
        }
    }

    private TestLocalChatModelFactory factory;
    private TestLocalChatModelFactory factoryWithException;

    @Mock
    private CustomChatModel customChatModel;

    @Mock
    private Project defaultProject;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);

        // Initialize factories
        factory = new TestLocalChatModelFactory(false);
        factoryWithException = new TestLocalChatModelFactory(true);

        // Set up common mocks for ChatModel
        when(customChatModel.getModelName()).thenReturn("test-model");
        when(customChatModel.getMaxRetries()).thenReturn(3);
        when(customChatModel.getTemperature()).thenReturn(0.7);
        when(customChatModel.getMaxTokens()).thenReturn(4096);
        when(customChatModel.getTimeout()).thenReturn(60);
        when(customChatModel.getTopP()).thenReturn(0.95);

        // Reset static fields
        try {
            Field warningShownField = LocalChatModelFactory.class.getDeclaredField("warningShown");
            warningShownField.setAccessible(true);
            warningShownField.set(null, false);
        } catch (Exception e) {
            fail("Failed to reset warningShown: " + e.getMessage());
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
    public void testGetModelsSuccess() {
        // Test the method
        List<LanguageModel> result = factory.getModels();

        // Verify results
        assertNotNull(result);
        assertEquals(2, result.size()); // Should be 2, not 3, as model2 throws exception

        // Verify first model
        LanguageModel firstModel = result.stream()
                .filter(m -> m.getModelName().equals("model1"))
                .findFirst()
                .orElse(null);
        assertNotNull(firstModel);
        assertEquals("MODEL1", firstModel.getDisplayName());
        assertEquals(ModelProvider.GPT4All, firstModel.getProvider());

        // Verify provider status
        assertTrue(factory.providerRunning);
        assertTrue(factory.providerChecked);
    }

    @Test
    public void testGetModelsCached() throws IOException {
        // Create spy to verify method calls
        TestLocalChatModelFactory spyFactory = spy(factory);

        // Call once to populate cache
        List<LanguageModel> firstResult = spyFactory.getModels();
        assertNotNull(firstResult);
        assertEquals(2, firstResult.size());

        // Call again - should use cache and not call fetchModels()
        List<LanguageModel> secondResult = spyFactory.getModels();

        // Verify the method was only called once
        verify(spyFactory, times(1)).fetchModels();

        // Verify results are equal (not necessarily same instance)
        assertEquals(firstResult, secondResult);

        // Verify content is the same
        assertEquals(2, secondResult.size());
        assertTrue(secondResult.stream().anyMatch(m -> m.getModelName().equals("model1")));
        assertTrue(secondResult.stream().anyMatch(m -> m.getModelName().equals("model3")));
    }

    @Test
    public void testGetModelsFailure() {
        // Create a version with mocked notification util to verify messages
        TestLocalChatModelFactory testFactory = spy(factoryWithException);

        // Test the method
        List<LanguageModel> result = testFactory.getModels();

        // Verify results
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify provider status
        assertFalse(testFactory.providerRunning);
        assertTrue(testFactory.providerChecked);
    }

    @Test
    public void testGetModelsWithNotificationWhenProviderNotRunning() {
        // Set up a factory in non-running state
        TestLocalChatModelFactory testFactory = spy(factoryWithException);
        testFactory.providerChecked = true;
        testFactory.providerRunning = false;

        try (MockedStatic<NotificationUtil> mockedUtil = mockStatic(NotificationUtil.class);
             MockedStatic<ProjectManager> mockedProjectManager = mockStatic(ProjectManager.class)) {

            // Mock dependencies
            mockedProjectManager.when(ProjectManager::getInstance).thenReturn(mock(ProjectManager.class));
            when(ProjectManager.getInstance().getDefaultProject()).thenReturn(defaultProject);

            // Test the method
            List<LanguageModel> result = testFactory.getModels();

            // Verify results
            assertTrue(result.isEmpty());

            // Verify notification was sent
            mockedUtil.verify(() ->
                    NotificationUtil.sendNotification(
                            any(Project.class),
                            eq("LLM provider is not running. Please start it and try again.")
                    )
            );
        }
    }

    @Test
    public void testResetModels() {
        // Populate models first
        factory.getModels();
        assertTrue(factory.providerChecked);
        assertTrue(factory.providerRunning);
        assertNotNull(factory.cachedModels);

        // Reset models
        factory.resetModels();

        // Verify the state is reset
        assertNull(factory.cachedModels);
        assertFalse(factory.providerChecked);
        assertFalse(factory.providerRunning);
    }
}
