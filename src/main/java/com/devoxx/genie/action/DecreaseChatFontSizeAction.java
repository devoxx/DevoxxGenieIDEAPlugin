package com.devoxx.genie.action;

import com.devoxx.genie.ui.util.ChatFontSizeService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Decreases the chat output font size (prose and code) by one step.
 */
public class DecreaseChatFontSizeAction extends AbstractChatFontSizeAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ChatFontSizeService.decrease();
    }
}
