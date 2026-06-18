package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.registry.MCPRegistryResponse;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerEntry;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerInfo;
import com.devoxx.genie.service.mcp.MCPRegistryService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MCPMarketplaceDialog extends DialogWrapper {

    private static final String ALL_FILTER = "All";
    private static final String LOCAL_FILTER = "Local";
    private static final String REMOTE_FILTER = "Remote";

    /** Number of servers requested per registry page. */
    private static final int PAGE_SIZE = 100;
    /** Debounce delay (ms) before a search keystroke triggers a registry request. */
    private static final int SEARCH_DEBOUNCE_MS = 350;

    private final SearchTextField searchField = new SearchTextField();
    private final JComboBox<String> locationFilter = new JComboBox<>(new String[]{ALL_FILTER, LOCAL_FILTER, REMOTE_FILTER});
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{ALL_FILTER});
    private final ServerTableModel tableModel = new ServerTableModel();
    private final JBTable serverTable = new JBTable(tableModel);
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton loadMoreButton = new JButton("Load More");
    private final JLabel statusLabel = new JLabel("Loading...");

    @Getter
    private MCPServer selectedMcpServer;

    /** Accumulated paging state for the current search query (loaded servers + cursor). */
    private final PagingState pagingState = new PagingState();
    /** Monotonic counter used to discard responses from superseded (stale) requests. */
    private final AtomicInteger requestGeneration = new AtomicInteger();
    private final Timer searchDebounceTimer = new Timer(SEARCH_DEBOUNCE_MS, e -> triggerSearch());
    private boolean populatingFilters;
    private boolean disposed;

    public MCPMarketplaceDialog() {
        super(true);
        setTitle("MCP Server Marketplace");
        setOKButtonText("Add Server");
        setSize(900, 600);

        init();

        searchDebounceTimer.setRepeats(false);

        // Initial load: first page only, no query.
        pagingState.setQuery("");
        loadPage(false);

        // Typing in the search field triggers a debounced server-side search.
        searchField.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(javax.swing.event.@NotNull DocumentEvent e) {
                searchDebounceTimer.restart();
            }
        });
        // Location/Type are client-side filters over the pages loaded so far.
        locationFilter.addActionListener(e -> applyFilters());
        typeFilter.addActionListener(e -> applyFilters());
    }

    @Override
    protected void dispose() {
        disposed = true;
        searchDebounceTimer.stop();
        super.dispose();
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

        // Bottom panel with Refresh, Load More and status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        refreshButton.addActionListener(e -> loadPage(false));
        loadMoreButton.addActionListener(e -> loadPage(true));
        loadMoreButton.setEnabled(false);
        leftButtons.add(refreshButton);
        leftButtons.add(loadMoreButton);
        bottomPanel.add(leftButtons, BorderLayout.WEST);
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

    /**
     * Trigger a fresh server-side search using the current text in the search field.
     * Invoked (debounced) when the user types.
     */
    private void triggerSearch() {
        pagingState.setQuery(searchField.getText().trim());
        loadPage(false);
    }

    /**
     * Load a single page of registry results on a background thread and update the table on the EDT.
     * <p>
     * The registry is paged rather than fully downloaded, so this never blocks on an unbounded
     * loop. A monotonically increasing generation counter ensures that if a newer request starts
     * (e.g. the user keeps typing) the response of a superseded request is discarded.
     *
     * @param append {@code true} to fetch the next page and append it; {@code false} to (re)load
     *               the first page of the current query, replacing the loaded list
     */
    private void loadPage(boolean append) {
        final int generation = requestGeneration.incrementAndGet();
        setLoading(true);
        statusLabel.setText("Loading...");

        final String query = pagingState.getQuery();
        final String effectiveQuery = (query == null || query.isBlank()) ? null : query;
        final String cursor = append ? pagingState.getNextCursor() : null;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            MCPRegistryResponse response = null;
            Exception error = null;
            try {
                response = MCPRegistryService.getInstance().searchServers(effectiveQuery, cursor, PAGE_SIZE);
            } catch (Exception e) {
                error = e;
            }
            final MCPRegistryResponse result = response;
            final Exception err = error;
            ApplicationManager.getApplication().invokeLater(
                    () -> onPageLoaded(generation, result, err, append),
                    ModalityState.any());
        });
    }

    private void onPageLoaded(int generation, @Nullable MCPRegistryResponse result,
                              @Nullable Exception error, boolean append) {
        // Discard stale responses and updates after the dialog has been closed.
        if (disposed || generation != requestGeneration.get()) {
            return;
        }
        setLoading(false);

        if (error != null) {
            log.error("Failed to fetch MCP servers from registry", error);
            statusLabel.setText("Error: " + error.getMessage());
            return;
        }

        pagingState.apply(result, append);

        populateTypeFilter();
        applyFilters();
    }

    /** Toggle button state while a registry request is in flight. */
    private void setLoading(boolean loading) {
        refreshButton.setEnabled(!loading);
        loadMoreButton.setEnabled(!loading && pagingState.hasMorePages());
    }

    private void populateTypeFilter() {
        populatingFilters = true;
        try {
            MCPRegistryService registryService = MCPRegistryService.getInstance();
            Set<String> types = new TreeSet<>();
            for (MCPRegistryServerEntry entry : pagingState.getLoadedServers()) {
                if (entry.getServer() != null) {
                    types.add(registryService.getServerType(entry.getServer()));
                }
            }
            // Preserve the current selection across rebuilds (types only ever grow as more pages load).
            String previouslySelected = (String) typeFilter.getSelectedItem();
            typeFilter.removeAllItems();
            typeFilter.addItem(ALL_FILTER);
            for (String type : types) {
                typeFilter.addItem(type);
            }
            if (previouslySelected != null) {
                typeFilter.setSelectedItem(previouslySelected);
            }
        } finally {
            populatingFilters = false;
        }
    }

    /**
     * Apply the client-side Location/Type filters over the pages loaded so far.
     * Text search is performed server-side (see {@link #loadPage}), so it is not re-applied here.
     */
    private void applyFilters() {
        if (populatingFilters) {
            return;
        }
        String selectedLocation = (String) locationFilter.getSelectedItem();
        String selectedType = (String) typeFilter.getSelectedItem();
        MCPRegistryService registryService = MCPRegistryService.getInstance();

        List<MCPRegistryServerEntry> filtered = pagingState.getLoadedServers().stream()
                .filter(entry -> SERVER_FILTER.matches(entry, "", selectedLocation, selectedType, registryService))
                .toList();

        tableModel.setServers(filtered);
        statusLabel.setText(buildStatusText(filtered.size()));
        loadMoreButton.setEnabled(refreshButton.isEnabled() && pagingState.hasMorePages());
        updateOkAction();
    }

    private String buildStatusText(int shownCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Showing ").append(shownCount).append(" of ")
                .append(pagingState.size()).append(" loaded");
        if (pagingState.hasMorePages()) {
            sb.append(" (more available)");
        }
        return sb.toString();
    }

    private static final ServerEntryFilter SERVER_FILTER = new ServerEntryFilter();

    /**
     * Holds the accumulated paging state for the currently active search query: the servers
     * loaded so far and the cursor for the next page. Extracted as a static nested class so the
     * append/replace/cursor-exhaustion logic can be unit tested without the IntelliJ platform.
     */
    static class PagingState {
        private final List<MCPRegistryServerEntry> loadedServers = new ArrayList<>();
        private String nextCursor;
        private String query = "";

        void setQuery(@Nullable String query) {
            this.query = query == null ? "" : query;
        }

        String getQuery() {
            return query;
        }

        /**
         * Merge a page of registry results into the loaded state.
         *
         * @param response the registry response (may be {@code null} on a no-op load)
         * @param append   {@code true} to append to the existing list (Load More),
         *                 {@code false} to replace it (new search / refresh)
         */
        void apply(@Nullable MCPRegistryResponse response, boolean append) {
            if (!append) {
                loadedServers.clear();
            }
            if (response != null && response.getServers() != null) {
                loadedServers.addAll(response.getServers());
            }
            nextCursor = (response != null && response.getMetadata() != null)
                    ? response.getMetadata().getNextCursor() : null;
        }

        List<MCPRegistryServerEntry> getLoadedServers() {
            return loadedServers;
        }

        @Nullable
        String getNextCursor() {
            return nextCursor;
        }

        boolean hasMorePages() {
            return nextCursor != null && !nextCursor.isBlank();
        }

        int size() {
            return loadedServers.size();
        }
    }

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
