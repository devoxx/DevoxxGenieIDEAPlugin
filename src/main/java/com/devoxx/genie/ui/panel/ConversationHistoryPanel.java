package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.ChatMemoryService;
import com.devoxx.genie.service.ConversationStorageService;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ConversationHistoryPanel extends JPanel {
    private final ConversationStorageService storageService;
    private final ConversationSelectionListener conversationSelectionListener;
    private final JPanel conversationsPanel;
    private final Project project;

    public ConversationHistoryPanel(@NotNull ConversationStorageService storageService,
                                    ConversationSelectionListener conversationSelectionListener,
                                    Project project) {
        this.storageService = storageService;
        this.conversationSelectionListener = conversationSelectionListener;
        this.project = project;

        setLayout(new BorderLayout());

        conversationsPanel = new JPanel();
        conversationsPanel.setLayout(new BoxLayout(conversationsPanel, BoxLayout.Y_AXIS));

        JBScrollPane scrollPane = new JBScrollPane(conversationsPanel);
        add(scrollPane, BorderLayout.CENTER);

        JButton deleteAll = new JButton("Delete All");
        deleteAll.addActionListener(e -> showDeleteAllConfirmationDialog());

        add(deleteAll, BorderLayout.SOUTH);

        loadConversations();
    }

    public void loadConversations() {
        conversationsPanel.removeAll();
        List<Conversation> conversations = storageService.getConversations(project);

        for (Conversation conversation : conversations) {
            conversationsPanel.add(createConversationRow(conversation));
        }

        revalidate();
        repaint();
    }

    private @NotNull JPanel createConversationRow(@NotNull Conversation conversation) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setPreferredSize(new Dimension(0, 50));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        rowPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.LIGHT_GRAY));

        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(conversation.getTitle());
        titleLabel.addMouseListener( new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                conversationSelectionListener.onConversationSelected(conversation);
                updateChatMemory(conversation);
            }
        });
        titleLabel.setBorder(JBUI.Borders.empty(5, 8));
        infoPanel.add(titleLabel, BorderLayout.CENTER);

        JButton viewButton = new JHoverButton(DevoxxGenieIconsUtil.EyeIcon, true);
        viewButton.setToolTipText("View");
        viewButton.addActionListener(e -> conversationSelectionListener.onConversationSelected(conversation));
        infoPanel.add(viewButton, BorderLayout.WEST);

        JLabel timeLabel = new JLabel(formatTimeSince(conversation.getTimestamp()));
        timeLabel.setForeground(JBColor.GRAY);
        timeLabel.setBorder(JBUI.Borders.empty(5, 8));
        infoPanel.add(timeLabel, BorderLayout.EAST);

        rowPanel.add(infoPanel, BorderLayout.CENTER);

        JButton deleteButton = new JHoverButton(AllIcons.Actions.GC, true);
        deleteButton.setToolTipText("Delete");
        deleteButton.addActionListener(e -> removeConversation(conversation));

        rowPanel.add(deleteButton, BorderLayout.EAST);

        return rowPanel;
    }

    private void updateChatMemory(Conversation conversation) {
        ChatMemoryService.getInstance().restoreConversation(project, conversation);
    }

    private @NotNull String formatTimeSince(String timestamp) {
        try {
            LocalDateTime messageTime = LocalDateTime.parse(timestamp);
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(messageTime, now);

            long minutes = duration.toMinutes();
            if (minutes < 1) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
            } else {
                long hours = duration.toHours();
                if (hours < 24) {
                    return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
                } else {
                    long days = duration.toDays();
                    return days + " day" + (days != 1 ? "s" : "") + " ago";
                }
            }
        } catch (Exception e) {
            return "";
        }
    }

    private void showDeleteAllConfirmationDialog() {
        int result = Messages.showYesNoDialog(
            (Project) null,
            "Are you sure you want to delete all conversations?",
            "Delete Conversation",
            Messages.getQuestionIcon()
        );

        if (result == Messages.YES) {
            removeAllConversations();
        }
    }

    private void removeConversation(Conversation conversation) {
        storageService.removeConversation(project, conversation);
        loadConversations();
    }

    private void removeAllConversations() {
        storageService.clearAllConversations(project);
        loadConversations();
    }
}
