package com.devoxx.genie.ui.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public class WindowPluginUtil {

    public static final String TOOL_WINDOW_ID = "DevoxxGenie";

    /**
     * Open the tool window if it is not visible.
     *
     * @param project the project
     */
    public static void ensureToolWindowVisible(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);

        if (toolWindow != null && !toolWindow.isVisible()) {
            toolWindow.show(null);
        }
    }
}
