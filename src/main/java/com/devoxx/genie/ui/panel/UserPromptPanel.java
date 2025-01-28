package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatMemoryService;
import com.devoxx.genie.ui.component.JEditorPaneUtils;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static com.devoxx.genie.ui.component.button.ButtonUtil.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.DevoxxIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.TrashIcon;

public class UserPromptPanel extends BackgroundPanel {

    private final JPanel container;

    /**
     * The user prompt panel.
     *
     * @param container          the container
     * @param chatMessageContext the chat message context
     */
    public UserPromptPanel(JPanel container,
                           @NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());
        this.container = container;
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(createHeaderLabel(), BorderLayout.WEST);
        headerPanel.add(createDeleteButton(chatMessageContext), BorderLayout.EAST);

        // User prompt setup
        JEditorPane htmlJEditorPane =
            JEditorPaneUtils.createHtmlJEditorPane(
                chatMessageContext.getUserPrompt(),
                null,
                StyleSheetsFactory.createParagraphStyleSheet()
            );

        add(headerPanel, BorderLayout.NORTH);
        add(htmlJEditorPane, BorderLayout.CENTER);
    }

    /**
     * Create the header label.
     */
    private @NotNull JBLabel createHeaderLabel() {
        JBLabel createdOnLabel = new JBLabel("DevoxxGenie", DevoxxIcon, SwingConstants.LEFT);
        createdOnLabel.setFont(createdOnLabel.getFont().deriveFont(12f));
        createdOnLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        return createdOnLabel;
    }

    /**
     * Create the Delete button to remove user prompt & response.
     *
     * @param chatMessageContext the chat message context
     * @return the panel with Delete button
     */
    private @NotNull JButton createDeleteButton(ChatMessageContext chatMessageContext) {
        return createActionButton(TrashIcon, "Remove the prompt * response", e -> removeChat(chatMessageContext));
    }

    /**
     * Remove the chat components based on chat UUID name.
     *
     * @param chatMessageContext the chat message context
     */
    private void removeChat(@NotNull ChatMessageContext chatMessageContext) {
        String nameToRemove = chatMessageContext.getId();
        java.util.List<Component> componentsToRemove = new ArrayList<>();

        for (Component c : container.getComponents()) {
            if (c.getName() != null && c.getName().equals(nameToRemove)) {
                componentsToRemove.add(c);
            }
        }

        for (Component c : componentsToRemove) {
            container.remove(c);
        }

        // Repaint the container
        container.revalidate();
        container.repaint();

        // Remove the chat from memory
        ChatMemoryService.getInstance().remove(chatMessageContext);
    }
}

