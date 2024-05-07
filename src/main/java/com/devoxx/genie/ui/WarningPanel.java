package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.RoundBorder;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

public class WarningPanel extends BackgroundPanel {

    public WarningPanel(String warning,
                        ChatMessageContext chatMessageContext,
                        String text) {
        super(warning);

        withMaximumSize(1500, 75)
            .withBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(JBColor.RED, 1, 5),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

        withPreferredHeight(75);
        withMinimumHeight(50);

        JBLabel jLabel = new JBLabel(text, SwingConstants.LEFT);

        JBScrollPane scrollPane = new JBScrollPane(jLabel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(new ResponseHeaderPanel(chatMessageContext, null), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
}
