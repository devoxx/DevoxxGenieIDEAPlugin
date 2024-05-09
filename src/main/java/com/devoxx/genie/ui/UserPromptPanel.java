package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.JEditorPaneUtils;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.devoxx.genie.ui.listener.ChatChangeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.DevoxxIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.TrashIcon;

public class UserPromptPanel extends BackgroundPanel {

    private final JPanel container;

    public UserPromptPanel(JPanel container, @NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());
        this.container = container;
        setLayout(new BorderLayout());

        JPanel iconPromptPanel = new JPanel(new BorderLayout());
        iconPromptPanel.setOpaque(false);

        // Icon Label setup
        JLabel iconLabel = new JLabel(DevoxxIcon);
        iconLabel.setHorizontalAlignment(JLabel.LEFT);
        iconLabel.setVerticalAlignment(JLabel.TOP);
        iconPromptPanel.add(iconLabel, BorderLayout.WEST);

        // User prompt setup
        JEditorPane htmlJEditorPane =
            JEditorPaneUtils.createHtmlJEditorPane(chatMessageContext.getUserMessage().singleText(), null, StyleSheetsFactory.createParagraphStyleSheet());
        iconPromptPanel.add(htmlJEditorPane, BorderLayout.CENTER);

        JPanel userPromptPanel = new JPanel(new BorderLayout());
        userPromptPanel.setOpaque(false);
        userPromptPanel.add(iconPromptPanel, BorderLayout.WEST);

        // Delete button setup
        JPanel deleteButtonPanel = createDeleteButton(chatMessageContext);
        add(deleteButtonPanel, BorderLayout.EAST);
        add(userPromptPanel, BorderLayout.CENTER);
    }

    private @NotNull JPanel createDeleteButton(ChatMessageContext chatMessageContext) {
        JButton deleteButton = new JHoverButton(TrashIcon, true);
        deleteButton.setToolTipText("Remove the prompt & response");
        deleteButton.addActionListener(e -> removeChat(chatMessageContext));

        JPanel deleteButtonPanel = new JPanel(new BorderLayout());
        deleteButtonPanel.setOpaque(false);
        deleteButtonPanel.add(deleteButton, BorderLayout.NORTH); // Should align to top-right

        return deleteButtonPanel;
    }

    /**
     * Remove the component.
     * @param chatMessageContext the chat message context
     */
    private void removeChat(ChatMessageContext chatMessageContext) {

        // Get all container components and delete by name
        Arrays.stream(container.getComponents())
            .filter(c -> c.getName() != null && c.getName().equals(chatMessageContext.getName()))
            .forEach(container::remove);

        container.revalidate();
        container.repaint();

        // Trigger the chat message change listener
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        ChatChangeListener chatChangeListener = bus.syncPublisher(AppTopics.CHAT_MESSAGES_CHANGED_TOPIC);
        chatChangeListener.removeMessagePair(chatMessageContext);
    }
}

