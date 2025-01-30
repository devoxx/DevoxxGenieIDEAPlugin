package com.devoxx.genie.ui;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

final class DevoxxGenieToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project,
                                        @NotNull ToolWindow toolWindow) {
        DevoxxGenieToolWindowContent toolWindowContent = new DevoxxGenieToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);

//        ImageDropPanel imageDropPanel = new ImageDropPanel();
//        new ImagePreviewHandler(imageDropPanel);
//        Content content = ContentFactory.getInstance().createContent(imageDropPanel, "DnD Image", false);
//        toolWindow.getContentManager().addContent(content);
//
        // Subscribe to settings changes
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(AppTopics.SETTINGS_CHANGED_TOPIC, toolWindowContent);
    }
}
