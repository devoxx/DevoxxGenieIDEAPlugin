package com.devoxx.genie.ui.panel.conversationhistory;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.conversations.PendingConversationDeletionManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntSupplier;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.TrashIcon;

public class ConversationHistoryPanel extends JPanel implements ConversationSelectionListener {
    private final transient ConversationStorageService storageService;
    private final ConversationTableModel tableModel;
    private final JBTable table;
    private final transient Project project;
    private final String tabId;
    private transient JBPopup activePopup;
    private transient ConversationSelectionListener directSelectionListener;

    /** Row currently under the mouse pointer, or -1; drives the hover highlight. */
    private int hoveredRow = -1;

    public ConversationHistoryPanel(Project project) {
        this(project, null);
    }

    public ConversationHistoryPanel(Project project, String tabId) {
        this.project = project;
        this.tabId = tabId;

        setLayout(new BorderLayout());

        storageService = ConversationStorageService.getInstance();

        // Create table model
        tableModel = new ConversationTableModel();
        table = new JBTable(tableModel);

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
                    openConversationAt(row);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredRow(-1);
            }
        });

        // Track the row under the mouse so renderers can paint a hover highlight
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoveredRow(table.rowAtPoint(e.getPoint()));
            }
        });

        // Keyboard support: Enter opens the selected conversation, Delete removes it (undoable)
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openConversation");
        table.getActionMap().put("openConversation", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openConversationAt(table.getSelectedRow());
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteConversation");
        table.getActionMap().put("deleteConversation", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    deleteWithUndo(tableModel.getConversationAt(row));
                }
            }
        });

        // Configure columns
        TableColumnModel columnModel = table.getColumnModel();
        IntSupplier hoveredRowSupplier = () -> hoveredRow;

        // Title column
        TableColumn titleColumn = columnModel.getColumn(1);
        titleColumn.setCellRenderer(new TitleRenderer(hoveredRowSupplier));

        // Set a very large preferred width to encourage expansion
        titleColumn.setPreferredWidth(Integer.MAX_VALUE);
        titleColumn.setWidth(Integer.MAX_VALUE);

        // Ensure this column gets any extra space
        table.getColumnModel().getColumn(2).setResizable(false);

        // Delete button column 0
        TableColumn deleteColumn = columnModel.getColumn(0);
        deleteColumn.setMinWidth(40);
        deleteColumn.setMaxWidth(40);
        deleteColumn.setCellRenderer(new ButtonRenderer(AllIcons.Actions.GC, hoveredRowSupplier));

        JButton deleteButton = new JButton(AllIcons.Actions.GC);
        deleteButton.setBorder(BorderFactory.createEmptyBorder());
        deleteButton.setPreferredSize(new Dimension(40, 40));
        deleteButton.setMaximumSize(new Dimension(40, 40));

        ButtonEditor buttonEditor = new ButtonEditor(deleteButton, conversation -> {
            deleteWithUndo(conversation);
            return true;
        });

        deleteColumn.setCellEditor(buttonEditor);

        // Time column 2
        TableColumn timeColumn = columnModel.getColumn(2);
        timeColumn.setMinWidth(100);
        timeColumn.setMaxWidth(100);
        timeColumn.setCellRenderer(new TimeRenderer(hoveredRowSupplier));

        // Add table to scroll pane
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        add(scrollPane, BorderLayout.CENTER);

        // Add delete all button
        add(createActionButton("Delete All", TrashIcon, e -> showDeleteAllConfirmationDialog()), BorderLayout.SOUTH);

        loadConversations();
    }

    /**
     * Set a direct selection listener to avoid broadcasting via message bus.
     * When set, conversation selection is routed directly to this listener
     * instead of fan-out to all tabs.
     */
    public void setDirectSelectionListener(ConversationSelectionListener listener) {
        this.directSelectionListener = listener;
    }

    public void setPopup(JBPopup popup) {
        this.activePopup = popup;
    }

    public void loadConversations() {
        List<Conversation> conversations = storageService.getConversations(project);
        // Conversations parked for (undoable) deletion are hidden but not yet removed
        // from storage — see PendingConversationDeletionManager.
        Set<String> pendingDeletion = PendingConversationDeletionManager.getInstance().pendingIds();
        if (!pendingDeletion.isEmpty()) {
            conversations.removeIf(c -> pendingDeletion.contains(c.getId()));
        }
        conversations.sort((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp()));
        hoveredRow = -1;
        tableModel.setConversations(conversations);
        tableModel.fireTableDataChanged();
    }

    private void setHoveredRow(int row) {
        if (row != hoveredRow) {
            hoveredRow = row;
            table.repaint();
        }
    }

    /** Opens the conversation at {@code row}: restores chat memory, notifies, closes the popup. */
    private void openConversationAt(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        Conversation conversation = tableModel.getConversationAt(row);
        updateChatMemory(conversation);
        onConversationSelected(conversation);
        if (activePopup != null && !activePopup.isDisposed()) {
            activePopup.cancel();
        }
    }

    @Override
    public void onConversationSelected(Conversation conversation) {
        // Use direct listener if available (tab-scoped, avoids cross-tab fan-out)
        if (directSelectionListener != null) {
            directSelectionListener.onConversationSelected(conversation);
            return;
        }

        // Fallback: broadcast via message bus (legacy path)
        Project currentProject = this.project;
        if (currentProject != null) {
            currentProject.getMessageBus()
                .syncPublisher(AppTopics.CONVERSATION_SELECTION_TOPIC)
                .onConversationSelected(conversation);
        }
    }

    // Table model class
    private static class ConversationTableModel extends AbstractTableModel {
        private transient List<Conversation> conversations = new ArrayList<>();
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
    }

    // Custom renderers
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        private final transient IntSupplier hoveredRowSupplier;

        public ButtonRenderer(Icon icon, IntSupplier hoveredRowSupplier) {
            super(icon);
            this.hoveredRowSupplier = hoveredRowSupplier;
            setPreferredSize(new Dimension(40, 40));
            setMaximumSize(new Dimension(40, 40));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            applyHoverBackground(this, table, isSelected, row, hoveredRowSupplier.getAsInt());
            return this;
        }
    }

    /** Shared hover/selection background logic for all renderers in this table. */
    private static void applyHoverBackground(@NotNull JComponent component,
                                             @NotNull JTable table,
                                             boolean isSelected,
                                             int row,
                                             int hoveredRow) {
        component.setOpaque(true);
        if (isSelected) {
            component.setBackground(table.getSelectionBackground());
        } else if (row == hoveredRow) {
            component.setBackground(JBUI.CurrentTheme.Table.Hover.background(true));
        } else {
            component.setBackground(table.getBackground());
        }
    }

    private static class TitleRenderer extends DefaultTableCellRenderer {
        private static final int MAX_TITLE_LENGTH = 50;
        private final transient IntSupplier hoveredRowSupplier;

        public TitleRenderer(IntSupplier hoveredRowSupplier) {
            this.hoveredRowSupplier = hoveredRowSupplier;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.LEADING);
            
            // Truncate long titles
            String fullText = String.valueOf(value);
            String displayText = fullText.length() > MAX_TITLE_LENGTH ? 
                fullText.substring(0, MAX_TITLE_LENGTH) + "..." : 
                fullText;
            
            setText(displayText);
            setMinimumSize(new Dimension(0, getHeight()));
            setBorder(JBUI.Borders.empty(0, 8));
            
            // Store the full text as tooltip
            setToolTipText(fullText);

            applyHoverBackground(this, table, isSelected, row, hoveredRowSupplier.getAsInt());

            return this;
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
        private final transient IntSupplier hoveredRowSupplier;

        public TimeRenderer(IntSupplier hoveredRowSupplier) {
            this.hoveredRowSupplier = hoveredRowSupplier;
            setForeground(JBColor.GRAY);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(JBUI.Borders.empty(0, 8));
            applyHoverBackground(this, table, isSelected, row, hoveredRowSupplier.getAsInt());
            return this;
        }
    }

    // Custom editor for buttons
    private static class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private final transient Function<Conversation, Boolean> action;
        private transient Conversation currentConversation;

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
        // Use tab-scoped memory key if available
        if (tabId != null) {
            String memoryKey = project.getLocationHash() + "-" + tabId;
            ChatMemoryManager.getInstance().restoreConversationByKey(memoryKey, conversation);
        } else {
            ChatMemoryManager.getInstance().restoreConversation(project, conversation);
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

    /**
     * Undoable deletion (task-236): the row disappears immediately, but the SQLite delete is
     * deferred for a grace period during which an "Undo" notification action restores it.
     */
    private void deleteWithUndo(@NotNull Conversation conversation) {
        PendingConversationDeletionManager deletionManager = PendingConversationDeletionManager.getInstance();
        if (deletionManager.isPendingDeletion(conversation.getId())) {
            return;
        }

        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("com.devoxx.genie.notifications")
                .createNotification("Conversation deleted", NotificationType.INFORMATION);
        notification.addAction(NotificationAction.createSimpleExpiring("Undo", () -> {
            if (deletionManager.undo(conversation.getId())) {
                loadConversations();
            }
        }));

        deletionManager.scheduleDeletion(project, conversation, () ->
                // Commit runs on a scheduler thread; expiring the balloon must happen on the EDT.
                ApplicationManager.getApplication().invokeLater(notification::expire));

        // Hide the row right away — loadConversations() filters ids pending deletion.
        loadConversations();
        Notifications.Bus.notify(notification, project);
    }

    private void removeAllConversations() {
        storageService.clearAllConversations(project);
        loadConversations();
        NotificationUtil.sendNotification(project, "All conversations removed");
    }
}
