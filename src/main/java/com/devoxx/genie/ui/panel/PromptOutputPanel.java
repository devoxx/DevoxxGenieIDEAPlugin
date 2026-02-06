package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.panel.conversation.ConversationPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.HelpUtil;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;

/**
 * This class represents the output panel for displaying chat prompts and responses.
 * It manages the user interface components related to displaying conversation history,
 * help messages, and user prompts.
 */
@Slf4j
public class PromptOutputPanel extends JBPanel<PromptOutputPanel> implements CustomPromptChangeListener {

    private final transient Project project;
    
    @Getter
    private final ConversationPanel conversationPanel;

    // Flag to track if we're in a new conversation (no messages sent yet)
    @Getter
    private boolean isNewConversation = true;

    /**
     * Constructor for PromptOutputPanel.
     *
     * @param project       The current project.
     * @param resourceBundle The resource bundle for localization.
     */
    public PromptOutputPanel(Project project, ResourceBundle resourceBundle) {
        super(new BorderLayout());

        this.project = project;

        // Initialize conversation panel
        conversationPanel = new ConversationPanel(project, resourceBundle);
        
        // Add conversation panel directly to this panel
        add(conversationPanel, BorderLayout.CENTER);

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
        
        // Reset the new conversation flag since we've cleared everything
        isNewConversation = true;
    }

    /**
     * Displays the welcome message in the panel.
     */
    public void showWelcomeText() {
        conversationPanel.showWelcome();
    }

    /**
     * Displays help text in the panel.
     * Now creates a help message as a direct AI response in the conversation panel
     * without showing a separate user prompt for it.
     */
    public void showHelpText() {
        String helpId = "help_" + System.currentTimeMillis();
        
        // Create special help content that doesn't include a user message
        String helpContent = HelpUtil.getHelpMessage();
        
        // Use direct JavaScript to add the help message to avoid the "Thinking..." indicator
        // and duplicate /help command display
        conversationPanel.webViewController.executeJavaScript(
            "const container = document.getElementById('conversation-container');" +
            "const messagePair = document.createElement('div');" +
            "messagePair.className = 'message-pair';" +
            "messagePair.id = '" + helpId + "';" +
            "const aiMessage = document.createElement('div');" +
            "aiMessage.className = 'assistant-message';" +
            "aiMessage.innerHTML = `" + 
            "<div class='metadata-info'>Available commands:</div>" +
            "<button class='copy-response-button' onclick='copyMessageResponse(this)'>Copy</button>" +
            helpContent + "`;" +
            "messagePair.appendChild(aiMessage);" +
            "container.appendChild(messagePair);" +
            "window.scrollTo(0, document.body.scrollHeight);" +
            "if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }"
        );
    }

    /**
     * Adds a chat response to the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void addChatResponse(@NotNull ChatMessageContext chatMessageContext) {
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
     * Creates a new help context and shows it in the conversation panel.
     * Called when custom prompts have changed.
     */
    public void updateHelpText() {
        String helpId = "help_updated_" + System.currentTimeMillis();
        
        // Create special help content that doesn't include a user message
        String helpContent = HelpUtil.getHelpMessage();
        
        // Use direct JavaScript to add the help message to avoid the "Thinking..." indicator
        // and duplicate /help command display
        conversationPanel.webViewController.executeJavaScript(
            "const container = document.getElementById('conversation-container');" +
            "const messagePair = document.createElement('div');" +
            "messagePair.className = 'message-pair';" +
            "messagePair.id = '" + helpId + "';" +
            "const aiMessage = document.createElement('div');" +
            "aiMessage.className = 'assistant-message';" +
            "aiMessage.innerHTML = `" + 
            "<div class='metadata-info'>Updated commands:</div>" +
            "<button class='copy-response-button' onclick='copyMessageResponse(this)'>Copy</button>" +
            helpContent + "`;" +
            "messagePair.appendChild(aiMessage);" +
            "container.appendChild(messagePair);" +
            "window.scrollTo(0, document.body.scrollHeight);" +
            "if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }"
        );
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

    /**
     * Called when panel is removed/disposed.
     * Unregisters from the panel registry.
     */
    @Override
    public void removeNotify() {
        // Unregister from the registry when removed from the UI
        PromptPanelRegistry.getInstance().unregisterPanel(project, this);
        // Cascade disposal to the conversation panel to clean up browser, timers, and bus connections
        if (conversationPanel != null) {
            conversationPanel.dispose();
        }
        super.removeNotify();
    }
    
    /**
     * Scrolls the conversation view to the bottom.
     * Used when a prompt is submitted to ensure the latest content is visible.
     */
    public void scrollToBottom() {
        // Defer to the conversation panel which contains the WebViewController
        ApplicationManager.getApplication().invokeLater(conversationPanel::scrollToBottom);
    }

    /**
     * Marks the conversation as no longer new after the first prompt is submitted.
     * This is called from the controller after processing the first prompt.
     */
    public void markConversationAsStarted() {
        isNewConversation = false;
    }
}
