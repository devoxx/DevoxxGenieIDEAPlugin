package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.listener.ChatChangeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.TrashIcon;

public class ResponseHeaderPanel extends JBPanel<ResponseHeaderPanel> {

    private final JComponent container;

    public ResponseHeaderPanel(ChatMessageContext chatMessageContext,
                               JComponent container) {
        super(new BorderLayout());
        this.container = container;

        andTransparent()
        .withMaximumSize(500, 30)
        .withPreferredHeight(30)
        .withPreferredWidth(500);

        String modelInfo = (chatMessageContext.getLlmProvider() != null ? chatMessageContext.getLlmProvider() : "") +
            (chatMessageContext.getModelName() != null ? " - " + chatMessageContext.getModelName() : "");

        String label = chatMessageContext.getCreatedOn().format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")) + " : " + modelInfo;
        JBLabel createdOnLabel = new JBLabel(label);
        createdOnLabel.setFont(createdOnLabel.getFont().deriveFont(12f));
        createdOnLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        add(createdOnLabel, BorderLayout.WEST);

        if (container != null) {
            JButton deleteButton = getDeleteButton(chatMessageContext);
            add(deleteButton, BorderLayout.EAST);
        }
    }

    private @NotNull JButton getDeleteButton(ChatMessageContext chatMessageContext) {
        JButton deleteButton = new JHoverButton(TrashIcon, true);
        deleteButton.setToolTipText("Remove the prompt & response");
        deleteButton.addActionListener(e -> removeComponent(chatMessageContext));
        return deleteButton;
    }

    private void removeComponent(ChatMessageContext chatMessageContext) {
        // Get all children of container and delete by name
        Arrays.stream(container.getComponents())
            .filter(c -> c.getName() != null && c.getName().equals(chatMessageContext.getName()))
            .forEach(container::remove);

        container.revalidate();
        container.repaint();

        // Delete from message list
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        ChatChangeListener chatChangeListener = bus.syncPublisher(AppTopics.CHAT_MESSAGES_CHANGED_TOPIC);
        chatChangeListener.removeMessagePair(chatMessageContext);
    }
}
