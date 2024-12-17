package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.List;

// FileListPanel.java
public class FileListPanel extends JPanel {
    public FileListPanel(ChatMessageContext chatMessageContext, List<VirtualFile> files) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        if (chatMessageContext.hasFiles()) {
            add(new ExpandablePanel(chatMessageContext, files));
        }
    }
}
