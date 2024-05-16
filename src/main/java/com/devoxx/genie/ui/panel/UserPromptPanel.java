package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.JEditorPaneUtils;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.devoxx.genie.ui.listener.ChatChangeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.DevoxxIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.TrashIcon;

public class UserPromptPanel extends BackgroundPanel {

    private final JPanel container;

    /**
     * The user prompt panel.
     * @param container the container
     * @param chatMessageContext the chat message context
     */
    public UserPromptPanel(JPanel container,
                           @NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());
        this.container = container;
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(createHeaderLabel(chatMessageContext), BorderLayout.WEST);
        headerPanel.add(createDeleteButton(chatMessageContext), BorderLayout.EAST);

        // User prompt setup
        JEditorPane htmlJEditorPane =
            JEditorPaneUtils.createHtmlJEditorPane(chatMessageContext.getUserPrompt(), null, StyleSheetsFactory.createParagraphStyleSheet());

        add(headerPanel, BorderLayout.NORTH);
        add(htmlJEditorPane, BorderLayout.CENTER);
    }

    /**
     * Create the header label.
     * @param chatMessageContext the chat message context
     */
    private JBLabel createHeaderLabel(@NotNull ChatMessageContext chatMessageContext) {
        JBLabel createdOnLabel = new JBLabel("DevoxxGenie", DevoxxIcon, SwingConstants.LEFT);
        createdOnLabel.setFont(createdOnLabel.getFont().deriveFont(12f));
        createdOnLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        return createdOnLabel;
    }

    /**
     * Create the Delete button to remove user prompt & response.
     * @param chatMessageContext the chat message context
     * @return the panel with Delete button
     */
    private @NotNull JButton createDeleteButton(ChatMessageContext chatMessageContext) {
        JButton deleteButton = new JHoverButton(TrashIcon, true);
        deleteButton.setToolTipText("Remove the prompt & response");
        deleteButton.addActionListener(e -> removeChat(chatMessageContext));
        return deleteButton;
    }

    /**
     * Remove the chat components based on chat UUID name.
     * @param chatMessageContext the chat message context
     */
    private void removeChat(ChatMessageContext chatMessageContext) {

        // Get all container components and delete by name
        Arrays.stream(container.getComponents())
            .filter(c -> c.getName() != null && c.getName().equals(chatMessageContext.getName()))
            .forEach(container::remove);

        // Repaint the container
        container.revalidate();
        container.repaint();

        // Broadcast that the chat message has been removed, this way the chat memory can be updated
        notifyChatMessageRemoval(chatMessageContext);
    }

    /**
     * Notify the chat message removal.
     * @param chatMessageContext the chat message context
     */
    private void notifyChatMessageRemoval(ChatMessageContext chatMessageContext) {
        // Trigger the chat message change listener
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        ChatChangeListener chatChangeListener = bus.syncPublisher(AppTopics.CHAT_MESSAGES_CHANGED_TOPIC);
        chatChangeListener.removeMessagePair(chatMessageContext);
    }
}

