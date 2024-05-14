package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.RoundBorder;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

public class WarningPanel extends BackgroundPanel {

    /**
     * Something went wrong, create a warning panel
     * @param warning the warning message
     * @param chatMessageContext the chat message context
     */
    public WarningPanel(String warning,
                        ChatMessageContext chatMessageContext) {
        super(warning);
        setName(chatMessageContext.getName());
        setLayout(new BorderLayout());
        withMaximumSize(1500, 75)
            .withBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(JBColor.RED, 1, 5),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

        withPreferredHeight(75);
        withMinimumHeight(50);

        JBLabel jLabel = new JBLabel(warning, SwingConstants.LEFT);

        JBScrollPane scrollPane = new JBScrollPane(jLabel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(new ResponseHeaderPanel(chatMessageContext), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
}
