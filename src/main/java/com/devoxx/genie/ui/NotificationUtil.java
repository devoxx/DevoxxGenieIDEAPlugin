package com.devoxx.genie.ui;

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;

public class NotificationUtil {

    private NotificationUtil() {
    }

    public static void sendNotification(Project project, String content) {
        NotificationGroup notificationGroup =
            NotificationGroupManager.getInstance().getNotificationGroup("com.devoxx.genie.notifications");
        Notification notification = notificationGroup.createNotification(content, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);
    }
}
