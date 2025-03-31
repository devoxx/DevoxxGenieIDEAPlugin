package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.JEditorPaneUtils;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.DevoxxIcon;

public class UserPromptPanel extends BackgroundPanel {

    /**
     * The user prompt panel.
     *
     * @param chatMessageContext the chat message context
     */
    public UserPromptPanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(createHeaderLabel(), BorderLayout.WEST);

        String userPrompt = chatMessageContext.getUserPrompt().replace("\n", "<br>");

        // User prompt setup
        JEditorPane htmlJEditorPane =
            JEditorPaneUtils.createHtmlJEditorPane(
                userPrompt,
                null,
                StyleSheetsFactory.createParagraphStyleSheet()
            );

        htmlJEditorPane.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 10));

        add(headerPanel, BorderLayout.NORTH);
        add(htmlJEditorPane, BorderLayout.CENTER);
    }

    /**
     * Create the header label.
     */
    private @NotNull JBLabel createHeaderLabel() {
        JBLabel createdOnLabel = new JBLabel("DevoxxGenie", DevoxxIcon, SwingConstants.LEFT);
        createdOnLabel.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 10));
        return createdOnLabel;
    }
}

