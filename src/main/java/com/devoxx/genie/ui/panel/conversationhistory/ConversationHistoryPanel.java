package com.devoxx.genie.ui.panel.conversationhistory;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.prompt.ChatMemoryService;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.TrashIcon;

public class ConversationHistoryPanel extends JPanel {
    private final ConversationStorageService storageService;
    private final ConversationTableModel tableModel;
    private final Project project;

    public ConversationHistoryPanel(@NotNull ConversationStorageService storageService,
                                    ConversationSelectionListener conversationSelectionListener,
                                    Project project) {
        this.storageService = storageService;
        this.project = project;

        setLayout(new BorderLayout());

        // Create table model
        tableModel = new ConversationTableModel();
        JTable table = new JBTable(tableModel);

        // Configure table properties
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowHeight(50);
        table.setTableHeader(null); // Hide header since we don't need it
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Ensure the table uses all available width
        table.setFillsViewportHeight(true);

        // Configure selection
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);

        // Style the table
        table.setBackground(UIUtil.getPanelBackground());
        table.setBorder(JBUI.Borders.empty());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row >= 0 && column != 0) {
                    // Get the conversation from the model
                    Conversation conversation = tableModel.getConversationAt(row);
                    // Update chat memory
                    updateChatMemory(conversation);
                    // Notify listener
                    conversationSelectionListener.onConversationSelected(conversation);
                }
            }
        });

        // Configure columns
        TableColumnModel columnModel = table.getColumnModel();

        // Title column
        TableColumn titleColumn = columnModel.getColumn(1);
        titleColumn.setCellRenderer(new TitleRenderer());

        // Set a very large preferred width to encourage expansion
        titleColumn.setPreferredWidth(Integer.MAX_VALUE);
        titleColumn.setWidth(Integer.MAX_VALUE);

        // Ensure this column gets any extra space
        table.getColumnModel().getColumn(2).setResizable(false);

        // Delete button column 0
        TableColumn deleteColumn = columnModel.getColumn(0);
        deleteColumn.setMinWidth(40);
        deleteColumn.setMaxWidth(40);
        deleteColumn.setCellRenderer(new ButtonRenderer(AllIcons.Actions.GC));

        JButton deleteButton = new JButton(AllIcons.Actions.GC);
        deleteButton.setBorder(BorderFactory.createEmptyBorder());
        deleteButton.setPreferredSize(new Dimension(40, 40));
        deleteButton.setMaximumSize(new Dimension(40, 40));

        ButtonEditor buttonEditor = new ButtonEditor(deleteButton, conversation -> {
            removeConversation(conversation);
            return true;
        });

        deleteColumn.setCellEditor(buttonEditor);

        // Time column 2
        TableColumn timeColumn = columnModel.getColumn(2);
        timeColumn.setMinWidth(100);
        timeColumn.setMaxWidth(100);
        timeColumn.setCellRenderer(new TimeRenderer());

        // Add table to scroll pane
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        add(scrollPane, BorderLayout.CENTER);

        // Add delete all button
        add(createActionButton("Delete All", TrashIcon, e -> showDeleteAllConfirmationDialog()), BorderLayout.SOUTH);

        loadConversations();
    }

    public void loadConversations() {
        List<Conversation> conversations = storageService.getConversations(project);
        conversations.sort((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp()));
        tableModel.setConversations(conversations);
        tableModel.fireTableDataChanged();
    }

    // Table model class
    private static class ConversationTableModel extends AbstractTableModel {
        private List<Conversation> conversations = new ArrayList<>();
        private final String[] columnNames = {"Delete", "Title", "Time"};

        public void setConversations(List<Conversation> conversations) {
            // Create new list to ensure clean state
            this.conversations = new ArrayList<>(conversations);
            fireTableDataChanged();
        }

        public Conversation getConversationAt(int row) {
            return conversations.get(row);
        }

        @Override
        public int getRowCount() {
            return conversations.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Conversation conversation = conversations.get(row);

            if (column == 0) {
                // For button columns, create a defensive copy to ensure ID consistency
                Conversation copy = new Conversation();
                copy.setId(conversation.getId());
                copy.setTitle(conversation.getTitle());
                // Copy other necessary fields
                return copy;
            }

            return switch (column) {
                case 1 -> conversation.getTitle();
                case 2 -> formatTimeSince(conversation.getTimestamp());
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0; // Only delete buttons are editable
        }
    }

    // Custom renderers
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(Icon icon) {
            super(icon);
            setPreferredSize(new Dimension(40, 40));
            setMaximumSize(new Dimension(40, 40));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            return this;
        }
    }

    private static class TitleRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.LEADING);
            setText(String.valueOf(value));
            setMinimumSize(new Dimension(0, getHeight()));
            setBorder(JBUI.Borders.empty(0, 8));

            return this;
        }

        @Override
        public void setText(String text) {
            super.setText(text);
             setToolTipText(text);
        }

        // Override these methods to ensure proper text display
        @Override
        public boolean isOpaque() {
            return true;
        }

        @Override
        public @NotNull Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            // Allow the preferred width to be as large as needed
            return new Dimension(d.width, d.height);
        }
    }

    private static class TimeRenderer extends DefaultTableCellRenderer {
        public TimeRenderer() {
            setForeground(JBColor.GRAY);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(JBUI.Borders.empty(0, 8));
            return this;
        }
    }

    // Custom editor for buttons
    private static class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private final Function<Conversation, Boolean> action;
        private Conversation currentConversation;

        public ButtonEditor(@NotNull JButton button,
                            Function<Conversation, Boolean> action) {
            super(new JCheckBox());
            this.button = button;
            this.action = action;
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentConversation = (Conversation) value;
            return button;
        }

        @Override
        public @Nullable Object getCellEditorValue() {
            if (currentConversation != null) {
                action.apply(currentConversation);
            }
            return null;
        }
    }

    private void updateChatMemory(Conversation conversation) {
        ChatMemoryService.getInstance().restoreConversation(project, conversation);
    }

    private static @NotNull String formatTimeSince(String timestamp) {
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
        try {
            storageService.removeConversation(project, conversation);
            loadConversations();
            NotificationUtil.sendNotification(project, "Conversation removed");
        } catch (Exception e) {
            NotificationUtil.sendNotification(project, "Failed to remove conversation: " + e.getMessage());
        }
    }

    private void removeAllConversations() {
        storageService.clearAllConversations(project);
        loadConversations();
        NotificationUtil.sendNotification(project, "All conversations removed");
    }
}
