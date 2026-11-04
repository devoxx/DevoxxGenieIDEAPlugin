package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.rag.ProjectIndexerService;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.embedding.EmbeddingModelInstaller;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ide.util.PropertiesComponent;
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
public final class RagIndexStaleNotifier {

    private static final String NOTIFICATION_GROUP = "com.devoxx.genie.notifications";
    private static final String TOLD_KEY = "com.devoxx.genie.rag.staleIndexNotified";

    private RagIndexStaleNotifier() {}

    /**
     * After a settings change: checks the project's index off the EDT (the manifest may be read
     * from disk) and shows the notification when another embedding setup built it.
     */
    static void notifyIfStale(@NotNull Project project) {
        checkAndNotify(project, false);
    }

    /**
     * At project startup: tells an upgraded install once that its index can be improved. Repeated
     * only if the index or the settings change, not on every IDE start, however the user responds.
     */
    public static void notifyOnceOnStartup(@NotNull Project project) {
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagEnabled())) {
            checkAndNotify(project, true);
        }
    }

    private static void checkAndNotify(@NotNull Project project, boolean onlyIfNotToldYet) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Optional<String> indexFingerprint =
                        IndexManifestService.getInstance().forProject(project).indexFingerprint();
                EmbeddingFingerprint configured = ChromaEmbeddingService.getInstance().configuredFingerprint();
                staleIndex(indexFingerprint, configured).ifPresent(index -> {
                    PropertiesComponent properties = PropertiesComponent.getInstance(project);
                    String told = notificationKey(index, configured);
                    if (onlyIfNotToldYet && told.equals(properties.getValue(TOLD_KEY))) {
                        return;
                    }
                    properties.setValue(TOLD_KEY, told);
                    show(project, index, configured);
                });
            } catch (Exception e) {
                log.warn("Could not check whether the RAG index matches the embedding setup: {}", e.getMessage());
            }
        });
    }

    /** The setup the index was built with, when that isn't the configured one. */
    static @NotNull Optional<EmbeddingFingerprint> staleIndex(@NotNull Optional<String> indexFingerprint,
                                                              @NotNull EmbeddingFingerprint configured) {
        return indexFingerprint
                .map(EmbeddingFingerprint::parse)
                .filter(index -> !index.equals(configured));
    }

    static @NotNull String notificationKey(@NotNull EmbeddingFingerprint index,
                                           @NotNull EmbeddingFingerprint configured) {
        return index.id() + " -> " + configured.id();
    }

    static @NotNull String message(@NotNull EmbeddingFingerprint index, @NotNull EmbeddingFingerprint configured) {
        boolean sameModel = index.provider().equals(configured.provider())
                && index.modelName().equals(configured.modelName());
        if (!sameModel) {
            return "This project's RAG index was built with " + index.modelName() + ". Re-index to use "
                    + configured.modelName() + "; until then, search keeps using " + index.modelName() + ".";
        }
        if (index.equals(EmbeddingFingerprint.LEGACY)) {
            return "Re-index this project to turn on " + index.modelName() + "'s recommended prefixes, which "
                    + "improve search results. Until then, search keeps using your existing index.";
        }
        return "This project's RAG index was built with different embedding settings (prefix or output "
                + "dimension). Re-index to apply them; until then, search keeps using the settings that built it.";
    }

    private static void show(@NotNull Project project,
                             @NotNull EmbeddingFingerprint index,
                             @NotNull EmbeddingFingerprint configured) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP)
                    .createNotification("Re-index for RAG embedding settings",
                            message(index, configured), NotificationType.INFORMATION);
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
                    EmbeddingModelInstaller.Result model = EmbeddingModelDownloadPrompt.ensureInstalled(project, null, status -> {
                        indicator.checkCanceled();
                        indicator.setText(status);
                    });
                    if (!model.ready()) {
                        NotificationUtil.sendNotification(project, model.problem());
                        return;
                    }
                    // The settings panel may be closed; progress shows in the IDE status bar.
                    ProjectIndexerService.getInstance().indexFiles(project, true, new JProgressBar(), new JLabel());
                } catch (ProcessCanceledException pce) {
                    throw pce;
                } catch (Exception e) {
                    log.warn("RAG re-index failed: {}", e.getMessage());
                    NotificationUtil.sendNotification(project, "RAG re-index failed: " + e.getMessage());
                }
            }

            @Override
            public void onCancel() {
                ProjectIndexerService.getInstance().cancelIndexing();
            }
        });
    }
}
