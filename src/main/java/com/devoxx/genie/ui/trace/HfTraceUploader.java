package com.devoxx.genie.ui.trace;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.devoxx.genie.service.trace.TraceSettings;
import com.devoxx.genie.service.trace.hf.HfDatasetTarget;
import com.devoxx.genie.service.trace.hf.HfTokenResolver;
import com.devoxx.genie.service.trace.hf.HfUploadSettings;
import com.devoxx.genie.service.trace.hf.HubApiException;
import com.devoxx.genie.service.trace.hf.HuggingFaceHubClient;
import com.devoxx.genie.service.trace.hf.TraceUploadService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Uploads conversations to Hugging Face from the UI (task-280): the chat window's upload button
 * and the conversation history panel both go through here. Runs the first-use setup dialog
 * when the token or dataset is missing, asks for confirmation (unless turned off), uploads in a
 * cancellable background task and reports the result as a notification with a link to the Hub.
 * Must be called on the EDT.
 */
@Slf4j
public final class HfTraceUploader {

    private static final String NOTIFICATION_GROUP = "com.devoxx.genie.notifications";

    private HfTraceUploader() {}

    /**
     * @param onStarted  runs on the EDT once the upload actually starts (after the dialogs)
     * @param onFinished runs on the EDT when the upload ends, successfully or not; not called when
     *                   the user cancels a dialog
     */
    public static void upload(@NotNull Project project, @NotNull List<Conversation> conversations,
                              @NotNull Runnable onStarted, @NotNull Runnable onFinished) {
        if (conversations.isEmpty()) {
            return;
        }
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        TraceSettings trace = TraceSettings.fromState(state);
        HfUploadSettings settings = HfUploadSettings.fromState(state);
        HfTokenResolver.ResolvedToken resolved = HfTokenResolver.system().resolve();
        String token = resolved == null ? null : resolved.token();

        if (token == null || settings.target() == null) {
            HfUploadSetupDialog dialog = new HfUploadSetupDialog(project, token == null, settings);
            if (!dialog.showAndGet()) {
                return;
            }
            if (dialog.token() != null) {
                token = dialog.token();
                CredentialService.getInstance().setCredential(CredentialKey.HUGGING_FACE_TOKEN, token);
            }
            state.setAgentTraceHfPrivate(dialog.isPrivateDataset());
            HfDatasetTarget chosen = dialog.target();
            if (chosen != null) {
                state.setAgentTraceHfTarget(chosen.repoId());
            }
            settings = HfUploadSettings.fromState(state);
        }
        if (token == null) {
            return;
        }

        if (settings.confirmBeforeUpload()) {
            String where = settings.target() != null
                    ? settings.target().repoId()
                    : "<your-hf-user>/" + HfUploadSettings.DEFAULT_DATASET_NAME;
            String what = conversations.size() == 1
                    ? "this conversation's trace"
                    : conversations.size() + " conversation traces";
            int answer = Messages.showOkCancelDialog(project,
                    "Upload " + what + " to the Hugging Face dataset " + where
                            + (settings.isPrivate() ? " (private if it has to be created)?" : " (public if it has to be created)?")
                            + "\n\nTraces are redacted, but can still contain code and prompts."
                            + "\nYou can turn this confirmation off in Settings → DevoxxGenie → Debug.",
                    "Upload to Hugging Face", "Upload", "Cancel", Messages.getQuestionIcon());
            if (answer != Messages.OK) {
                return;
            }
        }

        onStarted.run();
        String finalToken = token;
        HfUploadSettings finalSettings = settings;
        new Task.Backgroundable(project, "Uploading agent trace to Hugging Face", true) {
            private TraceUploadService.Outcome outcome;
            private String error;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                HuggingFaceHubClient client = new HuggingFaceHubClient(finalSettings.hubUrl());
                try {
                    HfUploadSettings s = finalSettings;
                    if (s.target() == null) {
                        indicator.setText("Checking the Hugging Face account…");
                        HfDatasetTarget target = new HfDatasetTarget(client.whoAmI(finalToken),
                                HfUploadSettings.DEFAULT_DATASET_NAME);
                        s = s.withTarget(target, s.isPrivate());
                        ApplicationManager.getApplication().invokeLater(() ->
                                DevoxxGenieStateService.getInstance().setAgentTraceHfTarget(target.repoId()));
                    }
                    indicator.setText("Uploading to " + s.target().repoId() + "…");
                    TraceUploadService service = new TraceUploadService(client,
                            id -> ConversationStorageService.getInstance().getTraceEvents(id));
                    outcome = service.upload(conversations, finalToken, s, trace, project.getName(), indicator::isCanceled);
                } catch (HubApiException e) {
                    error = e.getMessage();
                } catch (RuntimeException e) {
                    log.warn("Hugging Face trace upload failed", e);
                    error = "Upload failed: " + e.getMessage();
                }
            }

            @Override
            public void onFinished() {
                try {
                    if (error != null) {
                        showNotification(project, error, NotificationType.ERROR, null);
                    } else if (outcome != null) {
                        report(project, outcome);
                    }
                } finally {
                    onFinished.run();
                }
            }
        }.queue();
    }

    private static void report(@NotNull Project project, @NotNull TraceUploadService.Outcome outcome) {
        String heldBack = outcome.heldBack().isEmpty() ? "" : "<br>Held back: " + outcome.heldBack().stream()
                .map(h -> escape(h.title() == null ? h.conversationId() : h.title()) + " (" + escape(h.reason()) + ")")
                .collect(Collectors.joining("; "));
        if (outcome.uploadedPaths().isEmpty()) {
            showNotification(project, "Nothing was uploaded to " + outcome.target().repoId() + "." + heldBack,
                    NotificationType.WARNING, null);
            return;
        }
        String count = outcome.uploadedPaths().size() == 1 ? "Trace uploaded" : outcome.uploadedPaths().size() + " traces uploaded";
        showNotification(project, count + " to " + outcome.target().repoId()
                        + (outcome.createdDataset() ? " (new dataset)" : "") + "." + heldBack,
                outcome.heldBack().isEmpty() ? NotificationType.INFORMATION : NotificationType.WARNING,
                outcome.firstFileUrl());
    }

    private static void showNotification(@NotNull Project project, @NotNull String content, @NotNull NotificationType type,
                               @Nullable String url) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification("Hugging Face upload", content, type);
        if (url != null) {
            notification.addAction(NotificationAction.createSimpleExpiring("Open on Hugging Face",
                    () -> BrowserUtil.browse(url)));
        }
        Notifications.Bus.notify(notification, project);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
