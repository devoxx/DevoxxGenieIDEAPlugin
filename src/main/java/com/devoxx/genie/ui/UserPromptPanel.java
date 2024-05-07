package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.DevoxxIcon;

public class UserPromptPanel extends BackgroundPanel {

    public UserPromptPanel(ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());

        setMaximumSize(new Dimension(1500, 75));
        setPreferredSize(new Dimension(500, 50));

        String label = chatMessageContext.getUserMessage().singleText().replace("\n", "<br/>");

        JBLabel userPromptLabel =
            new JBLabel("<html>" + label + "</html>", DevoxxIcon, SwingConstants.LEFT);

        add(userPromptLabel, BorderLayout.NORTH);
    }
}
