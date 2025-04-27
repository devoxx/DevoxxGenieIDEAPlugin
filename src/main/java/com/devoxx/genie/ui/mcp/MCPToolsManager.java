package com.devoxx.genie.ui.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.HammerIcon;

/**
 * Manages MCP (Message Command Processor) tools UI components and interactions.
 */
public class MCPToolsManager {

    private final Project project;
    
    @Getter
    private final JLabel mcpToolsCountLabel;
    
    private Consumer<Boolean> visibilityChangeListener;

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
     * Sets a callback to be invoked when the visibility of MCP tools changes.
     *
     * @param listener The listener to be called with the new visibility state
     */
    public void setVisibilityChangeListener(Consumer<Boolean> listener) {
        this.visibilityChangeListener = listener;
    }

    /**
     * Creates the MCP tools counter label with its icon and listeners.
     *
     * @return The configured JLabel for MCP tools count
     */
    private JLabel createMCPToolsCounter() {
        JLabel label = new JLabel();
        label.setIcon(HammerIcon);
        label.setToolTipText("Total MCP Tools Available");
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
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
     * Updates the MCP Tools counter to display the total number of tools provided by all activated MCP servers.
     */
    public void updateMCPToolsCounter() {
        if (!MCPService.isMCPEnabled()) {
            mcpToolsCountLabel.setVisible(false);
            if (visibilityChangeListener != null) {
                visibilityChangeListener.accept(false);
            }
            return;
        }
        
        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers();
        int totalToolsCount = mcpServers.values().stream()
                .filter(MCPServer::isEnabled)
                .mapToInt(server -> server.getAvailableTools().size())
                .sum();
        
        if (totalToolsCount > 0) {
            mcpToolsCountLabel.setText(String.valueOf(totalToolsCount));
            mcpToolsCountLabel.setVisible(true);
            
            // Create a more detailed tooltip
            StringBuilder toolTip = new StringBuilder("<html>Total MCP Tools Available: " + totalToolsCount + "<br><br>");
            
            mcpServers.values().stream()
                    .filter(MCPServer::isEnabled)
                    .forEach(server -> {
                        if (!server.getAvailableTools().isEmpty()) {
                            toolTip.append("<b>").append(server.getName()).append(":</b> ")
                                  .append(server.getAvailableTools().size()).append(" tools<br>");
                        }
                    });
            
            toolTip.append("<br>Click to enable/disable MCP servers</html>");
            mcpToolsCountLabel.setToolTipText(toolTip.toString());
            
            if (visibilityChangeListener != null) {
                visibilityChangeListener.accept(true);
            }
        } else {
            mcpToolsCountLabel.setVisible(false);
            if (visibilityChangeListener != null) {
                visibilityChangeListener.accept(false);
            }
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
        
        List<JCheckBox> serverCheckboxes = new ArrayList<>();
        
        for (MCPServer server : serverList) {
            JPanel serverPanel = new JPanel(new BorderLayout());
            serverPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            
            JCheckBox enabledCheckbox = new JCheckBox(server.getName());
            enabledCheckbox.setSelected(server.isEnabled());
            enabledCheckbox.setFont(enabledCheckbox.getFont().deriveFont(Font.PLAIN));
            
            // Add tooltip with tool count
            int toolCount = server.getAvailableTools().size();
            enabledCheckbox.setToolTipText(String.format("%s - %d tool%s available", 
                    server.getName(), toolCount, toolCount == 1 ? "" : "s"));
            
            serverCheckboxes.add(enabledCheckbox);
            
            // Add label with tool count
            JLabel toolCountLabel = new JLabel(String.format("(%d tool%s)", 
                    toolCount, toolCount == 1 ? "" : "s"));
            toolCountLabel.setFont(toolCountLabel.getFont().deriveFont(Font.PLAIN));
            toolCountLabel.setForeground(Color.GRAY);
            
            serverPanel.add(enabledCheckbox, BorderLayout.WEST);
            serverPanel.add(toolCountLabel, BorderLayout.EAST);
            
            // Add the server panel to the list panel
            mcpListPanel.add(serverPanel);
            
            // Add action listener to auto-apply changes when checkbox is toggled
            enabledCheckbox.addActionListener(e -> {
                // Update the server's enabled state
                boolean newState = enabledCheckbox.isSelected();
                if (server.isEnabled() != newState) {
                    server.setEnabled(newState);
                    
                    // Update the server in the settings
                    DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers()
                            .put(server.getName(), server);
                    
                    // Notify the MCPService about the changes
                    MCPService.refreshToolWindowVisibility();
                    
                    // Update the UI
                    updateMCPToolsCounter();
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
        public String toString() {
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
    private void showAllMCPTools(Map<String, MCPServer> mcpServers) {
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
