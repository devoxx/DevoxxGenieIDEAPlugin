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

    private static final String TITLE = "Help shape DevoxxGenie";

    private static final String ANALYTICS_SOURCE_URL =
            "https://github.com/devoxx/DevoxxGenieIDEAPlugin/blob/master/" +
                    "src/main/java/com/devoxx/genie/service/analytics/AnalyticsEventBuilder.java";

    private static final String CONTENT =
            "<html>" +
                    "We're a small open-source team, and <b>anonymous</b> usage data is the only way " +
                    "we know which features are actually worth our time. " +
                    "<b>No prompts, no code, no file paths, no API keys</b> — ever. " +
                    "Just things like which LLM provider you picked and whether RAG is enabled." +
                    "<br><br>" +
                    "See exactly what we send: " +
                    "<a href=\"" + ANALYTICS_SOURCE_URL + "\">AnalyticsEventBuilder.java</a>" +
                    "<br><br>" +
                    "You can turn this off any time in <i>Settings → DevoxxGenie → Analytics</i>." +
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

            notification.addAction(new AnAction("Sure, help out") {
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
