package com.devoxx.genie.ui.panel.ap;

import com.devoxx.genie.model.ap.ApAgent;
import com.devoxx.genie.service.ap.ApCliException;
import com.devoxx.genie.service.ap.ApCliService;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Read-only catalog of available Agentic Platform agents.
 *
 * <p>Each row has a <b>View</b> button that opens {@link ApAgentDetailsDialog} for that agent.
 * Single-click selects the row (notifies the parent via {@link #setOnAgentSelected}, used to
 * pre-fill the New Run tab). Double-click on the row also opens the details dialog.</p>
 */
@Slf4j
public class ApAgentsTab extends JPanel {

    private static final String VIEW_LABEL = "View";

    private final AgentTableModel tableModel = new AgentTableModel();
    private final JBTable table = new JBTable(tableModel);
    private final JBLabel statusLabel = new JBLabel(" ");

    private Consumer<ApAgent> onAgentSelected = a -> {};
    private Consumer<ApAgent> onStartNewRun = a -> {};

    public ApAgentsTab() {
        super(new BorderLayout());

        configureTable();

        JButton refreshBtn = new JButton(DevoxxGenieIconsUtil.RefreshIcon);
        refreshBtn.setToolTipText("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.add(refreshBtn);
        toolbar.add(statusLabel);

        add(toolbar, BorderLayout.NORTH);
        add(new JBScrollPane(table), BorderLayout.CENTER);

        refresh();
    }

    private void configureTable() {
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(140); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(420); // Description
        table.getColumnModel().getColumn(2).setPreferredWidth(44);  // View icon button
        table.getColumnModel().getColumn(2).setMaxWidth(48);

        ViewButtonRenderer renderer = new ViewButtonRenderer();
        ViewButtonEditor editor = new ViewButtonEditor(this::openDetails);
        table.getColumnModel().getColumn(2).setCellRenderer(renderer);
        table.getColumnModel().getColumn(2).setCellEditor(editor);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ApAgent selected = selectedAgent();
                if (selected != null) onAgentSelected.accept(selected);
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    // Ignore double-clicks on the button column — its cell editor handles that.
                    if (row >= 0 && col != 2) {
                        ApAgent agent = tableModel.getAt(table.convertRowIndexToModel(row));
                        if (agent != null) openDetails(agent);
                    }
                }
            }
        });
    }

    private @Nullable ApAgent selectedAgent() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return tableModel.getAt(table.convertRowIndexToModel(viewRow));
    }

    private void openDetails(@NotNull ApAgent agent) {
        new ApAgentDetailsDialog(agent, onStartNewRun).show();
    }

    public void setOnAgentSelected(@NotNull Consumer<ApAgent> handler) {
        this.onAgentSelected = handler;
    }

    /** Called when the user clicks "Start a new run with this agent" inside the details dialog. */
    public void setOnStartNewRun(@NotNull Consumer<ApAgent> handler) {
        this.onStartNewRun = handler;
    }

    public void refresh() {
        statusLabel.setText("Loading…");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<ApAgent> agents = ApCliService.getInstance().listAgents(50);
                ApplicationManager.getApplication().invokeLater(() -> {
                    tableModel.set(agents);
                    statusLabel.setText(agents.size() + " agent(s)");
                }, ModalityState.any());
            } catch (ApCliException e) {
                log.warn("Failed to list agents: {}", e.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        statusLabel.setText("Error: " + e.getMessage()), ModalityState.any());
            }
        });
    }

    // ===== Table model =====

    private static class AgentTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Name", "Description", ""};
        private final List<ApAgent> rows = new ArrayList<>();

        void set(@NotNull List<ApAgent> next) {
            rows.clear();
            rows.addAll(next);
            fireTableDataChanged();
        }

        @Nullable ApAgent getAt(int row) {
            return row >= 0 && row < rows.size() ? rows.get(row) : null;
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Only the View-button column is "editable" — that's how cell editors get focus.
            return columnIndex == 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ApAgent a = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> a.name() == null ? "" : a.name();
                case 1 -> a.description() == null ? "" : a.description();
                case 2 -> VIEW_LABEL;
                default -> "";
            };
        }
    }

    // ===== View button renderer + editor =====

    private static class ViewButtonRenderer extends JButton implements TableCellRenderer {
        ViewButtonRenderer() {
            setOpaque(true);
            setIcon(AllIcons.Actions.Show);
            setToolTipText(VIEW_LABEL);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                        boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }
    }

    /**
     * Editor for the View column — fires the supplied consumer with the agent for the row
     * currently being edited, then immediately stops editing. The row's {@code ApAgent} is
     * resolved through the table model rather than the editor's value, because the value is
     * the constant {@code "View"} label.
     */
    private class ViewButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton(AllIcons.Actions.Show);
        private final Consumer<ApAgent> action;
        private int editingRow = -1;

        ViewButtonEditor(@NotNull Consumer<ApAgent> action) {
            this.action = action;
            button.setToolTipText(VIEW_LABEL);
            button.addActionListener(e -> {
                ApAgent agent = editingRow >= 0
                        ? tableModel.getAt(table.convertRowIndexToModel(editingRow))
                        : null;
                fireEditingStopped();
                if (agent != null) action.accept(agent);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value, boolean isSelected,
                                                      int row, int column) {
            this.editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return VIEW_LABEL;
        }
    }
}
