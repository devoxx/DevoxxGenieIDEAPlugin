package com.devoxx.genie.service.analytics;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Shows the first-launch analytics consent notification exactly once per install (task-206).
 *
 * <p>Until the user acknowledges (via either inline action or by visiting Settings → DevoxxGenie
 * → General), {@link AnalyticsService} refuses to emit any event. The notification therefore
 * gates all telemetry on informed consent.
 */
public final class AnalyticsConsentNotifier {

    private static final String NOTIFICATION_GROUP_ID = "com.devoxx.genie.notifications";

    private static final String TITLE = "DevoxxGenie usage analytics";

    private static final String CONTENT =
            "<html>To guide which LLM providers and models we invest engineering effort in, " +
                    "DevoxxGenie collects <b>anonymous</b> usage data when you run a prompt or change models:" +
                    "<ul>" +
                    "<li>Anonymous install ID, per-launch session ID, plugin version, IDE version</li>" +
                    "<li>LLM provider name and model name</li>" +
                    "</ul>" +
                    "<b>We never send</b> prompt text, response text, file content, file paths, project names, " +
                    "API keys, or anything that could identify you. " +
                    "You can change this any time in <i>Settings → DevoxxGenie → General</i>." +
                    "</html>";

    private AnalyticsConsentNotifier() {
    }

    public static void maybeShow(@NotNull Project project) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        // Show exactly once per install: gate on analyticsNoticeShown, not on Acknowledged.
        // Acknowledged still gates emission (informed-consent rule), but the notice itself
        // never re-appears for users who saw it once and dismissed without clicking.
        if (Boolean.TRUE.equals(state.getAnalyticsNoticeShown())) {
            return;
        }
        // Mark as shown synchronously, BEFORE scheduling the EDT task, so concurrent project
        // openings during startup never race into showing the balloon twice.
        state.setAnalyticsNoticeShown(true);

        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(TITLE, CONTENT, NotificationType.INFORMATION);

            notification.addAction(new AnAction("OK, Keep Enabled") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    DevoxxGenieStateService.getInstance().setAnalyticsNoticeAcknowledged(true);
                    // Analytics just became eligible — emit the feature-enablement snapshot (task-209).
                    AnalyticsSessionSnapshotService.getInstance().snapshotIfNeeded();
                    notification.expire();
                }
            });

            notification.addAction(new AnAction("Disable") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
                    s.setAnalyticsEnabled(false);
                    s.setAnalyticsNoticeAcknowledged(true);
                    notification.expire();
                }
            });

            Notifications.Bus.notify(notification, project);
        });
    }
}
