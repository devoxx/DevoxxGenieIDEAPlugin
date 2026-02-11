package com.devoxx.genie.ui.util;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

public class NotificationUtil {

    private NotificationUtil() {
    }

    public static void sendNotification(Project project, String content) {
        sendNotification(project, content, NotificationType.INFORMATION);
    }

    public static void sendWarningNotification(Project project, String content) {
        sendNotification(project, content, NotificationType.WARNING);
    }

    public static void sendErrorNotification(Project project, String content) {
        sendNotification(project, content, NotificationType.ERROR);
    }

    private static void sendNotification(Project project, String content, NotificationType type) {
        // Ensure notifications are shown on the EDT
        ApplicationManager.getApplication().invokeLater(() -> {
            NotificationGroup notificationGroup =
                NotificationGroupManager.getInstance().getNotificationGroup("com.devoxx.genie.notifications");
            Notification notification = notificationGroup.createNotification(content, type);
            Notifications.Bus.notify(notification, project);
        });
    }
}
