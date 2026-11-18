package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** TASK-277.1: /search uses the shared provider readiness rule and explains what is missing. */
class SearchCommandTest {

    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private MockedStatic<NotificationUtil> notificationStatic;
    private DevoxxGenieStateService state;
    private ChatMessageContext context;
    private final PromptOutputPanel panel = mock(PromptOutputPanel.class);
    private final SearchCommand command = new SearchCommand();

    @BeforeEach
    void setUp() {
        state = new DevoxxGenieStateService();
        state.setIsWebSearchEnabled(true);
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);
        notificationStatic = mockStatic(NotificationUtil.class);

        context = ChatMessageContext.builder()
                .project(mock(Project.class))
                .userPrompt("/search intellij plugin compatibility")
                .build();
    }

    @AfterEach
    void tearDown() {
        notificationStatic.close();
        stateStatic.close();
    }

    private String notification() {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        notificationStatic.verify(() -> NotificationUtil.sendNotification(any(Project.class), message.capture()));
        return message.getValue();
    }

    @Test
    void readyTavily_returnsTheQuery_andMarksTheContext() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-123");

        Optional<String> query = command.process(context, panel);

        assertThat(query).contains("intellij plugin compatibility");
        assertThat(context.isWebSearchRequested()).isTrue();
        notificationStatic.verifyNoInteractions();
    }

    @Test
    void readyGoogle_returnsTheQuery() {
        state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
        state.setGoogleSearchKey("g-key");
        state.setGoogleCSIKey("cse-id");

        assertThat(command.process(context, panel)).contains("intellij plugin compatibility");
    }

    @Test
    void masterSwitchOff_isRejected_evenWithAReadyProvider() {
        state.setIsWebSearchEnabled(false);
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-123");

        assertThat(command.process(context, panel)).isEmpty();
        assertThat(notification()).contains("Web search").contains("Settings → DevoxxGenie → Web search");
        assertThat(context.isWebSearchRequested()).isFalse();
    }

    @Test
    void noProvider_isRejected_withProviderNeutralMessage() {
        state.setSelectedWebSearchProvider(null);

        assertThat(command.process(context, panel)).isEmpty();
        assertThat(notification()).contains("No web search provider is selected");
    }

    @Test
    void selectedTavilyWithBlankKey_isRejected_namingTavilyAndTheKey() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("  ");

        assertThat(command.process(context, panel)).isEmpty();
        assertThat(notification()).contains("Tavily").contains("API key");
    }

    @Test
    void keyOfAnUnselectedProvider_doesNotMakeSearchReady() {
        state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
        state.setTavilySearchKey("tvly-123");

        assertThat(command.process(context, panel)).isEmpty();
        assertThat(notification()).contains("Google").contains("API key").contains("Search Engine ID");
    }

    @Test
    void matches_onlySearchCommand() {
        assertThat(command.matches("/search foo")).isTrue();
        assertThat(command.matches("  /search")).isTrue();
        assertThat(command.matches("/find foo")).isFalse();
        verify(panel, never()).addChatResponse(any());
        notificationStatic.verify(() -> NotificationUtil.sendNotification(any(Project.class), anyString()), never());
    }
}
