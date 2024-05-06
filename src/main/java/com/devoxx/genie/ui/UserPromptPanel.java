package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.PromptContext;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.DevoxxIcon;

public class UserPromptPanel extends BackgroundPanel {

    public UserPromptPanel(PromptContext promptContext) {
        super(promptContext.getName());

        setMaximumSize(new Dimension(1500, 75));
        setPreferredSize(new Dimension(500, 50));

        String label = promptContext.getUserPrompt().replace("\n", "<br/>");

        JBLabel userPromptLabel =
            new JBLabel("<html>" + label + "</html>", DevoxxIcon, SwingConstants.LEFT);

        add(userPromptLabel, BorderLayout.NORTH);
    }
}
