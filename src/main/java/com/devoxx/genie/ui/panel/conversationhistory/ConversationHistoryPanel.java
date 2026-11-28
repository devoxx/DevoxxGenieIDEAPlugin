package com.devoxx.genie.ui.panel.conversationhistory;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.conversations.PendingConversationDeletionManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.trace.TraceSettings;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.trace.TraceExportActions;
import com.devoxx.genie.ui.trace.view.AgentRunViewer;
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
import com.intellij.ui.ClickListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

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
    /** Column (model index) under the mouse pointer, or -1; drives the per-icon highlight. */
    private int hoveredColumn = -1;

    // Table columns (model indices): title and time on the left, the row actions on the right
    // (TASK-283). The actions show only on the hovered or selected row.
    static final int COL_TITLE = 0;
    static final int COL_TIME = 1;
    static final int COL_VIEW_RUN = 2;
    static final int COL_EXPORT = 3;
    static final int COL_UPLOAD = 4;
    static final int COL_DELETE = 5;
    private static final int ICON_COLUMN_WIDTH = 28;
    private static final int ICON_HOVER_SIZE = 22;
    private static final int TIME_COLUMN_WIDTH = 80;
    /** Room left free right of the last icon for an overlay scrollbar (macOS draws it over the content). */
    private static final int SCROLLBAR_GAP = 14;
    /** Whether the trace actions (view, export, upload) are offered; trace recording must be on. */
    private final boolean traceActions;
    /** Conversations with a recorded agent trace, loaded with the list so the menu never queries on the EDT. */
    private Set<String> tracedConversationIds = Set.of();

    public ConversationHistoryPanel(Project project) {
        this(project, null);
    }

    public ConversationHistoryPanel(Project project, String tabId) {
        this.project = project;
        this.tabId = tabId;
        this.traceActions = TraceSettings.current().enabled();

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

        // ClickListener, not mouseClicked: AWT drops mouseClicked when the pointer moves at all
        // between press and release (trackpad jitter), which made the row icons ignore clicks.
        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent e, int clickCount) {
                // Left click only. Trace actions are row icons (TASK-283), not a right-click menu: on
                // macOS a secondary click also opened the conversation and hid the menu.
                if (!SwingUtilities.isLeftMouseButton(e) || e.isPopupTrigger() || e.isControlDown()) {
                    return false;
                }
                // The second click of a double-click would hit the next row after a delete.
                if (clickCount > 1) {
                    return true;
                }
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || row >= tableModel.getRowCount()) {
                    return false;
                }
                int column = modelColumnAt(e.getPoint());
                if (isActionShown(column, row)) {
                    runRowAction(column, row);
                } else {
                    // Title, time, or an action cell that doesn't apply to this row.
                    openConversationAt(row);
                }
                return true;
            }
        }.installOn(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                setHovered(-1, -1);
            }
        });

        // Track the cell under the mouse so renderers can paint the row and icon hover highlights
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHovered(table.rowAtPoint(e.getPoint()), modelColumnAt(e.getPoint()));
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

        // Row actions on the right (TASK-283): one flat icon style for all four, shown on the
        // hovered or selected row only. The trace ones need trace recording to be on.
        configureIconColumn(columnModel.getColumn(COL_VIEW_RUN), traceActions,
                new ActionIconRenderer(AllIcons.Actions.Show, "View agent run", 0));
        configureIconColumn(columnModel.getColumn(COL_EXPORT), traceActions,
                new ActionIconRenderer(AllIcons.Actions.Download, "Export as trace…", 0));
        configureIconColumn(columnModel.getColumn(COL_UPLOAD), traceActions,
                new ActionIconRenderer(AllIcons.Actions.Upload, "Upload to Hugging Face…", 0));
        // The last icon keeps a trailing gap: macOS overlay scrollbars are laid over the
        // viewport's right edge and would otherwise cover half the trash icon.
        configureIconColumn(columnModel.getColumn(COL_DELETE), true,
                new ActionIconRenderer(AllIcons.Actions.GC, "Delete conversation", SCROLLBAR_GAP));

        // Title column
        TableColumn titleColumn = columnModel.getColumn(COL_TITLE);
        titleColumn.setCellRenderer(new TitleRenderer(hoveredRowSupplier));

        // Set a very large preferred width to encourage expansion
        titleColumn.setPreferredWidth(Integer.MAX_VALUE);
        titleColumn.setWidth(Integer.MAX_VALUE);

        // Time column: compact relative time ("30m ago") so it fits without truncation
        TableColumn timeColumn = columnModel.getColumn(COL_TIME);
        timeColumn.setResizable(false);
        timeColumn.setMinWidth(JBUI.scale(TIME_COLUMN_WIDTH));
        timeColumn.setMaxWidth(JBUI.scale(TIME_COLUMN_WIDTH));
        timeColumn.setCellRenderer(new TimeRenderer(hoveredRowSupplier));

        // Add table to scroll pane
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        // A wheel scroll moves rows under a still pointer without any mouseMoved event.
        scrollPane.getViewport().addChangeListener(e -> refreshHoverFromPointer());
        add(scrollPane, BorderLayout.CENTER);

        // Bottom buttons: Delete All, plus trace export/upload while trace recording is on (task-280)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        bottom.add(createActionButton("Delete All", TrashIcon, e -> showDeleteAllConfirmationDialog()));
        if (TraceSettings.current().enabled()) {
            bottom.add(createActionButton("Export All Traces…", AllIcons.Actions.Download,
                    e -> runTraceAction(() -> TraceExportActions.export(project, tableModel.getConversations()))));
            bottom.add(createActionButton("Upload All to Hugging Face", AllIcons.Actions.Upload,
                    e -> runTraceAction(() -> TraceExportActions.upload(project, tableModel.getConversations()))));
        }
        add(bottom, BorderLayout.SOUTH);

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
        try {
            tracedConversationIds = storageService.getConversationIdsWithTraces();
        } catch (RuntimeException e) {
            tracedConversationIds = Set.of(); // The menu then just offers no run view.
        }
        tableModel.setConversations(conversations);
        tableModel.fireTableDataChanged();
        // After a delete the next conversation slides under the pointer, which hasn't moved.
        refreshHoverFromPointer();
    }

    /** Recomputes the hovered cell from where the pointer is now, without waiting for it to move. */
    private void refreshHoverFromPointer() {
        Point pointer = table.getMousePosition();
        // Reset first: setHovered skips the cursor update and repaint when the cell is unchanged.
        hoveredRow = Integer.MIN_VALUE;
        if (pointer == null) {
            setHovered(-1, -1);
        } else {
            setHovered(table.rowAtPoint(pointer), modelColumnAt(pointer));
        }
    }

    private int modelColumnAt(Point point) {
        int viewColumn = table.columnAtPoint(point);
        return viewColumn < 0 ? -1 : table.convertColumnIndexToModel(viewColumn);
    }

    private void setHovered(int row, int column) {
        if (row != hoveredRow || column != hoveredColumn) {
            hoveredRow = row;
            hoveredColumn = column;
            boolean overAction = row >= 0 && row < tableModel.getRowCount() && isActionShown(column, row);
            table.setCursor(overAction ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            table.repaint();
        }
    }

    /** Whether {@code row} shows its action icons: it is under the mouse pointer or selected. */
    private boolean isRowActive(int row) {
        return row == hoveredRow || table.isRowSelected(row);
    }

    /** Whether the action icon of model {@code column} is shown, and clickable, on {@code row}. */
    private boolean isActionShown(int column, int row) {
        boolean hasTrace = tracedConversationIds.contains(tableModel.getConversationAt(row).getId());
        return isActionShown(column, isRowActive(row), traceActions, hasTrace);
    }

    /**
     * Which row actions show (TASK-283). Only the hovered or selected row shows any; delete
     * always applies, export and upload need trace recording on, view needs a recorded trace too.
     */
    static boolean isActionShown(int column, boolean rowActive, boolean traceActions, boolean hasTrace) {
        if (!rowActive) {
            return false;
        }
        return switch (column) {
            case COL_DELETE -> true;
            case COL_EXPORT, COL_UPLOAD -> traceActions;
            case COL_VIEW_RUN -> traceActions && hasTrace;
            default -> false;
        };
    }

    /** Runs the action of an icon column on {@code row} (TASK-283). */
    private void runRowAction(int column, int row) {
        Conversation conversation = tableModel.getConversationAt(row);
        switch (column) {
            case COL_DELETE -> deleteWithUndo(conversation);
            case COL_VIEW_RUN -> runTraceAction(() -> AgentRunViewer.open(project, conversation));
            case COL_EXPORT -> runTraceAction(() -> TraceExportActions.export(project, List.of(conversation)));
            case COL_UPLOAD -> runTraceAction(() -> TraceExportActions.upload(project, List.of(conversation)));
            default -> { }
        }
    }

    private static void configureIconColumn(TableColumn column, boolean visible, ActionIconRenderer renderer) {
        int width = visible ? JBUI.scale(ICON_COLUMN_WIDTH + renderer.trailingGap) : 0;
        column.setMinWidth(width);
        column.setMaxWidth(width);
        column.setPreferredWidth(width);
        column.setResizable(false);
        column.setCellRenderer(renderer);
    }

    /** Closes the history popup first: the actions open dialogs or editor tabs that would otherwise hide behind it. */
    private void runTraceAction(Runnable action) {
        if (activePopup != null && !activePopup.isDisposed()) {
            activePopup.cancel();
        }
        ApplicationManager.getApplication().invokeLater(action);
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
        private final String[] columnNames = {"Title", "Time", "Run", "Export", "Upload", "Delete"};

        /** Snapshot of the listed conversations (task-280: "Export/Upload All"). */
        public List<Conversation> getConversations() {
            return List.copyOf(conversations);
        }

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
            return switch (column) {
                case COL_TITLE -> conversation.getTitle();
                case COL_TIME -> formatTimestamp(conversation.getTimestamp());
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Row actions are icons handled by the table's click listener
        }

        private static @NotNull String formatTimestamp(String timestamp) {
            try {
                return ConversationHistoryPanel.formatTimeSince(LocalDateTime.parse(timestamp), LocalDateTime.now());
            } catch (Exception e) {
                return "";
            }
        }
    }

    /** Compact relative time for the history list ("30m ago"), short enough not to truncate. */
    static @NotNull String formatTimeSince(@NotNull LocalDateTime time, @NotNull LocalDateTime now) {
        Duration duration = Duration.between(time, now);
        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + "h ago";
        }
        return duration.toDays() + "d ago";
    }

    /**
     * A row action icon (TASK-283): blank unless the action is shown on the row, with a
     * rounded highlight behind the icon under the mouse pointer.
     */
    private class ActionIconRenderer extends JLabel implements TableCellRenderer {
        private final transient Icon icon;
        private final String tooltip;
        /** Unscaled blank space right of the icon. */
        private final int trailingGap;
        private boolean iconHovered;

        ActionIconRenderer(Icon icon, String tooltip, int trailingGap) {
            this.icon = icon;
            this.tooltip = tooltip;
            this.trailingGap = trailingGap;
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(JBUI.Borders.emptyRight(trailingGap));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            int modelColumn = table.convertColumnIndexToModel(column);
            boolean shown = row < tableModel.getRowCount() && isActionShown(modelColumn, row);
            setIcon(shown ? icon : null);
            setToolTipText(shown ? tooltip : null);
            iconHovered = shown && row == hoveredRow && modelColumn == hoveredColumn;
            applyHoverBackground(this, table, isSelected, row, hoveredRow);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Paint the row background ourselves, then the icon highlight, then the icon on top.
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (iconHovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(JBUI.CurrentTheme.ActionButton.hoverBackground());
                    int size = JBUI.scale(ICON_HOVER_SIZE);
                    int arc = JBUI.scale(6);
                    int iconAreaWidth = getWidth() - JBUI.scale(trailingGap);
                    g2.fillRoundRect((iconAreaWidth - size) / 2, (getHeight() - size) / 2, size, size, arc, arc);
                } finally {
                    g2.dispose();
                }
            }
            boolean opaque = isOpaque();
            setOpaque(false);
            try {
                super.paintComponent(g);
            } finally {
                setOpaque(opaque);
            }
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
