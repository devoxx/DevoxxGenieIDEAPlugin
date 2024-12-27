package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;

public class FileListPanel extends ExpandablePanel {

    public FileListPanel(ChatMessageContext chatMessageContext) {
        super(chatMessageContext, FileListManager.getInstance().getFiles(chatMessageContext.getProject()));
    }
}
