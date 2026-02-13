package com.devoxx.genie.ui.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.HammerIcon;

/**
 * Manages MCP (Message Command Processor) tools UI components and interactions.
 */
public class MCPToolsManager {

    private final Project project;
    
    @Getter
    private final JLabel mcpToolsCountLabel;

    /**
     * Constructs a new MCPToolsManager for the given project.
     *
     * @param project The IDEA project
     */
    public MCPToolsManager(Project project) {
        this.project = project;
        this.mcpToolsCountLabel = createMCPToolsCounter();
        // Now that the label is initialized, we can safely update it
        updateMCPToolsCounter();
    }

    /**
     * Creates the MCP tools counter label with its icon and listeners.
     *
     * @return The configured JLabel for MCP tools count
     */
    private @NotNull JLabel createMCPToolsCounter() {
        JLabel label = new JLabel();
        label.setIcon(HammerIcon);
        label.setToolTipText("Total MCP Tools Available");
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.CENTER);
        label.setIconTextGap(4);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Add click listener for showing the tool list popup
        label.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMCPToolsPopup();
            }
        });
        
        // Don't call updateMCPToolsCounter() here - will be called from the constructor after initialization
        return label;
    }

    /**
     * Updates the MCP Tools counter to display the total number of enabled tools provided by all activated MCP servers.
     */
    public void updateMCPToolsCounter() {
        if (!MCPService.isMCPEnabled()) {
            mcpToolsCountLabel.setVisible(false);
            return;
        }

        mcpToolsCountLabel.setVisible(true);

        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers();
        int totalToolsCount = mcpServers.values().stream()
                .filter(MCPServer::isEnabled)
                .mapToInt(server -> {
                    int total = server.getAvailableTools().size();
                    int disabled = server.getDisabledTools() != null ? server.getDisabledTools().size() : 0;
                    return total - disabled;
                })
                .sum();

        mcpToolsCountLabel.setText(String.valueOf(totalToolsCount));

        if (totalToolsCount > 0) {
            StringBuilder toolTip = new StringBuilder("<html>Total MCP Tools Enabled: " + totalToolsCount + "<br><br>");

            mcpServers.values().stream()
                    .filter(MCPServer::isEnabled)
                    .forEach(server -> {
                        if (!server.getAvailableTools().isEmpty()) {
                            int total = server.getAvailableTools().size();
                            int disabled = server.getDisabledTools() != null ? server.getDisabledTools().size() : 0;
                            int enabled = total - disabled;
                            toolTip.append("<b>").append(server.getName()).append(":</b> ")
                                  .append(enabled).append("/").append(total).append(" tools<br>");
                        }
                    });

            toolTip.append("<br>Click to enable/disable MCP servers and tools</html>");
            mcpToolsCountLabel.setToolTipText(toolTip.toString());
        }
    }

    /**
     * Shows a popup with the list of available MCPs with checkboxes to enable/disable them.
     */
    private void showMCPToolsPopup() {
        if (!MCPService.isMCPEnabled()) {
            return;
        }
        
        // Get all MCP servers
        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers();
        if (mcpServers.isEmpty()) {
            return;
        }
        
        // Create a panel with checkboxes for each MCP server
        JPanel popupPanel = new JPanel(new BorderLayout());
        JPanel mcpListPanel = new JPanel();
        mcpListPanel.setLayout(new BoxLayout(mcpListPanel, BoxLayout.Y_AXIS));
        
        // Add buttons at the top
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton showAllToolsButton = new JButton("Show All Tools");
        showAllToolsButton.addActionListener(e -> showAllMCPTools(mcpServers));
        buttonsPanel.add(showAllToolsButton);
        
        // Title
        JLabel titleLabel = new JLabel("Available MCP Servers");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        
        // Add the title and buttons panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonsPanel, BorderLayout.EAST);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mcpListPanel.add(headerPanel);
        
        // Add a checkbox for each MCP server
        List<MCPServer> serverList = new ArrayList<>(mcpServers.values());
        // Sort alphabetically by name
        serverList.sort(Comparator.comparing(MCPServer::getName));

        for (MCPServer server : serverList) {
            JPanel serverPanel = new JPanel(new BorderLayout());
            serverPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

            JCheckBox enabledCheckbox = new JCheckBox(server.getName());
            enabledCheckbox.setSelected(server.isEnabled());
            enabledCheckbox.setFont(enabledCheckbox.getFont().deriveFont(Font.BOLD));

            // Add tooltip with tool count
            int toolCount = server.getAvailableTools().size();
            int disabledCount = server.getDisabledTools() != null ? server.getDisabledTools().size() : 0;
            int enabledToolCount = toolCount - disabledCount;
            enabledCheckbox.setToolTipText(String.format("%s - %d/%d tool%s enabled",
                    server.getName(), enabledToolCount, toolCount, toolCount == 1 ? "" : "s"));

            // Add label with tool count
            JLabel toolCountLabel = new JLabel(String.format("(%d/%d tools)",
                    enabledToolCount, toolCount));
            toolCountLabel.setFont(toolCountLabel.getFont().deriveFont(Font.PLAIN));
            toolCountLabel.setForeground(JBColor.GRAY);

            serverPanel.add(enabledCheckbox, BorderLayout.WEST);
            serverPanel.add(toolCountLabel, BorderLayout.EAST);

            mcpListPanel.add(serverPanel);

            // Add per-tool checkboxes (collapsible, shown under the server)
            JPanel toolsPanel = new JPanel();
            toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.Y_AXIS));
            toolsPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 5, 5));

            java.util.Set<String> disabledTools = server.getDisabledTools() != null
                    ? server.getDisabledTools() : new java.util.HashSet<>();

            for (String toolName : server.getAvailableTools()) {
                JCheckBox toolCheckbox = new JCheckBox(toolName);
                toolCheckbox.setSelected(!disabledTools.contains(toolName));
                toolCheckbox.setFont(toolCheckbox.getFont().deriveFont(Font.PLAIN));
                toolCheckbox.setEnabled(server.isEnabled());

                String desc = server.getToolDescriptions() != null
                        ? server.getToolDescriptions().getOrDefault(toolName, "") : "";
                if (!desc.isEmpty()) {
                    toolCheckbox.setToolTipText(desc);
                }

                toolCheckbox.addActionListener(e -> {
                    if (toolCheckbox.isSelected()) {
                        server.getDisabledTools().remove(toolName);
                    } else {
                        server.getDisabledTools().add(toolName);
                    }
                    DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers()
                            .put(server.getName(), server);
                    updateMCPToolsCounter();
                });

                toolsPanel.add(toolCheckbox);
            }

            // Collapse/expand toggle
            toolsPanel.setVisible(server.isEnabled() && !server.getAvailableTools().isEmpty());
            mcpListPanel.add(toolsPanel);

            // Add action listener to auto-apply changes when server checkbox is toggled
            enabledCheckbox.addActionListener(e -> {
                boolean newState = enabledCheckbox.isSelected();
                if (server.isEnabled() != newState) {
                    server.setEnabled(newState);

                    DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers()
                            .put(server.getName(), server);

                    MCPService.refreshToolWindowVisibility();
                    updateMCPToolsCounter();

                    // Enable/disable individual tool checkboxes based on server state
                    toolsPanel.setVisible(newState && !server.getAvailableTools().isEmpty());
                    for (Component c : toolsPanel.getComponents()) {
                        if (c instanceof JCheckBox cb) {
                            cb.setEnabled(newState);
                        }
                    }
                }
            });
        }
        
        // Add some padding at the bottom
        mcpListPanel.add(Box.createVerticalStrut(10));
        
        JScrollPane scrollPane = new JBScrollPane(mcpListPanel);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        popupPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create the popup
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupPanel, null)
                .setTitle("Enable/Disable MCP Servers")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .createPopup();
        
        popup.showUnderneathOf(mcpToolsCountLabel);
    }
    
    /**
     * Record for storing tool information for display in the popup.
     */
    private record ToolInfo(String serverName, String toolName, String description) {
        @Override
        public @NotNull String toString() {
            return toolName;
        }
    }
    
    /**
     * Custom cell renderer for displaying tool information in the list.
     */
    private static class ToolInfoRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ToolInfo tool) {
                StringBuilder text = new StringBuilder("<html>");
                text.append("<b>").append(tool.toolName()).append("</b>");
                text.append(" <font color='gray'>(from ").append(tool.serverName()).append(")</font>");
                
                if (!tool.description().isEmpty()) {
                    text.append("<br><font size='2'>").append(tool.description()).append("</font>");
                }
                
                text.append("</html>");
                label.setText(text.toString());
                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
            
            return label;
        }
    }

    /**
     * Shows a popup with all available MCP tools from all servers
     * 
     * @param mcpServers The map of MCP servers
     */
    private void showAllMCPTools(@NotNull Map<String, MCPServer> mcpServers) {
        List<ToolInfo> allTools = new ArrayList<>();
        
        // Collect all tools from all servers
        mcpServers.values().stream()
                .filter(MCPServer::isEnabled)
                .forEach(server -> {
                    for (String toolName : server.getAvailableTools()) {
                        String description = server.getToolDescriptions().getOrDefault(toolName, "");
                        allTools.add(new ToolInfo(server.getName(), toolName, description));
                    }
                });
        
        if (allTools.isEmpty()) {
            NotificationUtil.sendNotification(project, "No MCP tools available");
            return;
        }
        
        // Sort tools alphabetically by name
        allTools.sort(Comparator.comparing(ToolInfo::toolName));
        
        // Create a panel with a list of tools
        JPanel popupPanel = new JPanel(new BorderLayout());
        
        // Add title
        JLabel titleLabel = new JLabel("All Available MCP Tools");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        popupPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Create the tools list
        DefaultListModel<ToolInfo> listModel = new DefaultListModel<>();
        allTools.forEach(listModel::addElement);
        
        JBList<ToolInfo> toolsList = new JBList<>(listModel);
        toolsList.setCellRenderer(new ToolInfoRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(toolsList);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        popupPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create and show the popup
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupPanel, null)
                .setTitle("Available MCP Tools (" + allTools.size() + ")")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .createPopup();
        
        popup.showCenteredInCurrentWindow(project);
    }
}
