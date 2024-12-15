package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.panel.chatresponse.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ChatResponsePanel extends BackgroundPanel {

    private final transient ChatMessageContext chatMessageContext;

    public ChatResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());
        this.chatMessageContext = chatMessageContext;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildResponsePanel();
    }

    private void buildResponsePanel() {
        add(new ResponseHeaderPanel(chatMessageContext));
        add(new ResponseContentPanel(chatMessageContext));
        add(new FileListPanel(chatMessageContext, FileListManager.getInstance().getFiles()));
        add(new SemanticSearchReferencesPanel(chatMessageContext, chatMessageContext.getSemanticReferences()));
        add(new MetricExecutionInfoPanel(chatMessageContext));
    }
}
