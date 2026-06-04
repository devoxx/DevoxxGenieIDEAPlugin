package com.devoxx.genie.service.prompt.websearch;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.IllegalConfigurationException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSearchPromptExecutionServiceTest {

    @Mock private DevoxxGenieStateService mockStateService;
    @Mock private ChatMessageContext mockContext;
    @Mock private ChatModel mockChatModel;
    @Mock private StreamingChatModel mockStreamingChatModel;
    @Mock private ChatModelProvider mockChatModelProvider;

    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<DevoxxGenieStateService> stateServiceMock;

    private WebSearchPromptExecutionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);

        Application mockApplication = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);

        when(mockContext.getUserPrompt()).thenReturn("test search query");
        when(mockContext.getChatModel()).thenReturn(mockChatModel);

        service = new WebSearchPromptExecutionService();
        service.chatModelProvider = mockChatModelProvider;
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (stateServiceMock != null) stateServiceMock.close();
    }

    @Test
    void searchWeb_withNoSearchEngineConfigured_returnsEmpty() {
        when(mockStateService.isTavilySearchEnabled()).thenReturn(false);
        when(mockStateService.isGoogleSearchEnabled()).thenReturn(false);

        Optional<AiMessage> result = service.searchWeb(mockContext);

        assertThat(result).isEmpty();
    }

    @Test
    void searchWeb_withGoogleSearchDisabled_andTavilyDisabled_returnsEmpty() {
        when(mockStateService.isTavilySearchEnabled()).thenReturn(false);
        when(mockStateService.isGoogleSearchEnabled()).thenReturn(false);

        Optional<AiMessage> result = service.searchWeb(mockContext);

        assertThat(result).isEmpty();
    }

    @Test
    void searchWeb_withGoogleSearchEnabled_butMissingKeys_returnsEmpty() {
        when(mockStateService.isTavilySearchEnabled()).thenReturn(false);
        when(mockStateService.isGoogleSearchEnabled()).thenReturn(true);
        when(mockStateService.getGoogleSearchKey()).thenReturn(null);
        when(mockStateService.getGoogleCSIKey()).thenReturn(null);

        Optional<AiMessage> result = service.searchWeb(mockContext);

        assertThat(result).isEmpty();
    }

    @Test
    void searchWeb_withGoogleSearchEnabled_butMissingCSIKey_returnsEmpty() {
        when(mockStateService.isTavilySearchEnabled()).thenReturn(false);
        when(mockStateService.isGoogleSearchEnabled()).thenReturn(true);
        when(mockStateService.getGoogleSearchKey()).thenReturn("valid-key");
        when(mockStateService.getGoogleCSIKey()).thenReturn(null);

        Optional<AiMessage> result = service.searchWeb(mockContext);

        assertThat(result).isEmpty();
    }

    // Regression test: when streaming mode is active, getChatModel() returns null. The service
    // must obtain a non-streaming chatModel from ChatModelProvider (AiServices requires chatModel
    // for synchronous String-returning methods — streamingChatModel alone is not sufficient).
    @Test
    void searchWeb_withTavilyEnabled_andNullChatModel_usesChatModelProvider() {
        when(mockStateService.isTavilySearchEnabled()).thenReturn(true);
        when(mockStateService.getTavilySearchKey()).thenReturn("test-tavily-key");
        when(mockStateService.getMaxSearchResults()).thenReturn(3);
        when(mockContext.getChatModel()).thenReturn(null);                    // streaming mode: no chatModel
        when(mockChatModelProvider.getChatLanguageModel(mockContext)).thenReturn(mockChatModel);
        when(mockContext.getUserPrompt()).thenReturn("latest news on Iran");

        // The service obtains a chatModel via the provider and calls AiServices.
        // The call to Tavily itself will fail (no real API backend) but must NOT throw
        // IllegalConfigurationException about a missing chatModel.
        assertThatThrownBy(() -> service.searchWeb(mockContext))
                .isNotInstanceOf(IllegalConfigurationException.class);

        // Verify the provider was consulted to obtain the non-streaming model.
        verify(mockChatModelProvider).getChatLanguageModel(mockContext);
    }

    @Test
    void searchWeb_withTavilyEnabled_andExistingChatModel_doesNotConsultProvider() {
        when(mockStateService.isTavilySearchEnabled()).thenReturn(true);
        when(mockStateService.getTavilySearchKey()).thenReturn("test-tavily-key");
        when(mockStateService.getMaxSearchResults()).thenReturn(3);
        when(mockContext.getChatModel()).thenReturn(mockChatModel);           // non-streaming mode: chatModel present
        when(mockContext.getUserPrompt()).thenReturn("latest news on Iran");

        // chatModel is already set — provider must NOT be invoked.
        assertThatThrownBy(() -> service.searchWeb(mockContext))
                .isNotInstanceOf(IllegalConfigurationException.class);

        verify(mockChatModelProvider, never()).getChatLanguageModel(any());
    }
}
