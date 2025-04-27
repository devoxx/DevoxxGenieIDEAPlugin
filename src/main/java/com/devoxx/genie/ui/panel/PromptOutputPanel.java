package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.HelpUtil;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import dev.langchain4j.data.message.AiMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;

/**
 * This class represents the output panel for displaying chat prompts and responses.
 * It manages the user interface components related to displaying conversation history,
 * help messages, and user prompts.
 */
@Slf4j
public class PromptOutputPanel extends JBPanel<PromptOutputPanel> implements CustomPromptChangeListener, MCPLoggingMessage {

    private final transient Project project;
    
    @Getter
    private final ConversationPanel conversationPanel;
    private final HelpPanel helpPanel;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    /**
     * Constructor for PromptOutputPanel.
     *
     * @param project       The current project.
     * @param resourceBundle The resource bundle for localization.
     */
    public PromptOutputPanel(Project project, ResourceBundle resourceBundle) {
        super(new BorderLayout());

        this.project = project;

        // Initialize panels with proper sizes
        conversationPanel = new ConversationPanel(project, resourceBundle);
        helpPanel = new HelpPanel(HelpUtil.getHelpMessage());
        
        // Add components to the card panel
        cards.add(conversationPanel, "conversation");
        cards.add(helpPanel, "help");
        
        // This is a key step - add our card panel to fill the entire area
        add(cards, BorderLayout.CENTER);

        // Set size constraints
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(800, 600));
        
        // Show the welcome text initially
        showWelcomeText();

        // Register this panel with the registry
        PromptPanelRegistry.getInstance().registerPanel(project, this);

        // Subscribe to MCP messages and file references
        ApplicationManager.getApplication().invokeLater(() ->
                MessageBusUtil.connect(project, connection -> {
                    MessageBusUtil.subscribe(connection, AppTopics.MCP_LOGGING_MSG, this);
                    // Also subscribe to file reference events
                    MessageBusUtil.subscribe(connection, AppTopics.FILE_REFERENCES_TOPIC, 
                        conversationPanel); // Delegate to the conversation panel
                }));
    }

    /**
     * Clears the container and shows the welcome text.
     */
    public void clear() {
        conversationPanel.clear();
        showWelcomeText();
    }

    /**
     * Displays the welcome message in the panel.
     */
    public void showWelcomeText() {
        cardLayout.show(cards, "conversation");
        conversationPanel.showWelcome();
    }

    /**
     * Displays help text in the panel.
     */
    public void showHelpText() {
        cardLayout.show(cards, "help");
    }

    /**
     * Adds a chat response to the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void addChatResponse(@NotNull ChatMessageContext chatMessageContext) {

        // Ensure we're showing the conversation panel
        cardLayout.show(cards, "conversation");

        // Special handling for find command
        if (FIND_COMMAND.equals(chatMessageContext.getCommandName()) &&
            chatMessageContext.getSemanticReferences() != null &&
            !chatMessageContext.getSemanticReferences().isEmpty()) {
            // TODO: Handle find command results in the WebView
            // For now, create a separate panel for find results
            JPanel findResultsContainer = new JPanel(new BorderLayout());
            findResultsContainer.add(new FindResultsPanel(project, chatMessageContext.getSemanticReferences()), BorderLayout.CENTER);
            JDialog dialog = new JDialog();
            dialog.setTitle("Find Results");
            dialog.setContentPane(findResultsContainer);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } else {
            // Update the existing message pair instead of creating a new one
            conversationPanel.updateUserPromptWithResponse(chatMessageContext);
        }
    }

    /**
     * Adds file references from a streaming response to the panel.
     * This method is kept for backward compatibility but no longer shows a dialog.
     * Instead, file references are now embedded directly in the HTML.
     *
     * @param fileListPanel The panel containing file references.
     * @deprecated File references are now shown inline in the web view
     */
    @Deprecated
    public void addStreamFileReferencesResponse(ExpandablePanel fileListPanel) {
        // No longer creating a dialog - references are shown in the HTML now
    }

    /**
     * Updates the help text displayed in the help panel.
     */
    public void updateHelpText() {
        helpPanel.updateHelpText(HelpUtil.getHelpMessage());
    }

    /**
     * Called when custom prompts have changed. Updates the help text accordingly.
     */
    @Override
    public void onCustomPromptsChanged() {
        ApplicationManager.getApplication().invokeLater(() -> {
            updateHelpText();
            conversationPanel.onCustomPromptsChanged();
        });
    }

    @Override
    public void onMCPLoggingMessage(@NotNull MCPMessage message) {
        if (message.getType().equals(MCPType.AI_MSG)) {
            
            // Create a chat message context for MCP messages
            ChatMessageContext chatMessageContext = ChatMessageContext.builder()
                    .id("mcp-" + UUID.randomUUID())
                    .project(project)
                    .userPrompt("")
                    .aiMessage(AiMessage.aiMessage(message.getContent()))
                    .executionTimeMs(0)
                    .build();
            
            // Add the message to the conversation panel 
            ApplicationManager.getApplication().invokeLater(() -> {
                // Ensure we're showing the conversation panel
                cardLayout.show(cards, "conversation");
                // For MCP messages, we still use addChatMessage since there is no user prompt to update
                conversationPanel.addChatMessage(chatMessageContext);
            });
        }
    }
    
    /**
     * Called when panel is removed/disposed.
     * Unregisters from the panel registry.
     */
    @Override
    public void removeNotify() {
        // Unregister from the registry when removed from the UI
        PromptPanelRegistry.getInstance().unregisterPanel(project, this);
        super.removeNotify();
    }
    
    /**
     * Scrolls the conversation view to the bottom.
     * Used when a prompt is submitted to ensure the latest content is visible.
     */
    public void scrollToBottom() {
        ApplicationManager.getApplication().invokeLater(() -> {
            // First make sure we're showing the conversation panel
            cardLayout.show(cards, "conversation");
            // Then defer to the conversation panel which contains the WebViewController
            conversationPanel.scrollToBottom();
        });
    }
}
