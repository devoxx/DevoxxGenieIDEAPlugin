package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.panel.chatresponse.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ChatResponsePanel extends BackgroundPanel {

    private final transient ChatMessageContext chatMessageContext;

    public ChatResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());
        this.chatMessageContext = chatMessageContext;
        setLayout(new GridBagLayout());
        buildResponsePanel();
    }

    private void buildResponsePanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1; // full width components
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally
        gbc.anchor = GridBagConstraints.WEST; // Anchor to the west (left)

        add(new ResponseHeaderPanel(chatMessageContext), gbc);

        gbc.gridy++;
        add(new ResponseContentPanel(chatMessageContext), gbc);

        gbc.gridy++;
        add(new FileListPanel(chatMessageContext, FileListManager.getInstance().getFiles()), gbc);

        gbc.gridy++;
        add(new SemanticSearchReferencesPanel(chatMessageContext, chatMessageContext.getSemanticReferences()), gbc);

        gbc.gridy++;
        JPanel metricPanelWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        metricPanelWrapper.add(new MetricExecutionInfoPanel(chatMessageContext));
        add(metricPanelWrapper, gbc);
    }
}
