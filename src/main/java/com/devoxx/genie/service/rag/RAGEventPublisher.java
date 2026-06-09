package com.devoxx.genie.service.rag;

import com.devoxx.genie.model.rag.RAGLogMessage;
import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Publishes RAG retrieval events to the unified log panel and to slf4j. Replaces the
 * single "Found N relevant project files" notification — which told you that something
 * happened but not what — with a structured event carrying the query, configured
 * thresholds, and one entry per matched chunk (path, score, content preview).
 *
 * <p>Subscribers receive {@link RAGLogMessage}s via {@link AppTopics#RAG_LOG_MSG}.
 */
@Slf4j
public final class RAGEventPublisher {

    /** Max characters of chunk text shown inline in the log panel; full text stays in the prompt. */
    public static final int PREVIEW_MAX_CHARS = 400;

    private RAGEventPublisher() {}

    /**
     * Publish a completed RAG retrieval. Safe to call from any thread; falls back to
     * a debug-level log line if the message bus is unavailable (e.g. headless tests).
     */
    public static void publish(@Nullable Project project,
                               @NotNull String query,
                               @NotNull List<SearchResult> results,
                               long durationMs) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        RAGLogMessage.RAGLogMessageBuilder builder = RAGLogMessage.builder()
                .projectLocationHash(project != null ? project.getLocationHash() : null)
                .query(query)
                .embeddingModel(safeEmbeddingModelName())
                .minScore(state != null ? state.getIndexerMinScore() : null)
                .maxResults(state != null ? state.getIndexerMaxResults() : null)
                .durationMs(durationMs);

        for (SearchResult r : results) {
            String content = r.content() != null ? r.content() : "";
            String preview = content.length() > PREVIEW_MAX_CHARS
                    ? content.substring(0, PREVIEW_MAX_CHARS) + "…"
                    : content;
            builder.hit(RAGLogMessage.Hit.builder()
                    .filePath(r.filePath())
                    .score(r.score())
                    .preview(preview)
                    .chunkLength(content.length())
                    .preRerankRank(r.preRerankRank())
                    .rerankerScore(r.rerankerScore())
                    .build());
        }

        RAGLogMessage message = builder.build();

        // slf4j first — survives headless tests and idea.log searches ("RAG retrieval" grep).
        log.info("RAG retrieval: query=\"{}\" hits={} duration={}ms",
                summarizeQuery(query), results.size(), durationMs);
        for (SearchResult r : results) {
            if (r.rerankerScore() != null) {
                log.info("RAG hit  score={}  rerank={}  preRank={}  file={}",
                        formatScore(r.score()),
                        formatScore(r.rerankerScore()),
                        r.preRerankRank() == null ? "n/a" : r.preRerankRank(),
                        r.filePath());
            } else {
                log.info("RAG hit  score={}  file={}", formatScore(r.score()), r.filePath());
            }
        }

        try {
            // syncPublisher returns a proxy that broadcasts to all subscribers on the topic.
            // Guard for tests where ApplicationManager isn't initialised.
            if (ApplicationManager.getApplication() == null) return;
            MessageBus bus = ApplicationManager.getApplication().getMessageBus();
            if (bus == null) return;
            bus.syncPublisher(AppTopics.RAG_LOG_MSG).onRAGLoggingMessage(message);
        } catch (Exception e) {
            log.debug("Could not publish RAG event to message bus: {}", e.getMessage());
        }
    }

    private static String safeEmbeddingModelName() {
        try {
            return ChromaEmbeddingService.EMBEDDING_MODEL_NAME;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String summarizeQuery(@NotNull String query) {
        String oneLine = query.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() > 120 ? oneLine.substring(0, 117) + "..." : oneLine;
    }

    private static String formatScore(@Nullable Double score) {
        return score == null ? "n/a" : String.format("%.3f", score);
    }
}
