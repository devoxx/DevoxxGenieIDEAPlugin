package com.devoxx.genie.ui.panel;

import com.devoxx.genie.controller.ActionButtonsPanelController;
import com.devoxx.genie.controller.ProjectContextController;
import com.devoxx.genie.controller.listener.TokenCalculationListener;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.component.TokenUsageBar;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.listener.GlowingListener;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

import javax.swing.*;
import java.awt.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import java.awt.event.ActionEvent;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class ActionButtonsPanel extends JPanel
        implements SettingsChangeListener, PromptSubmissionListener, GlowingListener, TokenCalculationListener {

    private final transient Project project;

    @Getter
    private final transient EditorFileButtonManager editorFileButtonManager;

    private JButton addFileBtn;
    private JButton submitBtn;
    private JButton addProjectBtn;
    private JLabel mcpToolsCountLabel;
    private JButton calcTokenCostBtn;

    private final SubmitPanel submitPanel;

    private final JPanel calcProjectPanel = createCalcProjectPanel();

    private final PromptInputArea promptInputArea;
    private final TokenUsageBar tokenUsageBar = createTokenUsageBar();

    private final transient DevoxxGenieToolWindowContent devoxxGenieToolWindowContent;

    private final transient ActionButtonsPanelController controller;
    private final transient ProjectContextController projectContextController;

    public ActionButtonsPanel(Project project,
                              SubmitPanel submitPanel,
                              PromptInputArea promptInputArea,
                              PromptOutputPanel promptOutputPanel,
                              ComboBox<ModelProvider> llmProvidersComboBox,
                              ComboBox<LanguageModel> modelNameComboBox,
                              DevoxxGenieToolWindowContent devoxxGenieToolWindowContent) {
        setLayout(new BorderLayout());

        // Initialize fields and components
        this.project = project;
        this.submitPanel = submitPanel;
        this.promptInputArea = promptInputArea;
        this.editorFileButtonManager = new EditorFileButtonManager(project, addFileBtn);
        this.devoxxGenieToolWindowContent = devoxxGenieToolWindowContent;

        this.projectContextController = new ProjectContextController(
                project, llmProvidersComboBox, modelNameComboBox, this);

        this.controller = new ActionButtonsPanelController(
                project, promptInputArea, promptOutputPanel,
                llmProvidersComboBox, modelNameComboBox, this
        );

        llmProvidersComboBox.addActionListener(e -> controller.updateButtonVisibility());

        // Call setupUI which will create the buttons before creating the button panel
        setupUI();
        setupAccessibility();
    }

    private void setupUI() {
        createButtons();
        createMCPToolsCounter();

        add(createProgressPanel(), BorderLayout.NORTH);
        add(createButtonPanel(), BorderLayout.CENTER);
    }
    
    private void createMCPToolsCounter() {
        mcpToolsCountLabel = new JLabel();
        mcpToolsCountLabel.setIcon(HammerIcon);
        mcpToolsCountLabel.setToolTipText("Total MCP Tools Available");
        mcpToolsCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        mcpToolsCountLabel.setFont(mcpToolsCountLabel.getFont().deriveFont(Font.BOLD));
        mcpToolsCountLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        mcpToolsCountLabel.setIconTextGap(4);
        mcpToolsCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Add click listener for showing the tool list popup
        mcpToolsCountLabel.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMCPToolsPopup();
            }
        });
        
        updateMCPToolsCounter();
    }

    /**
     * Shows a popup with the list of all MCP tools and their descriptions.
     */
    private void showMCPToolsPopup() {
        if (!MCPService.isMCPEnabled()) {
            return;
        }
        
        // Collect all tool information from enabled servers
        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers();
        List<ToolInfo> allTools = new ArrayList<>();
        
        mcpServers.values().stream()
                .filter(MCPServer::isEnabled)
                .forEach(server -> {
                    for (String toolName : server.getAvailableTools()) {
                        String description = server.getToolDescriptions().getOrDefault(toolName, "");
                        allTools.add(new ToolInfo(server.getName(), toolName, description));
                    }
                });
        
        if (allTools.isEmpty()) {
            return;
        }
        
        // Sort tools alphabetically by name
        allTools.sort(Comparator.comparing(ToolInfo::toolName));
        
        // Create a panel with a list of tools
        JPanel popupPanel = new JPanel(new BorderLayout());
        
        DefaultListModel<ToolInfo> listModel = new DefaultListModel<>();
        allTools.forEach(listModel::addElement);
        
        JBList<ToolInfo> toolsList = new JBList<>(listModel);
        toolsList.setCellRenderer(new ToolInfoRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(toolsList);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        popupPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create and show the popup
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupPanel, null)
                .setTitle("Available MCP Tools")
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
     * Updates the MCP Tools counter to display the total number of tools provided by all activated MCP servers.
     */
    public void updateMCPToolsCounter() {
        if (!MCPService.isMCPEnabled()) {
            mcpToolsCountLabel.setVisible(false);
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
            
            toolTip.append("<br>Click to open MCP logs panel<br>");
            toolTip.append("Shift+Click to see tools list</html>");
            mcpToolsCountLabel.setToolTipText(toolTip.toString());
        } else {
            mcpToolsCountLabel.setVisible(false);
        }
    }

    private void createButtons() {
        submitBtn = createActionButton(SubmitIcon, this::onSubmitPrompt);
        addFileBtn = createActionButton(AddFileIcon, this::selectFilesForPromptContext);
        addProjectBtn = createActionButton(ADD_PROJECT_TO_CONTEXT, AddFileIcon, this::handleProjectContext);
        calcTokenCostBtn = createActionButton(CALC_TOKENS_COST, CalculateIcon, e -> controller.calculateTokensAndCost());
    }

    private @NotNull JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        
        // Main buttons using GridLayout
        JPanel mainButtons = new JPanel(new GridLayout(1, 4, 5, 0));
        mainButtons.add(submitBtn);
        mainButtons.add(calcTokenCostBtn);
        mainButtons.add(addProjectBtn);
        mainButtons.add(addFileBtn);
        
        buttonPanel.add(mainButtons, BorderLayout.CENTER);
        
        // MCP Tools counter on the right
        JPanel mcpPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        mcpPanel.add(mcpToolsCountLabel);
        
        buttonPanel.add(mcpPanel, BorderLayout.EAST);
        
        return buttonPanel;
    }

    private @NotNull TokenUsageBar createTokenUsageBar() {
        TokenUsageBar tokenUsageBar = new TokenUsageBar();
        tokenUsageBar.setVisible(false);
        tokenUsageBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 3));
        return tokenUsageBar;
    }

    private @NotNull JPanel createCalcProjectPanel() {
        return new JPanel(new GridLayout(1, 2));
    }

    private @NotNull JPanel createProgressPanel() {
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(tokenUsageBar, BorderLayout.CENTER);
        return progressPanel;
    }

    private void handleProjectContext(ActionEvent e) {
        if (projectContextController.isProjectContextAdded()) {
            confirmProjectContextRemoval();
        } else {
            projectContextController.addProjectContext();
        }
    }

    /**
     * Add files to the prompt context.
     */
    private void selectFilesForPromptContext(ActionEvent e) {
        java.util.List<VirtualFile> openFiles = editorFileButtonManager.getOpenFiles();
        List<VirtualFile> sortedFiles = new ArrayList<>(openFiles);
        sortedFiles.sort(Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(FileSelectionPanelFactory.createPanel(project, sortedFiles), null)
                .setTitle(FILTER_AND_DOUBLE_CLICK_TO_ADD_TO_PROMPT_CONTEXT)
                .setRequestFocus(true)
                .setResizable(true)
                .setMovable(false)
                .setMinSize(new Dimension(300, 350))
                .createPopup();

        if (addFileBtn.isShowing()) {
            new ContextPopupMenu().show(submitBtn,
                    popup,
                    devoxxGenieToolWindowContent.getContentPanel().getSize().width,
                    promptInputArea.getLocationOnScreen().y);
        }
    }

    /**
     * Submit the user prompt.
     */
    private void onSubmitPrompt(ActionEvent actionEvent) {
        if (controller.isPromptRunning()) {
            controller.stopPromptExecution();
            return;
        }

        boolean response = controller.handlePromptSubmission(actionEvent.getActionCommand(),
                projectContextController.isProjectContextAdded(),
                projectContextController.getProjectContext());

        if (!response) {
            controller.endPromptExecution();
        }
    }

    public void enableButtons() {
        ApplicationManager.getApplication().invokeLater(() -> {
            submitBtn.setIcon(SubmitIcon);
            promptInputArea.setEnabled(true);
            submitPanel.stopGlowing();
        });
    }

    public void disableButtons() {
        promptInputArea.setEnabled(false);
    }

    public void disableSubmitBtn() {
        ApplicationManager.getApplication().invokeLater(() -> {
            submitBtn.setIcon(StopIcon);
            submitBtn.setToolTipText(PROMPT_IS_RUNNING_PLEASE_BE_PATIENT);
        });
    }

    public void resetProjectContext() {
        projectContextController.resetProjectContext();
    }

    public void updateAddProjectButton(boolean isProjectContextAdded) {
        updateAddProjectButton(isProjectContextAdded, 0);
    }

    public void updateAddProjectButton(boolean isProjectContextAdded, int tokenCount) {
        if (isProjectContextAdded) {
            setAddProjectButton(DeleteIcon, REMOVE_CONTEXT, REMOVE_ENTIRE_PROJECT_FROM_PROMPT_CONTEXT);
            if (tokenCount > 0) {
                addProjectBtn.setText(WindowContextFormatterUtil.format(tokenCount, "tokens"));
            }
        } else {
            setAddProjectButton(AddFileIcon, ADD_PROJECT_TO_CONTEXT, ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT);
        }
    }

    private void setAddProjectButton(Icon addFileIcon, String addProjectToContext, String addEntireProjectToPromptContext) {
        addProjectBtn.setIcon(addFileIcon);
        addProjectBtn.setText(addProjectToContext);
        addProjectBtn.setToolTipText(addEntireProjectToPromptContext);
    }

    public void setAddProjectButtonEnabled(boolean enabled) {
        addProjectBtn.setEnabled(enabled);
    }

    public void setTokenUsageBarVisible(boolean visible) {
        tokenUsageBar.setVisible(visible);
    }

    public void resetTokenUsageBar() {
        ApplicationManager.getApplication().invokeLater(tokenUsageBar::reset);
    }

    @Override
    public void settingsChanged(boolean hasKey) {
        calcProjectPanel.setVisible(hasKey && projectContextController.isProjectContextSupportedProvider());
        controller.updateButtonVisibility();
        updateMCPToolsCounter();
    }

    public void updateTokenUsage(int maxTokens) {
        ApplicationManager.getApplication().invokeLater(() -> tokenUsageBar.setMaxTokens(maxTokens));
    }

    @Override
    public void onPromptSubmitted(@NotNull Project projectPrompt, String prompt) {
        if (!this.project.getName().equals(projectPrompt.getName())) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            promptInputArea.setText(prompt);
            onSubmitPrompt(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Constant.SUBMIT_ACTION));
        });
    }

    private void setupAccessibility() {
        submitBtn.getAccessibleContext().setAccessibleDescription("Submit prompt to AI");
        addFileBtn.getAccessibleContext().setAccessibleDescription("Add files to context");

        // Add keyboard mnemonics
        submitBtn.setMnemonic('S');
        addFileBtn.setMnemonic('A');
    }

    private void confirmProjectContextRemoval() {
        int result = Messages.showYesNoDialog(project,
                "Are you sure you want to remove the project context?",
                "Confirm Removal",
                Messages.getQuestionIcon()
        );
        if (result == Messages.YES) {
            projectContextController.resetProjectContext();
        }
    }

    public void setCalcTokenCostButtonVisible(boolean visible) {
        calcTokenCostBtn.setVisible(visible);
    }

    public void setAddProjectButtonVisible(boolean visible) {
        addProjectBtn.setVisible(visible);
    }

    @Override
    public void startGlowing() {
        submitPanel.startGlowing();
    }

    @Override
    public void stopGlowing() {
        submitPanel.stopGlowing();
    }

    public void updateTokenUsageBar(int tokenCount, int tokenLimit) {
        ApplicationManager.getApplication().invokeLater(() -> tokenUsageBar.setTokens(tokenCount, tokenLimit));
    }

    @Override
    public void onTokenCalculationComplete(String message) {
        NotificationUtil.sendNotification(project, message);
    }
}
