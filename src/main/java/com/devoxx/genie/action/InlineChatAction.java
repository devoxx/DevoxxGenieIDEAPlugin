package com.devoxx.genie.action;

import com.devoxx.genie.ui.panel.InlineChatPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class InlineChatAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Create a new InlineChatPanel instance every time the action is performed
        InlineChatPanel inlineChatPanel = new InlineChatPanel();
        inlineChatPanel.initialize(e);
        inlineChatPanel.showPopup(e);
    }
}
