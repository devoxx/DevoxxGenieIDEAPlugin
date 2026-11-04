package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.rag.ProjectIndexerService;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

/**
 * After the RAG embedding model changes (TASK-212), tells the user the project's index was
 * built with another model and offers a one-click re-index. Never re-indexes on its own: a
 * full re-embed can take many minutes.
 *
 * <p>Until the user re-indexes, search keeps querying the index with the model that built it,
 * so nothing breaks in the meantime.
 */
@Slf4j
final class RagIndexStaleNotifier {

    private static final String NOTIFICATION_GROUP = "com.devoxx.genie.notifications";

    private RagIndexStaleNotifier() {}

    /**
     * Checks the project's index off the EDT (the manifest may be read from disk) and shows
     * the notification when it was built with a model other than the configured one.
     */
    static void notifyIfStale(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Optional<String> indexFingerprint =
                        IndexManifestService.getInstance().forProject(project).indexFingerprint();
                EmbeddingFingerprint configured = ChromaEmbeddingService.getInstance().configuredFingerprint();
                staleIndexModel(indexFingerprint, configured)
                        .ifPresent(indexModel -> show(project, indexModel, configured.modelName()));
            } catch (Exception e) {
                log.warn("Could not check whether the RAG index matches the embedding model: {}", e.getMessage());
            }
        });
    }

    /** The model the index was built with, when that isn't the configured one. */
    static @NotNull Optional<String> staleIndexModel(@NotNull Optional<String> indexFingerprint,
                                                     @NotNull EmbeddingFingerprint configured) {
        return indexFingerprint
                .map(EmbeddingFingerprint::parse)
                .filter(index -> !index.equals(configured))
                .map(EmbeddingFingerprint::modelName);
    }

    static @NotNull String message(@NotNull String indexModel, @NotNull String configuredModel) {
        return "This project's RAG index was built with " + indexModel + ". Re-index to use "
                + configuredModel + "; until then, search keeps using " + indexModel + ".";
    }

    private static void show(@NotNull Project project, @NotNull String indexModel, @NotNull String configuredModel) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP)
                    .createNotification("RAG embedding model changed",
                            message(indexModel, configuredModel), NotificationType.INFORMATION);
            notification.addAction(NotificationAction.createSimpleExpiring("Re-index now",
                    () -> reindex(project)));
            notification.notify(project);
        });
    }

    private static void reindex(@NotNull Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Re-indexing project for RAG", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    // The settings panel may be closed; progress shows in the IDE status bar.
                    ProjectIndexerService.getInstance().indexFiles(project, true, new JProgressBar(), new JLabel());
                } catch (ProcessCanceledException pce) {
                    throw pce;
                } catch (Exception e) {
                    log.warn("RAG re-index failed: {}", e.getMessage());
                }
            }

            @Override
            public void onCancel() {
                ProjectIndexerService.getInstance().cancelIndexing();
            }
        });
    }
}
