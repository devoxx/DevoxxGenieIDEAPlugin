package com.devoxx.genie.action;

import com.devoxx.genie.ui.util.WindowPluginUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for chat-output font-size shortcuts.
 * <p>
 * The action is only enabled while the DevoxxGenie tool window is active, so the CMD/Ctrl +/-
 * shortcut passes through to the editor (or wherever focus is) when the chat is not in use.
 */
public abstract class AbstractChatFontSizeAction extends DumbAwareAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = false;
        if (project != null) {
            ToolWindow toolWindow =
                ToolWindowManager.getInstance(project).getToolWindow(WindowPluginUtil.TOOL_WINDOW_ID);
            enabled = toolWindow != null && toolWindow.isActive();
        }
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
