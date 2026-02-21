package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
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
import java.util.*;
import java.util.List;

@Slf4j
public class MCPMarketplaceDialog extends DialogWrapper {

    private static final String ALL_FILTER = "All";
    private static final String LOCAL_FILTER = "Local";
    private static final String REMOTE_FILTER = "Remote";

    private final SearchTextField searchField = new SearchTextField();
    private final JComboBox<String> locationFilter = new JComboBox<>(new String[]{ALL_FILTER, LOCAL_FILTER, REMOTE_FILTER});
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{ALL_FILTER});
    private final ServerTableModel tableModel = new ServerTableModel();
    private final JBTable serverTable = new JBTable(tableModel);
    private final JButton refreshButton = new JButton("Refresh");
    private final JLabel statusLabel = new JLabel("Loading...");

    @Getter
    private MCPServer selectedMcpServer;

    private List<MCPRegistryServerEntry> allServers = new ArrayList<>();
    private boolean populatingFilters;

    public MCPMarketplaceDialog() {
        super(true);
        setTitle("MCP Server Marketplace");
        setOKButtonText("Add Server");
        setSize(900, 600);

        init();

        // Initial load (uses cache if available)
        loadAllServers(false);

        // Search field and filter combos all trigger the same local filter
        searchField.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(javax.swing.event.@NotNull DocumentEvent e) {
                applyFilters();
            }
        });
        locationFilter.addActionListener(e -> applyFilters());
        typeFilter.addActionListener(e -> applyFilters());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setPreferredSize(new Dimension(880, 520));

        // Top panel with search bar and filters
        JPanel topPanel = new JPanel(new BorderLayout(0, 4));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchPanel, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.add(new JLabel("Location:"));
        filterPanel.add(locationFilter);
        filterPanel.add(new JLabel("Type:"));
        filterPanel.add(typeFilter);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

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

        // Bottom panel with Refresh and status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        refreshButton.addActionListener(e -> loadAllServers(true));
        bottomPanel.add(refreshButton, BorderLayout.WEST);
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

    @SuppressWarnings("unchecked")
    private void loadAllServers(boolean forceRefresh) {
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading...");

        final List<MCPRegistryServerEntry>[] result = new List[]{null};
        final Exception[] error = {null};
        boolean cancelled = false;

        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setIndeterminate(true);
                indicator.setText("Fetching servers from MCP registry...");
                try {
                    result[0] = MCPRegistryService.getInstance().fetchAllServers(forceRefresh);
                } catch (ProcessCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    error[0] = e;
                }
            }, "Loading MCP Servers", true, null);
        } catch (ProcessCanceledException e) {
            cancelled = true;
        }

        refreshButton.setEnabled(true);

        if (cancelled) {
            statusLabel.setText("Cancelled");
            return;
        }

        if (error[0] != null) {
            log.error("Failed to fetch MCP servers from registry", error[0]);
            statusLabel.setText("Error: " + error[0].getMessage());
            return;
        }

        if (result[0] == null) {
            allServers = new ArrayList<>();
        } else {
            allServers = new ArrayList<>(result[0]);
        }

        populateTypeFilter();
        applyFilters();
    }

    private void populateTypeFilter() {
        populatingFilters = true;
        try {
            MCPRegistryService registryService = MCPRegistryService.getInstance();
            Set<String> types = new TreeSet<>();
            for (MCPRegistryServerEntry entry : allServers) {
                if (entry.getServer() != null) {
                    types.add(registryService.getServerType(entry.getServer()));
                }
            }
            typeFilter.removeAllItems();
            typeFilter.addItem(ALL_FILTER);
            for (String type : types) {
                typeFilter.addItem(type);
            }
        } finally {
            populatingFilters = false;
        }
    }

    private void applyFilters() {
        if (populatingFilters) {
            return;
        }
        String query = searchField.getText().trim();
        String selectedLocation = (String) locationFilter.getSelectedItem();
        String selectedType = (String) typeFilter.getSelectedItem();
        MCPRegistryService registryService = MCPRegistryService.getInstance();

        List<MCPRegistryServerEntry> filtered = allServers.stream()
                .filter(entry -> SERVER_FILTER.matches(entry, query, selectedLocation, selectedType, registryService))
                .toList();

        tableModel.setServers(filtered);
        statusLabel.setText("Showing " + filtered.size() + " of " + allServers.size() + " servers");
        updateOkAction();
    }

    private static final ServerEntryFilter SERVER_FILTER = new ServerEntryFilter();

    /**
     * Pure filter logic for MCP registry server entries.
     * Extracted as a static nested class to allow unit testing without IntelliJ platform.
     */
    static class ServerEntryFilter {

        boolean matches(MCPRegistryServerEntry entry,
                        String query,
                        String selectedLocation,
                        String selectedType,
                        MCPRegistryService registryService) {
            MCPRegistryServerInfo info = entry.getServer();
            if (info == null) return false;
            return matchesText(info, query)
                    && matchesLocation(info, selectedLocation)
                    && matchesType(info, selectedType, registryService);
        }

        boolean matchesText(MCPRegistryServerInfo info, String query) {
            if (query.isEmpty()) return true;
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            String name = info.getName() != null ? info.getName().toLowerCase(Locale.ROOT) : "";
            String desc = info.getDescription() != null ? info.getDescription().toLowerCase(Locale.ROOT) : "";
            return name.contains(lowerQuery) || desc.contains(lowerQuery);
        }

        boolean matchesLocation(MCPRegistryServerInfo info, String selectedLocation) {
            if (selectedLocation == null || ALL_FILTER.equals(selectedLocation)) return true;
            boolean isRemote = info.getRemotes() != null && !info.getRemotes().isEmpty();
            if (REMOTE_FILTER.equals(selectedLocation)) return isRemote;
            if (LOCAL_FILTER.equals(selectedLocation)) return !isRemote;
            return true;
        }

        boolean matchesType(MCPRegistryServerInfo info, String selectedType, MCPRegistryService registryService) {
            if (selectedType == null || ALL_FILTER.equals(selectedType)) return true;
            return selectedType.equals(registryService.getServerType(info));
        }
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
