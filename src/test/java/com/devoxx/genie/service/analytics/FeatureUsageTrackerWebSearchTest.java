package com.devoxx.genie.service.analytics;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** TASK-277.6: web search usage is counted per provider, once per completed search. */
class FeatureUsageTrackerWebSearchTest {

    private MockedStatic<AnalyticsService> analyticsStatic;
    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private AnalyticsService analytics;
    private DevoxxGenieStateService state;

    @BeforeEach
    void setUp() {
        analytics = mock(AnalyticsService.class);
        analyticsStatic = mockStatic(AnalyticsService.class);
        analyticsStatic.when(AnalyticsService::getInstance).thenReturn(analytics);
        state = new DevoxxGenieStateService();
        state.setStreamMode(false);
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);
    }

    @AfterEach
    void tearDown() {
        stateStatic.close();
        analyticsStatic.close();
    }

    private static LanguageModel cloudModel() {
        return LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt").build();
    }

    @Test
    void slashSearch_emitsOnlyTheProviderFeature_withTheChatModelsProviderType() {
        FeatureUsageTracker.webSearchUsed(FeatureId.WEB_SEARCH_KEENABLE, false, cloudModel());

        verify(analytics).trackFeatureUsed(FeatureId.WEB_SEARCH_KEENABLE, ProviderType.CLOUD, "0");
        verifyNoMoreInteractions(analytics);
    }

    @Test
    void agentTool_emitsTheProviderFeature_andTheAgentToolFeature() {
        FeatureUsageTracker.webSearchUsed(FeatureId.WEB_SEARCH_PARALLEL, true, null);

        verify(analytics).trackFeatureUsed(FeatureId.WEB_SEARCH_PARALLEL, ProviderType.NONE, "0");
        verify(analytics).trackFeatureUsed(FeatureId.WEB_SEARCH_AGENT_TOOL, ProviderType.NONE, "0");
        verifyNoMoreInteractions(analytics);
    }

    @Test
    void nullFeature_emitsNothing() {
        FeatureUsageTracker.webSearchUsed(null, true, cloudModel());

        verifyNoInteractions(analytics);
    }

    @Test
    void analyticsFailure_neverReachesTheCaller() {
        doThrow(new IllegalStateException("analytics down"))
                .when(analytics).trackFeatureUsed(any(), any(), anyString());

        assertThatCode(() -> FeatureUsageTracker.webSearchUsed(FeatureId.WEB_SEARCH_TAVILY, true, null))
                .doesNotThrowAnyException();
    }

    @Test
    void promptCompletion_noLongerEmitsWebSearch_soASearchIsNeverCountedTwice() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(cloudModel())
                .webSearchActivated(true)
                .build();

        FeatureUsageTracker.emitForPrompt(context);

        verify(analytics, never()).trackFeatureUsed(eq(FeatureId.WEB_SEARCH_TAVILY), any(), anyString());
    }
}
