// File: src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java
package com.devoxx.genie.ui.settings.mcp;

import com.devoxx.genie.model.mcp.GitHubRepo;
import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.service.mcp.MCPProcessManager;
import com.devoxx.genie.service.mcp.MCPInstallerService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.mcp.dialog.MCPServerDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Settings component for configuring MCP (Model Context Protocol) servers.
 * Now includes two tabs: "Installed Servers" and "Marketplace".
 */
@Slf4j
public class MCPSettingsComponent extends AbstractSettingsComponent {

    // Existing components for the "Installed Servers" tab
    private final MCPServerTableModel installedServersTableModel;
    private final JBTable installedServersTable; // Changed to JBTable
    private final JCheckBox enableMcpCheckbox;
    private final JCheckBox enableDebugLogsCheckbox;
    private final JCheckBox enableApprovalRequiredCheckbox;

    // New components for the "Marketplace" tab
    private final MarketplaceTableModel marketplaceTableModel;
    private final JBTable marketplaceTable; // Changed to JBTable
    private final JButton refreshMarketplaceButton;
    private final JTabbedPane tabbedPane; // Main tabbed pane

    private boolean isModified = false;

    // Services (injected via ApplicationManager.getService)
    private final MCPInstallerService mcpInstallerService;
    private final MCPProcessManager mcpProcessManager;

    public MCPSettingsComponent() {
        // Initialize services
        this.mcpInstallerService = ApplicationManager.getApplication().getService(MCPInstallerService.class);
        this.mcpProcessManager = ApplicationManager.getApplication().getService(MCPProcessManager.class);

        // --- Installed Servers Tab Components (Existing and Enhanced) ---
        installedServersTableModel = new MCPServerTableModel();
        installedServersTable = new JBTable(installedServersTableModel);

        // Initialize checkboxes
        enableMcpCheckbox = new JCheckBox("Enable MCP Support");
        enableMcpCheckbox.addActionListener(e -> isModified = true);

        enableDebugLogsCheckbox = new JCheckBox("Enable MCP Logging");
        enableDebugLogsCheckbox.addActionListener(e -> isModified = true);

        enableApprovalRequiredCheckbox = new JCheckBox("Enable Approval Required");
        enableApprovalRequiredCheckbox.addActionListener(e -> isModified = true);

        setupInstalledServersTable(); // Configure columns and renderers/editors

        // Toolbar for installed servers tab (Add/Edit/Remove)
        ToolbarDecorator installedToolbarDecorator = ToolbarDecorator.createDecorator(installedServersTable)
                .setAddAction(button -> addMcpServer())
                .setEditAction(button -> editMcpServer())
                .setRemoveAction(button -> removeMcpServer());

        JPanel decoratedInstalledTablePanel = installedToolbarDecorator.createPanel();

        // Info panel for installed servers tab
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("<html>Configure MCP (Model Context Protocol) servers.<br>" +
                "Each MCP requires a name, command, arguments and optional environment variables.<br>" +
                "Environment variables can be configured when adding or editing a server.</html>"), BorderLayout.CENTER);

        // Checkbox panel for installed servers tab
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkboxPanel.add(enableMcpCheckbox);
        checkboxPanel.add(enableDebugLogsCheckbox);
        checkboxPanel.add(enableApprovalRequiredCheckbox);

        // Top panel combining info and checkboxes
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoPanel, BorderLayout.NORTH);
        topPanel.add(checkboxPanel, BorderLayout.CENTER);

        // Button panel for external MCP links
        JPanel buttonPanel = getButtonPanel();

        // Build the full "Installed Servers" tab panel
        JPanel installedServersTabPanel = new JPanel(new BorderLayout());
        installedServersTabPanel.add(topPanel, BorderLayout.NORTH);
        installedServersTabPanel.add(decoratedInstalledTablePanel, BorderLayout.CENTER);
        installedServersTabPanel.add(buttonPanel, BorderLayout.SOUTH);


        // --- Marketplace Tab Components (New) ---
        marketplaceTableModel = new MarketplaceTableModel();
        marketplaceTable = new JBTable(marketplaceTableModel);
        setupMarketplaceTable(); // Configure columns and renderers/editors

        refreshMarketplaceButton = new JButton("Refresh", AllIcons.Actions.Refresh);
        refreshMarketplaceButton.addActionListener(e -> refreshMarketplace());

        JPanel marketplaceToolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        marketplaceToolbarPanel.add(refreshMarketplaceButton);

        JPanel marketplaceInfoPanel = new JPanel(new BorderLayout());
        marketplaceInfoPanel.add(new JLabel("<html>Discover and install new MCP servers from GitHub.<br>" +
                "Projects are searched using the 'mcp-server' tag and 'java' language.</html>"), BorderLayout.CENTER);

        JPanel marketplaceTopPanel = new JPanel(new BorderLayout());
        marketplaceTopPanel.add(marketplaceInfoPanel, BorderLayout.NORTH);
        marketplaceTopPanel.add(marketplaceToolbarPanel, BorderLayout.CENTER);

        // Build the full "Marketplace" tab panel
        JPanel marketplaceTabPanel = new JPanel(new BorderLayout());
        marketplaceTabPanel.add(marketplaceTopPanel, BorderLayout.NORTH);
        marketplaceTabPanel.add(new JBScrollPane(marketplaceTable), BorderLayout.CENTER);


        // --- Main Tabbed Pane ---
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Installed Servers", installedServersTabPanel);
        tabbedPane.addTab("Marketplace", marketplaceTabPanel);

        // Set the main panel (inherited from AbstractSettingsComponent) to use the tabbed pane
        panel.setLayout(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);

        // Load settings and refresh marketplace after UI is initialized
        ApplicationManager.getApplication().invokeLater(() -> {
            loadCurrentSettings(); // Load persisted MCP server data
            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
            enableMcpCheckbox.setSelected(stateService.getMcpEnabled());
            enableDebugLogsCheckbox.setSelected(stateService.getMcpDebugLogsEnabled());
            enableApprovalRequiredCheckbox.setSelected(stateService.getMcpApprovalRequired());
            isModified = false; // Initial load doesn't count as modification

            // Perform initial refresh of marketplace on startup in the background
            refreshMarketplace();
        });
    }

    /**
     * Creates a panel with external links related to MCP.
     * @return The JPanel containing the buttons.
     */
    private static @NotNull JPanel getButtonPanel() {
        JButton infoButton = new JButton("What is MCP", AllIcons.Actions.Help);
        infoButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://modelcontextprotocol.io/introduction"));
            } catch (Exception ex) {
                log.error("Error opening MCP introduction link: {}", ex.getMessage());
            }
        });

        JButton githubMCPButton = new JButton("GitHub MCP", AllIcons.Vcs.Vendors.Github);
        githubMCPButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/stephanj/GitHubMCP"));
            } catch (Exception ex) {
                log.error("Error opening GitHub MCP link: {}", ex.getMessage());
            }
        });

        JButton fileSystemMCPButton = new JButton("FileSystem MCP", AllIcons.General.OpenDisk);
        fileSystemMCPButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/stephanj/MCPJavaFileSystem"));
            } catch (Exception ex) {
                log.error("Error opening FileSystem MCP link: {}", ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(infoButton);
        buttonPanel.add(githubMCPButton);
        buttonPanel.add(fileSystemMCPButton);
        return buttonPanel;
    }

    /**
     * Configures columns, renderers, and editors for the "Installed Servers" table.
     */
    private void setupInstalledServersTable() {
        // Define column widths for better layout
        installedServersTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // Enabled
        installedServersTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Name
        installedServersTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Transport Type
        installedServersTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Connection Info
        installedServersTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Env Count
        installedServersTable.getColumnModel().getColumn(5).setPreferredWidth(180);  // Tools
        installedServersTable.getColumnModel().getColumn(6).setPreferredWidth(60);   // View (Tools Info)
        installedServersTable.getColumnModel().getColumn(7).setPreferredWidth(70);   // Status (Running/Stopped)
        installedServersTable.getColumnModel().getColumn(8).setPreferredWidth(200);  // Actions (Start/Stop/Restart)
        installedServersTable.getColumnModel().getColumn(9).setPreferredWidth(60);   // Console (View Logs)

        // Assign custom renderers and editors for button columns
        installedServersTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer()); // View Tools
        installedServersTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox(), this::showToolsInfoDialog));

        installedServersTable.getColumnModel().getColumn(8).setCellRenderer(new ActionButtonRenderer()); // Start/Stop/Restart
        installedServersTable.getColumnModel().getColumn(8).setCellEditor(new ActionButtonEditor(new JCheckBox(), this));

        installedServersTable.getColumnModel().getColumn(9).setCellRenderer(new ConsoleButtonRenderer()); // Console Logs
        installedServersTable.getColumnModel().getColumn(9).setCellEditor(new ConsoleButtonEditor(new JCheckBox(), this));

        // Center align the environment count column for numerical values
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        installedServersTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Add a listener to mark settings as modified when the 'Enabled' checkbox changes
        installedServersTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) { // 'Enabled' column
                isModified = true;
            }
        });

        // Add a listener from the process manager to update the UI when server status changes
        mcpProcessManager.addProcessStatusListener((serverName, isRunning) -> {
            // Find the corresponding row for the server and repaint it on the EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                int row = -1;
                for (int i = 0; i < installedServersTableModel.getRowCount(); i++) {
                    MCPServer server = installedServersTableModel.getMcpServerAt(i);
                    if (server != null && server.getName().equals(serverName)) {
                        row = i;
                        break;
                    }
                }
                if (row != -1) {
                    installedServersTableModel.fireTableRowsUpdated(row, row); // Only update affected row
                }
            });
        });
    }

    /**
     * Configures columns, renderers, and editors for the "Marketplace" table.
     */
    private void setupMarketplaceTable() {
        // Define column widths
        marketplaceTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Name
        marketplaceTable.getColumnModel().getColumn(1).setPreferredWidth(300); // Description
        marketplaceTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Stars
        marketplaceTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Last Updated
        marketplaceTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Install button

        // Assign button renderer and editor for the "Install" column
        marketplaceTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        marketplaceTable.getColumnModel().getColumn(4).setCellEditor(new InstallButtonEditor(new JCheckBox(), this));

        // Center align the stars column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        marketplaceTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    }

    /**
     * Loads the current MCP server settings from {@link DevoxxGenieStateService} into the table model.
     */
    private void loadCurrentSettings() {
        Map<String, MCPServer> servers = DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers();
        List<MCPServer> loadedServers = new ArrayList<>(servers.values());
        installedServersTableModel.setMcpServers(loadedServers);
        // Ensure transient runtime fields are reset on load
        loadedServers.forEach(server -> {
            server.setCurrentProcess(null);
            server.setRunning(false);
            server.setConsoleOutputBuffer(new StringBuilder());
        });
    }

    /**
     * Opens a dialog to add a new MCP server configuration manually.
     */
    private void addMcpServer() {
        MCPServerDialog dialog = new MCPServerDialog(null);
        if (dialog.showAndGet()) {
            installedServersTableModel.addMcpServer(dialog.getMcpServer());
            isModified = true;
        }
    }

    /**
     * Opens a dialog to edit an existing MCP server configuration.
     */
    private void editMcpServer() {
        int selectedRow = installedServersTable.getSelectedRow();
        if (selectedRow >= 0) {
            MCPServer selectedServer = installedServersTableModel.getMcpServerAt(selectedRow);
            MCPServerDialog dialog = new MCPServerDialog(selectedServer);
            if (dialog.showAndGet()) {
                installedServersTableModel.updateMcpServer(selectedRow, dialog.getMcpServer());
                isModified = true;
            }
        }
    }

    /**
     * Removes the selected MCP server configuration. If it's an installed server,
     * it also prompts to delete the associated files.
     */
    private void removeMcpServer() {
        int selectedRow = installedServersTable.getSelectedRow();
        if (selectedRow >= 0) {
            MCPServer serverToRemove = installedServersTableModel.getMcpServerAt(selectedRow);
            if (serverToRemove == null) return;

            // First, attempt to stop the server if it's running
            if (mcpProcessManager.isRunning(serverToRemove.getName())) {
                mcpProcessManager.stopProcess(serverToRemove.getName());
                // Give a small moment for OS to clean up process resources
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }

            int result = Messages.showYesNoDialog(
                    "Are you sure you want to remove this MCP server configuration?" +
                            (serverToRemove.getInstallationPath() != null ? "\nIts installed files will also be deleted." : ""),
                    "Confirm Removal",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                installedServersTableModel.removeMcpServer(selectedRow);
                isModified = true;

                // If it was an installed server, delete its files from disk
                if (serverToRemove.getInstallationPath() != null) {
                    Path serverDir = serverToRemove.getInstallationPath().getParent();
                    if (serverDir != null && Files.exists(serverDir)) {
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            try {
                                log.info("Deleting installed server directory: {}", serverDir);
                                Files.walk(serverDir)
                                        .sorted(java.util.Comparator.reverseOrder()) // Delete files before directories
                                        .map(Path::toFile)
                                        .forEach(java.io.File::delete);
                                log.info("Deleted installed server directory: {}", serverDir);
                            } catch (IOException e) {
                                log.error("Failed to delete installed server directory: {}", serverDir, e);
                                ApplicationManager.getApplication().invokeLater(() ->
                                        Messages.showErrorDialog("Failed to delete server files: " + e.getMessage(), "Deletion Error")
                                );
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Applies the current settings from the UI components to the {@link DevoxxGenieStateService}.
     */
    public void apply() {
        if (isModified) {
            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

            boolean oldMcpEnabled = stateService.getMcpEnabled();

            // Convert the list of servers from the table model to a map for persistence
            Map<String, MCPServer> serverMap = installedServersTableModel.getMcpServers().stream()
                    .collect(Collectors.toMap(MCPServer::getName, server -> server,
                            (existing, replacement) -> existing)); // handle duplicates by keeping existing

            stateService.getMcpSettings().getMcpServers().clear();
            stateService.getMcpSettings().getMcpServers().putAll(serverMap);

            // Save checkbox states
            stateService.setMcpEnabled(enableMcpCheckbox.isSelected());
            stateService.setMcpDebugLogsEnabled(enableDebugLogsCheckbox.isSelected());
            stateService.setMcpApprovalRequired(enableApprovalRequiredCheckbox.isSelected());

            // Refresh tool window visibility if MCP enabled state changed
            if (oldMcpEnabled != enableMcpCheckbox.isSelected()) {
                if (!enableMcpCheckbox.isSelected()) {
                    MCPService.resetNotificationFlag();
                }
            }
            MCPService.refreshToolWindowVisibility(); // Always update visibility on apply

            isModified = false;
        }
    }

    /**
     * Checks if any settings in the UI have been modified compared to the persisted state.
     * @return True if settings are modified, false otherwise.
     */
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        // Check local modified flag or if any checkbox state differs
        return isModified ||
                enableMcpCheckbox.isSelected() != stateService.getMcpEnabled() ||
                enableDebugLogsCheckbox.isSelected() != stateService.getMcpDebugLogsEnabled() ||
                enableApprovalRequiredCheckbox.isSelected() != stateService.getMcpApprovalRequired();
    }

    /**
     * Shows a dialog with detailed tools information for a given MCP server.
     *
     * @param server The {@link MCPServer} to show tools for.
     */
    private void showToolsInfoDialog(MCPServer server) {
        if (server == null) {
            return;
        }

        DialogWrapper dialog = new DialogWrapper(true) {
            {
                init();
                setTitle("MCP Tools Information - " + server.getName());
            }

            @Override
            protected JComponent createCenterPanel() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBorder(JBUI.Borders.empty(10)); // Use JBUI for IntelliJ-consistent margins

                if (server.getAvailableTools() == null || server.getAvailableTools().isEmpty()) {
                    JLabel noToolsLabel = new JLabel("No tools available for this MCP server.");
                    noToolsLabel.setBorder(JBUI.Borders.empty(20));
                    panel.add(noToolsLabel, BorderLayout.CENTER);
                } else {
                    String[] columnNames = {"Tool Name", "Description"};
                    Object[][] data = new Object[server.getAvailableTools().size()][2];

                    for (int i = 0; i < server.getAvailableTools().size(); i++) {
                        String toolName = server.getAvailableTools().get(i);
                        String description = server.getToolDescriptions() != null ?
                                server.getToolDescriptions().getOrDefault(toolName, "") : "";
                        data[i][0] = toolName;
                        data[i][1] = description;
                    }

                    JTable toolsTable = new JTable(data, columnNames); // Use JBTable
                    toolsTable.setRowHeight(24);
                    toolsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
                    toolsTable.getColumnModel().getColumn(1).setPreferredWidth(350);
                    toolsTable.getColumnModel().getColumn(1).setCellRenderer(new MultiLineTableCellRenderer()); // Enable text wrapping

                    JScrollPane scrollPane = new JBScrollPane(toolsTable);
                    scrollPane.setPreferredSize(new Dimension(600, 400));

                    JLabel headerLabel = new JLabel("Available Tools: " + server.getAvailableTools().size());
                    headerLabel.setBorder(JBUI.Borders.emptyBottom(10));

                    panel.add(headerLabel, BorderLayout.NORTH);
                    panel.add(scrollPane, BorderLayout.CENTER);
                }
                return panel;
            }

            @Override
            protected Action @NotNull [] createActions() {
                return new Action[]{getOKAction()}; // Only an OK button
            }
        };
        dialog.show();
    }

    /**
     * Initiates the start of an MCP server process.
     * @param server The {@link MCPServer} to start.
     */
    void startMcpServer(MCPServer server) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                mcpProcessManager.startProcess(server);
            } catch (IOException e) {
                log.error("Failed to start MCP server {}: {}", server.getName(), e.getMessage(), e);
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog("Failed to start server: " + e.getMessage(), "Start Error")
                );
            }
        });
    }

    /**
     * Initiates the stop of an MCP server process.
     * @param server The {@link MCPServer} to stop.
     */
    void stopMcpServer(MCPServer server) {
        mcpProcessManager.stopProcess(server.getName());
    }

    /**
     * Initiates the restart of an MCP server process.
     * @param server The {@link MCPServer} to restart.
     */
    void restartMcpServer(MCPServer server) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Stop first, then start after a small delay
            mcpProcessManager.stopProcess(server.getName());
            try {
                Thread.sleep(500); // Give process a moment to terminate
                mcpProcessManager.startProcess(server);
            } catch (IOException | InterruptedException e) {
                log.error("Failed to restart MCP server {}: {}", server.getName(), e.getMessage(), e);
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog("Failed to restart server: " + e.getMessage(), "Restart Error")
                );
            }
        });
    }

    /**
     * Shows the console output for an MCP server, preferably by opening its log file in the IDE.
     * @param server The {@link MCPServer} whose console output to show.
     */
    void showConsoleOutput(MCPServer server) {
        if (server == null) {
            Messages.showInfoMessage("No server selected.", "Console Output");
            return;
        }

        // Determine the log file path based on the installation directory
        Path logFilePath = null;
        if (server.getInstallationPath() != null) {
            Path serverDir = server.getInstallationPath().getParent();
            if (serverDir != null) {
                logFilePath = serverDir.resolve("console.log");
            }
        }

        if (logFilePath != null && Files.exists(logFilePath)) {
            try {
                // Find the VirtualFile for the log and open it in the IDE's editor
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(logFilePath.toFile(), true);
                if (virtualFile != null) {
                    Project project = ProjectManager.getInstance().getDefaultProject(); // Use default project
                    FileEditorManager.getInstance(project).openFile(virtualFile, true);
                    log.info("Opened log file for {} at: {}", server.getName(), logFilePath);
                } else {
                    Messages.showInfoMessage("Could not find log file as VirtualFile: " + logFilePath, "Console Output");
                }
            } catch (Exception e) {
                log.error("Failed to open log file in IDE: {}", logFilePath, e);
                Messages.showErrorDialog("Failed to open log file: " + e.getMessage(), "Console Output Error");
            }
        } else {
            // Fallback: If no log file or path not found, show the in-memory buffer content in a dialog
            JTextArea consoleArea = new JTextArea(server.getConsoleOutputBuffer().toString());
            consoleArea.setEditable(false);
            consoleArea.setLineWrap(true);
            consoleArea.setWrapStyleWord(true);
            JBScrollPane scrollPane = new JBScrollPane(consoleArea);
            scrollPane.setPreferredSize(new Dimension(800, 600));

            DialogWrapper consoleDialog = new DialogWrapper(true) {
                {
                    init();
                    setTitle("Console Output for " + server.getName());
                }

                @Override
                protected JComponent createCenterPanel() {
                    return scrollPane;
                }

                @Override
                protected Action @NotNull [] createActions() {
                    return new Action[]{getOKAction()};
                }
            };
            consoleDialog.show();
            log.warn("Log file not found or not applicable for {}. Showing in-memory buffer.", server.getName());
        }
    }

    /**
     * Refreshes the marketplace by fetching the latest MCP server list from GitHub.
     * This operation runs in a background task with a progress indicator.
     */
    private void refreshMarketplace() {
        log.info("Refreshing MCP Marketplace from GitHub...");
        ProgressManager.getInstance().run(new Task.Backgroundable(ProjectManager.getInstance().getDefaultProject(), "Refreshing MCP Marketplace", true) {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                try {
                    // Call the service to search GitHub, passing the indicator for progress updates
                    List<GitHubRepo> repos = mcpInstallerService.searchMcpServersOnGitHub(indicator);
                    // Update the table model on the EDT
                    ApplicationManager.getApplication().invokeLater(() -> {
                        marketplaceTableModel.setGitHubRepos(repos);
                        log.info("Marketplace refreshed. Found {} MCP servers.", repos.size());
                    });
                } catch (Exception e) {
                    log.error("Failed to refresh marketplace: {}", e.getMessage(), e);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog("Failed to refresh marketplace: " + e.getMessage(), "Marketplace Error")
                    );
                }
            }
        });
    }

    /**
     * Installs a selected GitHub repository as an MCP server.
     * This involves cloning, building, and configuring the new server.
     * A warning is shown if the repository has less than 100 stars.
     * This operation runs in a background task with a progress indicator.
     * @param repo The {@link GitHubRepo} to install.
     */
    void installMcpServer(GitHubRepo repo) {
        log.info("Attempting to install MCP server from GitHub repo: {}", repo.getFullName());

        // Step 1: Check star count for a warning to the user.
        if (repo.getStars() < 100) {
            int result = Messages.showYesNoDialog(
                    "<html>The repository '" + repo.getFullName() + "' has only " + repo.getStars() + " stars.<br>" +
                            "It's recommended to review the code before installing software from less popular sources.<br>" +
                            "Do you wish to proceed with the installation?</html>",
                    "Low Star Count Warning",
                    Messages.getQuestionIcon()
            );
            if (result != Messages.YES) {
                log.info("Installation aborted by user due to low star count.");
                return;
            }
        }

        // Step 2: Perform installation in a background task with progress updates.
        ProgressManager.getInstance().run(new Task.Backgroundable(ProjectManager.getInstance().getDefaultProject(), "Installing MCP Server", true) {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                try {
                    // Call the service to perform the installation steps
                    MCPServer installedServer = mcpInstallerService.installMcpServer(repo, indicator);

                    // Update UI on the EDT after successful installation
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (installedServer != null) {
                            installedServersTableModel.addMcpServer(installedServer); // Add to installed servers table
                            isModified = true; // Mark settings as modified
                            Messages.showInfoMessage("Successfully installed MCP server: " + installedServer.getName(), "Installation Complete");
                            tabbedPane.setSelectedIndex(0); // Switch to "Installed Servers" tab
                            // Refresh marketplace table to update the "Install" button state for this repo
                            marketplaceTableModel.fireTableDataChanged();
                        } else {
                            Messages.showErrorDialog("Installation failed for unknown reason. Check logs.", "Installation Error");
                        }
                    });

                } catch (Exception e) {
                    log.error("Failed to install MCP server {}: {}", repo.getFullName(), e.getMessage(), e);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog("Failed to install server: " + e.getMessage(), "Installation Error")
                    );
                }
            }
        });
    }

    /**
     * Resets the UI components to match the currently persisted settings.
     */
    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        loadCurrentSettings(); // Reload server list
        enableMcpCheckbox.setSelected(stateService.getMcpEnabled());
        enableDebugLogsCheckbox.setSelected(stateService.getMcpDebugLogsEnabled());
        enableApprovalRequiredCheckbox.setSelected(stateService.getMcpApprovalRequired());
        isModified = false;
        // Re-fetch marketplace to update install button states
        refreshMarketplace();
    }

    /**
     * A table cell renderer that supports multi-line text for descriptions.
     */
    private static class MultiLineTableCellRenderer extends JTextArea implements TableCellRenderer {
        public MultiLineTableCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(JBUI.Borders.empty(2, 5)); // Add padding for better readability
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            // Dynamically adjust row height based on content
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
            int preferredHeight = getPreferredSize().height;
            if (preferredHeight > table.getRowHeight(row)) {
                table.setRowHeight(row, preferredHeight);
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    /**
     * Base class for a table cell renderer that displays a JButton.
     */
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFocusPainted(false); // Remove focus border
            setRolloverEnabled(false); // No rollover effect by default
            setMargin(JBUI.emptyInsets()); // Remove default button margins
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            // Set background/foreground based on selection
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    /**
     * Renderer for the Start/Stop/Restart action buttons column in the Installed Servers tab.
     */
    private static class ActionButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton startButton;
        private final JButton stopButton;
        private final JButton restartButton;

        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0)); // Small horizontal gap between buttons
            setOpaque(true);

            startButton = new JButton("Start", AllIcons.Actions.Execute);
            stopButton = new JButton("Stop", AllIcons.Actions.Suspend);
            restartButton = new JButton("Restart", AllIcons.Actions.Restart);

            // Set small margins for compact buttons
            startButton.setMargin(JBUI.emptyInsets());
            stopButton.setMargin(JBUI.emptyInsets());
            restartButton.setMargin(JBUI.emptyInsets());

            add(startButton);
            add(stopButton);
            add(restartButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // Get the current server to determine button enable/disable state
            MCPServer server = ((MCPServerTableModel) table.getModel()).getMcpServerAt(row);
            if (server != null) {
                boolean isRunning = ApplicationManager.getApplication().getService(MCPProcessManager.class).isRunning(server.getName());
                startButton.setEnabled(!isRunning);
                stopButton.setEnabled(isRunning);
                restartButton.setEnabled(isRunning); // Can only restart if already running
            } else {
                startButton.setEnabled(false);
                stopButton.setEnabled(false);
                restartButton.setEnabled(false);
            }

            // Set background based on selection
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    /**
     * Editor for the Start/Stop/Restart action buttons column.
     * Handles the actual logic when buttons are clicked.
     */
    private class ActionButtonEditor extends DefaultCellEditor {
        private final JPanel panel; // Panel containing the buttons
        private final JButton startButton;
        private final JButton stopButton;
        private final JButton restartButton;
        private MCPServer currentServer; // The server associated with the current row
        private final MCPSettingsComponent parentComponent; // Reference to the parent component for calling methods

        public ActionButtonEditor(JCheckBox checkBox, MCPSettingsComponent parentComponent) {
            super(checkBox);
            this.parentComponent = parentComponent;
            setClickCountToStart(1); // One click activates the editor and triggers the button action

            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            panel.setOpaque(true);

            startButton = new JButton("Start", AllIcons.Actions.Execute);
            stopButton = new JButton("Stop", AllIcons.Actions.Suspend);
            restartButton = new JButton("Restart", AllIcons.Actions.Restart);

            startButton.setMargin(JBUI.emptyInsets());
            stopButton.setMargin(JBUI.emptyInsets());
            restartButton.setMargin(JBUI.emptyInsets());

            panel.add(startButton);
            panel.add(stopButton);
            panel.add(restartButton);

            // Add action listeners to each button
            startButton.addActionListener(e -> {
                if (currentServer != null) {
                    parentComponent.startMcpServer(currentServer);
                }
                fireEditingStopped(); // Stop editing after action
            });
            stopButton.addActionListener(e -> {
                if (currentServer != null) {
                    parentComponent.stopMcpServer(currentServer);
                }
                fireEditingStopped();
            });
            restartButton.addActionListener(e -> {
                if (currentServer != null) {
                    parentComponent.restartMcpServer(currentServer);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentServer = ((MCPServerTableModel) table.getModel()).getMcpServerAt(row);
            // Update button states based on the server's running status
            if (currentServer != null) {
                boolean isRunning = ApplicationManager.getApplication().getService(MCPProcessManager.class).isRunning(currentServer.getName());
                startButton.setEnabled(!isRunning);
                stopButton.setEnabled(isRunning);
                restartButton.setEnabled(isRunning);
            } else {
                startButton.setEnabled(false);
                stopButton.setEnabled(false);
                restartButton.setEnabled(false);
            }

            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null; // No value to return, action is performed directly
        }

        @Override
        public boolean stopCellEditing() {
            return super.stopCellEditing();
        }
    }

    /**
     * Renderer for the "Console" (View Logs) button column.
     */
    private static class ConsoleButtonRenderer extends JButton implements TableCellRenderer {
        public ConsoleButtonRenderer() {
            setOpaque(true);
            setIcon(PlatformIcons.EXPORT_ICON); // Use a standard console icon
            setText("Logs");
            setMargin(JBUI.emptyInsets());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            MCPServer server = ((MCPServerTableModel) table.getModel()).getMcpServerAt(row);
            // Enable only if the server is an "installed" server (has an installation path)
            setEnabled(server != null && server.getInstallationPath() != null);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    /**
     * Editor for the "Console" (View Logs) button column.
     */
    private class ConsoleButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private MCPServer currentServer;
        private final MCPSettingsComponent parentComponent;

        public ConsoleButtonEditor(JCheckBox checkBox, MCPSettingsComponent parentComponent) {
            super(checkBox);
            this.parentComponent = parentComponent;
            setClickCountToStart(1);

            button = new JButton("Logs", PlatformIcons.EXPORT_ICON);
            button.setOpaque(true);
            button.setMargin(JBUI.emptyInsets());
            button.addActionListener(e -> {
                if (currentServer != null) {
                    parentComponent.showConsoleOutput(currentServer);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentServer = ((MCPServerTableModel) table.getModel()).getMcpServerAt(row);
            button.setEnabled(currentServer != null && currentServer.getInstallationPath() != null);
            if (isSelected) {
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setBackground(table.getBackground());
            }
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }

    /**
     * Generic table cell editor for a button that performs an action on an MCPServer.
     * Used for the "View" (Tools Info) button.
     */
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private int currentRow;
        private final java.util.function.Consumer<MCPServer> action; // Functional interface for the action

        public ButtonEditor(JCheckBox checkBox, java.util.function.Consumer<MCPServer> action) {
            super(checkBox);
            this.action = action;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped()); // Stop editing when button is pressed
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            currentRow = row;
            // Set background based on selection
            if (isSelected) {
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setBackground(table.getBackground());
            }
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            // Perform the action here
            MCPServer server = installedServersTableModel.getMcpServerAt(currentRow);
            if (server != null) {
                action.accept(server);
            }
            return label; // Return the label, though it's often ignored for button editors
        }

        @Override
        public boolean stopCellEditing() {
            return super.stopCellEditing();
        }
    }

    /**
     * Table cell editor for the "Install" button in the Marketplace tab.
     */
    private class InstallButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private GitHubRepo currentRepo; // The GitHub repository associated with the current row
        private final MCPSettingsComponent parentComponent;

        public InstallButtonEditor(JCheckBox checkBox, MCPSettingsComponent parentComponent) {
            super(checkBox);
            this.parentComponent = parentComponent;
            button = new JButton("Install", AllIcons.Actions.Download); // Button with a download icon
            button.setOpaque(true);
            button.setMargin(JBUI.emptyInsets());
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRepo = marketplaceTableModel.getGitHubRepoAt(row);
            button.setText("Install"); // Always display "Install"

            // Disable the button if the server is already installed
            boolean isAlreadyInstalled = parentComponent.installedServersTableModel.getMcpServers().stream()
                    .anyMatch(s -> s.getGitHubUrl() != null && s.getGitHubUrl().equals(currentRepo.getHtmlUrl()));
            button.setEnabled(!isAlreadyInstalled);

            if (isSelected) {
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setBackground(table.getBackground());
            }
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (currentRepo != null) {
                parentComponent.installMcpServer(currentRepo); // Trigger the installation process
            }
            return "Install";
        }

        @Override
        public boolean stopCellEditing() {
            return super.stopCellEditing();
        }
    }

    /**
     * Table model for displaying installed MCP servers in the "Installed Servers" tab.
     * Includes new columns for status, actions, and console.
     */
    private class MCPServerTableModel extends AbstractTableModel {
        // Updated column names to include new functionality
        private final String[] COLUMN_NAMES = {"Enabled", "Name", "Transport Type", "Connection Info", "Env Variables", "Tools", "", "Status", "Actions", "Console"};
        @Getter
        private List<MCPServer> mcpServers = new ArrayList<>();

        public void setMcpServers(List<MCPServer> mcpServers) {
            this.mcpServers = mcpServers;
            fireTableDataChanged(); // Notify table that data has changed
        }

        public @Nullable MCPServer getMcpServerAt(int row) {
            if (row >= 0 && row < mcpServers.size()) {
                return mcpServers.get(row);
            }
            return null;
        }

        public void addMcpServer(MCPServer server) {
            mcpServers.add(server);
            fireTableRowsInserted(mcpServers.size() - 1, mcpServers.size() - 1);
        }

        public void updateMcpServer(int row, MCPServer server) {
            if (row >= 0 && row < mcpServers.size()) {
                mcpServers.set(row, server);
                fireTableRowsUpdated(row, row);
            }
        }

        public void removeMcpServer(int row) {
            if (row >= 0 && row < mcpServers.size()) {
                mcpServers.remove(row);
                fireTableRowsDeleted(row, row);
                // Also update tool window visibility if MCP is enabled and servers are removed
                if (DevoxxGenieStateService.getInstance().getMcpEnabled()) {
                    MCPService.refreshToolWindowVisibility();
                }
            }
        }

        @Override
        public int getRowCount() {
            return mcpServers.size();
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
        public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
            MCPServer server = mcpServers.get(rowIndex);
            switch (columnIndex) {
                case 0: return server.isEnabled();
                case 1: return server.getName();
                case 2: return server.getTransportType();
                case 3: return getConnectionInfo(server);
                case 4: return server.getEnv() != null ? server.getEnv().size() : 0;
                case 5: return getToolsSummary(server);
                case 6: return "View"; // Button text for tools info
                case 7: return mcpProcessManager.isRunning(server.getName()) ? "Running" : "Stopped"; // Dynamic status
                case 8: return "Actions"; // Placeholder for Start/Stop/Restart buttons
                case 9: return "Logs";    // Placeholder for Console Output button
                default: return null;
            }
        }

        private String getConnectionInfo(@NotNull MCPServer server) {
            if (server.getTransportType() == MCPServer.TransportType.HTTP_SSE) {
                return server.getSseUrl();
            } else if (server.getTransportType() == MCPServer.TransportType.STDIO) {
                // If an installed server, show its local JAR path
                if (server.getInstallationPath() != null) {
                    return server.getInstallationPath().toString();
                }
                return server.getCommand(); // Otherwise, show the command directly
            }
            return "";
        }

        private @NotNull String getToolsSummary(@NotNull MCPServer server) {
            if (server.getAvailableTools() == null || server.getAvailableTools().isEmpty()) {
                return "No tools info";
            }
            return "Available tools: " + server.getAvailableTools().size();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0: return Boolean.class;
                case 1, 3, 5, 6, 7, 8, 9: return String.class; // Buttons and status displayed as Strings
                case 2: return MCPServer.TransportType.class;
                case 4: return Integer.class;
                default: return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Define which columns are editable (Enabled checkbox and all button columns)
            return columnIndex == 0 || columnIndex == 6 || columnIndex == 8 || columnIndex == 9;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean enabled) {
                MCPServer server = mcpServers.get(rowIndex);
                server.setEnabled(enabled);
                fireTableCellUpdated(rowIndex, columnIndex); // Notify table of specific cell change

                // Refresh tool window visibility if MCP is enabled and server enable state changes
                if (DevoxxGenieStateService.getInstance().getMcpEnabled()) {
                    MCPService.refreshToolWindowVisibility();
                }
            }
            // For button columns, the editor handles the action, so no value needs to be set here.
        }
    }

    /**
     * Table model for displaying GitHub repositories in the "Marketplace" tab.
     */
    private class MarketplaceTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Name", "Description", "Stars", "Last Updated", "Action"};
        @Getter
        private List<GitHubRepo> gitHubRepos = new ArrayList<>();

        public void setGitHubRepos(List<GitHubRepo> gitHubRepos) {
            this.gitHubRepos = gitHubRepos;
            fireTableDataChanged(); // Notify table that data has changed
        }

        public @Nullable GitHubRepo getGitHubRepoAt(int row) {
            if (row >= 0 && row < gitHubRepos.size()) {
                return gitHubRepos.get(row);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return gitHubRepos.size();
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
            GitHubRepo repo = gitHubRepos.get(rowIndex);
            switch (columnIndex) {
                case 0: return repo.getFullName();
                case 1: return repo.getDescription();
                case 2: return repo.getStars();
                case 3: return repo.getUpdatedAt();
                case 4: return "Install"; // Button text
                default: return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0, 1, 3, 4: return String.class;
                case 2: return Integer.class;
                default: return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 4; // Only the "Action" (Install button) column is editable
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            // For button columns, the editor handles the action, no value needs to be set here.
        }
    }
}