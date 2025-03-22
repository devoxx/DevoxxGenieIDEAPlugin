package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.JEditorPaneUtils;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
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
        headerPanel.add(createHeaderLabel(), BorderLayout.WEST);
        headerPanel.add(createDeleteButton(chatMessageContext), BorderLayout.EAST);

        String userPrompt = chatMessageContext.getUserPrompt().replace("\n", "<br>");

        // User prompt setup
        JEditorPane htmlJEditorPane =
            JEditorPaneUtils.createHtmlJEditorPane(
                userPrompt,
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
        // Use the new PromptOutputPanel method which handles both UI and memory cleanup
        PromptOutputPanel outputPanel = findPromptOutputPanel();
        if (outputPanel != null) {
            outputPanel.removeConversationItem(chatMessageContext, true);
        } else {
            // Fallback to manual removal if we can't find the output panel
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
        }
    }
    
    /**
     * Find the parent PromptOutputPanel in the component hierarchy
     * 
     * @return the PromptOutputPanel or null if not found
     */
    private PromptOutputPanel findPromptOutputPanel() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof PromptOutputPanel) {
                return (PromptOutputPanel) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
}

