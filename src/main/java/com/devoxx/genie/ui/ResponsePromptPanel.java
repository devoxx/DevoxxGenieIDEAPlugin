package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;

import java.awt.*;

public class ResponsePromptPanel extends BackgroundPanel {

    public ResponsePromptPanel(ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());

        if (chatMessageContext.getEditorInfo() != null &&
            chatMessageContext.getEditorInfo().getSelectedFiles() != null &&
            !chatMessageContext.getEditorInfo().getSelectedFiles().isEmpty()) {
            ExpandablePanel fileListPanel = new ExpandablePanel(chatMessageContext);
            add(fileListPanel, BorderLayout.SOUTH);
        }
    }
}
