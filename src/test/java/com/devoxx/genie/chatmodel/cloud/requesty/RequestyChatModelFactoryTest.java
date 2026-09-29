package com.devoxx.genie.chatmodel.cloud.requesty;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.requesty.Data;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RequestyChatModelFactoryTest extends BasePlatformTestCase {

    private RequestyChatModelFactory factory;

    @Mock
    private RequestyService requestyService;

    @Mock
    private CustomChatModel customChatModel;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);

        DevoxxGenieStateService stateServiceMock = mock(DevoxxGenieStateService.class);
        when(stateServiceMock.getRequestyKey()).thenReturn("dummy-api-key");

        ServiceContainerUtil.replaceService(
                ApplicationManager.getApplication(),
                DevoxxGenieStateService.class,
                stateServiceMock,
                getTestRootDisposable()
        );

        factory = new RequestyChatModelFactory() {
            @Override
            protected void handleModelFetchError(IOException e) {
                // No notification in tests
            }
        };

        when(customChatModel.getModelName()).thenReturn("openai/gpt-4o-mini");
        when(customChatModel.getMaxRetries()).thenReturn(3);
        when(customChatModel.getTemperature()).thenReturn(0.7);
        when(customChatModel.getTimeout()).thenReturn(60);
        when(customChatModel.getTopP()).thenReturn(0.95);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
    public void testGetModelsMapsCatalogEntries() throws IOException {
        try (MockedStatic<RequestyService> mockedService = mockStatic(RequestyService.class)) {
            mockedService.when(RequestyService::getInstance).thenReturn(requestyService);
            when(requestyService.getModels(anyString())).thenReturn(createTestModels());

            List<LanguageModel> result = factory.getModels();

            assertEquals(2, result.size());

            LanguageModel first = result.get(0);
            assertEquals(ModelProvider.Requesty, first.getProvider());
            assertEquals("openai/gpt-4o-mini", first.getModelName());
            assertEquals("openai/gpt-4o-mini", first.getDisplayName());
            assertEquals(0.15, first.getInputCost(), 0.000001);
            assertEquals(0.6, first.getOutputCost(), 0.000001);
            assertEquals(128000, first.getInputMaxTokens());
            assertTrue(first.isApiKeyUsed());

            LanguageModel second = result.get(1);
            assertEquals("anthropic/claude-sonnet-4-5", second.getModelName());
            assertEquals(3.0, second.getInputCost(), 0.000001);
            assertEquals(15.0, second.getOutputCost(), 0.000001);
            assertEquals(200000, second.getInputMaxTokens());

            // Second call is served from the cache
            factory.getModels();
            verify(requestyService, times(1)).getModels(anyString());

            // resetModels clears the cache
            factory.resetModels();
            factory.getModels();
            verify(requestyService, times(2)).getModels(anyString());
        }
    }

    @Test
    public void testGetModelsHandlesException() throws IOException {
        try (MockedStatic<RequestyService> mockedService = mockStatic(RequestyService.class)) {
            mockedService.when(RequestyService::getInstance).thenReturn(requestyService);
            when(requestyService.getModels(anyString())).thenThrow(new IOException("Network error"));

            List<LanguageModel> result = factory.getModels();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(requestyService, times(1)).getModels(anyString());
        }
    }

    private List<Data> createTestModels() {
        List<Data> models = new ArrayList<>();

        Data chat = new Data();
        chat.setId("openai/gpt-4o-mini");
        chat.setApi("chat");
        chat.setContextWindow(128000);
        chat.setInputPrice(0.00000015);
        chat.setOutputPrice(0.0000006);
        models.add(chat);

        Data embedding = new Data();
        embedding.setId("openai/text-embedding-3-small");
        embedding.setApi("embedding");
        embedding.setInputPrice(0.00000002);
        models.add(embedding);

        Data anthropic = new Data();
        anthropic.setId("anthropic/claude-sonnet-4-5");
        anthropic.setApi("chat");
        anthropic.setContextWindow(200000);
        anthropic.setInputPrice(0.000003);
        anthropic.setOutputPrice(0.000015);
        models.add(anthropic);

        return models;
    }
}
