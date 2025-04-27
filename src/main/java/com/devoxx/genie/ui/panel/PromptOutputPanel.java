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
    private final WaitingPanel waitingPanel = new WaitingPanel();
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

        // Subscribe to MCP messages
        ApplicationManager.getApplication().invokeLater(() ->
                MessageBusUtil.connect(project, connection ->
                    MessageBusUtil.subscribe(connection, AppTopics.MCP_LOGGING_MSG, this)
                ));
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
        waitingPanel.hideMsg();
        
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
            // Add the message to the conversation panel
            conversationPanel.addChatMessage(chatMessageContext);
        }
    }

    /**
     * Adds file references from a streaming response to the panel.
     *
     * @param fileListPanel The panel containing file references.
     */
    public void addStreamFileReferencesResponse(ExpandablePanel fileListPanel) {
        // TODO: Handle file references in the WebView
        // For now, create a separate dialog for file references
        JDialog dialog = new JDialog();
        dialog.setTitle("File References");
        dialog.setContentPane(fileListPanel);
        dialog.setSize(800, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
            // Parse and render markdown content
            String markdownContent = "⦿ " + message.getContent();
            
            // Create a chat message context for MCP messages
            ChatMessageContext chatMessageContext = ChatMessageContext.builder()
                    .id("mcp-" + UUID.randomUUID())
                    .project(project)
                    .userPrompt("")
                    .aiMessage(AiMessage.aiMessage(markdownContent))
                    .executionTimeMs(0)
                    .build();
            
            // Add the message to the conversation panel
            ApplicationManager.getApplication().invokeLater(() -> {
                // Ensure we're showing the conversation panel
                cardLayout.show(cards, "conversation");
                conversationPanel.addChatMessage(chatMessageContext);
            });
        }
    }
}
