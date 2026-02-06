package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.registry.MCPRegistryResponse;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerEntry;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerInfo;
import com.devoxx.genie.service.mcp.MCPRegistryService;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MCPMarketplaceDialog extends DialogWrapper {

    private static final int PAGE_SIZE = 20;

    private final SearchTextField searchField = new SearchTextField();
    private final ServerTableModel tableModel = new ServerTableModel();
    private final JBTable serverTable = new JBTable(tableModel);
    private final JButton loadMoreButton = new JButton("Load More");
    private final JLabel statusLabel = new JLabel("Loading...");

    @Getter
    private MCPServer selectedMcpServer;

    private String nextCursor;
    private Timer debounceTimer;

    public MCPMarketplaceDialog() {
        super(true);
        setTitle("MCP Server Marketplace");
        setOKButtonText("Add Server");
        setSize(900, 600);

        init();

        // Initial load
        loadServers(null, null, false);

        // Debounced search
        searchField.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(javax.swing.event.@NotNull DocumentEvent e) {
                if (debounceTimer != null) {
                    debounceTimer.stop();
                }
                debounceTimer = new Timer(300, evt -> {
                    nextCursor = null;
                    loadServers(searchField.getText().trim(), null, false);
                });
                debounceTimer.setRepeats(false);
                debounceTimer.start();
            }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setPreferredSize(new Dimension(880, 520));

        // Search bar
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // Server table
        serverTable.setRowHeight(24);
        serverTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Name
        serverTable.getColumnModel().getColumn(1).setPreferredWidth(400); // Description
        serverTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Type
        serverTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Version
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverTable.getSelectionModel().addListSelectionListener(e -> updateOkAction());

        JBScrollPane scrollPane = new JBScrollPane(serverTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with Load More and status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        loadMoreButton.addActionListener(e -> {
            if (nextCursor != null) {
                loadServers(searchField.getText().trim(), nextCursor, true);
            }
        });
        loadMoreButton.setEnabled(false);
        bottomPanel.add(loadMoreButton, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        int selectedRow = serverTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        MCPRegistryServerInfo serverInfo = tableModel.getServerInfoAt(selectedRow);
        if (serverInfo == null) {
            return;
        }

        MCPMarketplaceInstallDialog installDialog = new MCPMarketplaceInstallDialog(serverInfo);
        if (installDialog.showAndGet()) {
            selectedMcpServer = installDialog.getConfiguredServer();
            super.doOKAction();
        }
    }

    private void updateOkAction() {
        setOKActionEnabled(serverTable.getSelectedRow() >= 0);
    }

    private void loadServers(@Nullable String query, @Nullable String cursor, boolean append) {
        loadMoreButton.setEnabled(false);
        statusLabel.setText("Loading...");

        final MCPRegistryResponse[] result = {null};
        final Exception[] error = {null};
        boolean cancelled = false;

        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setIndeterminate(true);
                indicator.setText("Fetching servers from MCP registry...");
                try {
                    result[0] = MCPRegistryService.getInstance().searchServers(query, cursor, PAGE_SIZE);
                } catch (ProcessCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    error[0] = e;
                }
            }, "Loading MCP Servers", true, null);
        } catch (ProcessCanceledException e) {
            cancelled = true;
        }

        if (cancelled) {
            statusLabel.setText("Cancelled");
            loadMoreButton.setEnabled(nextCursor != null);
            return;
        }

        if (error[0] != null) {
            log.error("Failed to fetch MCP servers from registry", error[0]);
            statusLabel.setText("Error: " + error[0].getMessage());
            loadMoreButton.setEnabled(false);
            return;
        }

        if (result[0] == null || result[0].getServers() == null) {
            statusLabel.setText("No servers found");
            if (!append) {
                tableModel.setServers(new ArrayList<>());
            }
            loadMoreButton.setEnabled(false);
            return;
        }

        if (append) {
            tableModel.addServers(result[0].getServers());
        } else {
            tableModel.setServers(result[0].getServers());
        }

        // Update pagination
        if (result[0].getMetadata() != null) {
            nextCursor = result[0].getMetadata().getNextCursor();
        } else {
            nextCursor = null;
        }

        loadMoreButton.setEnabled(nextCursor != null && !nextCursor.isBlank());
        statusLabel.setText("Showing " + tableModel.getRowCount() + " servers");
        updateOkAction();
    }

    /**
     * Table model for displaying registry servers.
     */
    private static class ServerTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Description", "Type", "Version"};
        private final List<MCPRegistryServerEntry> entries = new ArrayList<>();

        public void setServers(List<MCPRegistryServerEntry> servers) {
            entries.clear();
            entries.addAll(servers);
            fireTableDataChanged();
        }

        public void addServers(List<MCPRegistryServerEntry> servers) {
            int firstRow = entries.size();
            entries.addAll(servers);
            fireTableRowsInserted(firstRow, entries.size() - 1);
        }

        public @Nullable MCPRegistryServerInfo getServerInfoAt(int row) {
            if (row >= 0 && row < entries.size()) {
                return entries.get(row).getServer();
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MCPRegistryServerInfo info = entries.get(rowIndex).getServer();
            if (info == null) return "";
            return switch (columnIndex) {
                case 0 -> info.getName();
                case 1 -> truncate(info.getDescription(), 120);
                case 2 -> MCPRegistryService.getInstance().getServerType(info);
                case 3 -> info.getVersion();
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        private static String truncate(String text, int maxLen) {
            if (text == null) return "";
            return text.length() <= maxLen ? text : text.substring(0, maxLen - 3) + "...";
        }
    }
}
