package com.devoxx.genie.service.rag;

import com.devoxx.genie.model.rag.RAGLogMessage;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RAGEventPublisherTest {

    private MockedStatic<ApplicationManager> applicationManagerStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private Application application;
    private MessageBus messageBus;
    private final List<RAGLogMessage> publishedMessages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        applicationManagerStatic = Mockito.mockStatic(ApplicationManager.class);
        stateServiceStatic = Mockito.mockStatic(DevoxxGenieStateService.class);

        application = mock(Application.class);
        messageBus = mock(MessageBus.class);
        DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
        MessageBusConnection connection = mock(MessageBusConnection.class);

        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);
        when(application.getMessageBus()).thenReturn(messageBus);
        when(messageBus.connect()).thenReturn(connection);

        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getIndexerMinScore()).thenReturn(0.7);
        when(stateService.getIndexerMaxResults()).thenReturn(10);

        // Capture whatever is published — we use a fake RAGLoggingMessage as the syncPublisher proxy.
        RAGLoggingMessage capturing = publishedMessages::add;
        when(messageBus.syncPublisher(AppTopics.RAG_LOG_MSG)).thenReturn(capturing);
    }

    @AfterEach
    void tearDown() {
        applicationManagerStatic.close();
        stateServiceStatic.close();
    }

    @Test
    void publishCarriesQueryHitsAndDurationToSubscribers() {
        Project project = mock(Project.class);
        when(project.getLocationHash()).thenReturn("PROJECT_HASH");

        List<SearchResult> results = List.of(
                new SearchResult("/abs/Foo.java", 0.91, "alpha chunk text"),
                new SearchResult("/abs/Bar.java", 0.82, "beta chunk text")
        );

        RAGEventPublisher.publish(project, "How does authentication work?", results, 137L);

        assertThat(publishedMessages).hasSize(1);
        RAGLogMessage m = publishedMessages.get(0);
        assertThat(m.getProjectLocationHash()).isEqualTo("PROJECT_HASH");
        assertThat(m.getQuery()).isEqualTo("How does authentication work?");
        assertThat(m.getDurationMs()).isEqualTo(137L);
        assertThat(m.getMinScore()).isEqualTo(0.7);
        assertThat(m.getMaxResults()).isEqualTo(10);
        assertThat(m.getHits()).hasSize(2);
        assertThat(m.getHits()).extracting(RAGLogMessage.Hit::getFilePath)
                .containsExactly("/abs/Foo.java", "/abs/Bar.java");
        assertThat(m.getHits().get(0).getPreview()).isEqualTo("alpha chunk text");
        assertThat(m.getHits().get(0).getChunkLength()).isEqualTo("alpha chunk text".length());
    }

    @Test
    void publishTruncatesPreviewsToProtectPanelMemory() {
        Project project = mock(Project.class);
        when(project.getLocationHash()).thenReturn("PROJECT_HASH");

        StringBuilder huge = new StringBuilder();
        huge.append("x".repeat(RAGEventPublisher.PREVIEW_MAX_CHARS + 200));
        List<SearchResult> results = List.of(new SearchResult("/abs/Huge.java", 0.95, huge.toString()));

        RAGEventPublisher.publish(project, "huge chunk", results, 10L);

        assertThat(publishedMessages).hasSize(1);
        RAGLogMessage.Hit hit = publishedMessages.get(0).getHits().get(0);
        assertThat(hit.getPreview().length())
                .as("preview must be truncated to PREVIEW_MAX_CHARS (+1 ellipsis); full text stays in the prompt")
                .isEqualTo(RAGEventPublisher.PREVIEW_MAX_CHARS + 1);
        assertThat(hit.getChunkLength())
                .as("chunkLength reflects the original full chunk size, not the truncated preview")
                .isEqualTo(huge.length());
    }

    @Test
    void publishWithEmptyResultsStillEmitsAnEventSoLogShowsZeroHits() {
        Project project = mock(Project.class);
        when(project.getLocationHash()).thenReturn("PROJECT_HASH");

        RAGEventPublisher.publish(project, "no matches expected", List.of(), 12L);

        assertThat(publishedMessages).hasSize(1);
        assertThat(publishedMessages.get(0).getHits()).isEmpty();
    }
}
