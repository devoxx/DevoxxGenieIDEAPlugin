package com.devoxx.genie.ui.panel;

// Import necessary classes and packages
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.HelpUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import dev.langchain4j.data.message.AiMessage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * This class represents the output panel for displaying chat prompts and responses.
 * It manages the user interface components related to displaying conversation history,
 * help messages, and user prompts.
 */
public class PromptOutputPanel extends JBPanel<PromptOutputPanel> implements CustomPromptChangeListener {

    private final Project project; // Holds the current project context
    private final ResourceBundle resourceBundle; // Resource bundle for localization

    private final JPanel container = new JPanel(); // Main container for chat components
    private final WelcomePanel welcomePanel; // Panel to display welcome message
    private final HelpPanel helpPanel; // Panel to display help information
    private final WaitingPanel waitingPanel = new WaitingPanel(); // Panel to indicate waiting state
    private final JBScrollPane scrollPane; // Scrollable pane for the container

    /**
     * Constructor for PromptOutputPanel.
     *
     * @param project       The current project.
     * @param resourceBundle The resource bundle for localization.
     */
    public PromptOutputPanel(Project project, ResourceBundle resourceBundle) {
        super();

        // Initialize fields
        this.project = project;
        this.resourceBundle = resourceBundle;
        welcomePanel = new WelcomePanel(resourceBundle);
        helpPanel = new HelpPanel(HelpUtil.getHelpMessage(resourceBundle));

        // Set layout for the container
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        // Initialize scroll pane with the container
        scrollPane = new JBScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

        // Set layout for the main panel and add components
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        // Set minimum size for the panel
        setMinimumSize(new Dimension(200, 200));
        showWelcomeText(); // Display the welcome message on initialization
    }

    /**
     * Clears the container and shows the welcome text.
     */
    public void clear() {
        container.removeAll(); // Remove all components
        showWelcomeText(); // Show welcome panel
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Displays the welcome message in the panel.
     */
    public void showWelcomeText() {
        welcomePanel.showMsg(); // Show the welcome message
        container.add(welcomePanel); // Add welcome panel to the container
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Displays help text in the panel.
     */
    public void showHelpText() {
        container.remove(welcomePanel); // Remove welcome panel
        container.add(helpPanel); // Add help panel
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Adds a filler component to the container.
     *
     * @param name The name of the filler.
     */
    private void addFiller(String name) {
        container.add(new FillerPanel(name)); // Add a filler panel
    }

    /**
     * Adds a user prompt to the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void addUserPrompt(ChatMessageContext chatMessageContext) {
        container.remove(welcomePanel); // Remove the welcome panel

        UserPromptPanel userPromptPanel = new UserPromptPanel(container, chatMessageContext); // Create user prompt panel

        // Display waiting message if not in stream mode
        if (!DevoxxGenieStateService.getInstance().getStreamMode()) {
            waitingPanel.showMsg();
            userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);
        }

        addFiller(chatMessageContext.getId()); // Add filler for the user prompt
        container.add(userPromptPanel); // Add user prompt panel
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Adds a chat response to the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void addChatResponse(@NotNull ChatMessageContext chatMessageContext) {
        waitingPanel.hideMsg(); // Hide the waiting message
        addFiller(chatMessageContext.getId()); // Add filler for the chat response
        container.add(new ChatResponsePanel(chatMessageContext)); // Add chat response panel
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Adds a streaming response to the panel.
     *
     * @param chatResponseStreamingPanel The streaming response panel.
     */
    public void addStreamResponse(ChatStreamingResponsePanel chatResponseStreamingPanel) {
        container.add(chatResponseStreamingPanel); // Add the streaming response panel
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Adds file references from a streaming response to the panel.
     *
     * @param fileListPanel The panel containing file references.
     */
    public void addStreamFileReferencesResponse(ExpandablePanel fileListPanel) {
        container.add(fileListPanel); // Add the file references panel
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Removes the last user prompt from the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void removeLastUserPrompt(ChatMessageContext chatMessageContext) {
        // Iterate through components to find and remove the last user prompt
        for (Component component : container.getComponents()) {
            if (component instanceof UserPromptPanel && component.getName().equals(chatMessageContext.getId())) {
                container.remove(component); // Remove the user prompt panel
                break;
            }
        }
        revalidate(); // Revalidate the container
        repaint(); // Repaint the container
        scrollToBottom(); // Scroll to the bottom
    }

    /**
     * Scrolls the view to the bottom of the scroll pane.
     */
    private void scrollToBottom() {
        ApplicationManager.getApplication().invokeLater(() -> {
            Timer timer = new Timer(100, e -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar(); // Get the vertical scrollbar
                vertical.setValue(vertical.getMaximum()); // Set the scrollbar to the maximum value
            });
            timer.setRepeats(false); // Ensure timer only runs once
            timer.start(); // Start the timer
        });
    }

    /**
     * Updates the help text displayed in the help panel.
     */
    public void updateHelpText() {
        helpPanel.updateHelpText(HelpUtil.getHelpMessage(resourceBundle)); // Update help text from resource bundle
    }

    /**
     * Displays an entire conversation in the panel.
     *
     * @param conversation The conversation to be displayed.
     */
    public void displayConversation(Conversation conversation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String conversationId = UUID.randomUUID().toString(); // Generate a unique ID for the conversation
            for (ChatMessage message : conversation.getMessages()) {
                conversation.setId(conversationId); // Set the conversation ID
                ChatMessageContext chatMessageContext = createChatMessageContext(conversation, message); // Create message context
                if (message.isUser()) {
                    addUserPrompt(chatMessageContext); // Add user prompt
                } else {
                    addChatResponse(chatMessageContext); // Add chat response
                }
            }
            scrollToBottom(); // Scroll to the bottom after adding messages
        });
    }

    /**
     * Creates a ChatMessageContext from a conversation and a chat message.
     *
     * @param conversation The conversation containing the message.
     * @param message      The chat message.
     * @return The created ChatMessageContext.
     */
    private ChatMessageContext createChatMessageContext(@NotNull Conversation conversation,
                                                        @NotNull ChatMessage message) {
        // Build and return a ChatMessageContext
        return ChatMessageContext.builder()
                .id(conversation.getId())
                .project(project)
                .userPrompt(message.isUser() ? message.getContent() : "")
                .aiMessage(message.isUser() ? null : AiMessage.aiMessage(message.getContent()))
                .totalFileCount(0)
                .executionTimeMs(conversation.getExecutionTimeMs())
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.valueOf(conversation.getLlmProvider()))
                        .modelName(conversation.getModelName())
                        .apiKeyUsed(conversation.getApiKeyUsed())
                        .inputCost(conversation.getInputCost() == null ? 0 : conversation.getInputCost())
                        .outputCost(conversation.getOutputCost() == null ? 0 : conversation.getOutputCost())
                        .contextWindow(conversation.getContextWindow() == null ? 0 : conversation.getContextWindow())
                        .build())
                .cost(0).build();
    }

    /**
     * Called when custom prompts have changed. Updates the help text accordingly.
     */
    @Override
    public void onCustomPromptsChanged() {
        // Update the help panel or any other UI components that display custom prompts
        ApplicationManager.getApplication().invokeLater(this::updateHelpText);
    }
}
