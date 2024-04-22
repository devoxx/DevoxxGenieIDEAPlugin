package com.devoxx.genie.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class AddToContextAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO Add code to context
        System.out.println("Add to context: " + e);
    }
}
