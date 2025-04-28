package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.AiMessage;
import com.devoxx.genie.service.ChatService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.listener.ConversationStarter;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.listener.FileReferencesListener;
import com.devoxx.genie.ui.panel.conversationhistory.ConversationHistoryPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.SettingsDialogUtil;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import com.intellij.openapi.vfs.VirtualFile;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;
import static com.devoxx.genie.ui.util.TimestampUtil.getCurrentTimestamp;

/**
 * A panel containing a single WebView that displays the entire conversation.
 * This replaces the previous approach of creating separate panels for each message.
 */
@Slf4j
public class ConversationPanel
        extends JBPanel<ConversationPanel>
        implements CustomPromptChangeListener, ConversationSelectionListener, ConversationEventListener, ConversationStarter, FileReferencesListener {

    // Make the webViewController accessible
    public final ConversationWebViewController webViewController;

    private final Project project;
    private final ResourceBundle resourceBundle;
    private JButton settingsBtn;
    private final ConversationHistoryPanel historyPanel;
    private final ChatService chatService;
    private JLabel conversationLabel;

    /**
     * Creates a new conversation panel.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public ConversationPanel(Project project, ResourceBundle resourceBundle) {
        super(new BorderLayout());

        this.project = project;
        this.resourceBundle = resourceBundle;

        webViewController = new ConversationWebViewController();

        add(createButtonPanel(), BorderLayout.NORTH);

        historyPanel = new ConversationHistoryPanel(project);
        chatService = new ChatService(project);

        // Create the WebViewController for managing the conversation content
        JBCefBrowser browser = webViewController.getBrowser();

        JComponent browserComponent = browser.getComponent();
        browserComponent.setOpaque(true);
        browserComponent.setBackground(Color.BLACK);
        
        // Set browser layout and sizing
        add(browserComponent, BorderLayout.CENTER);
        
        // Set sizes for the panel to ensure proper display
        setMinimumSize(new Dimension(400, 300));
        setPreferredSize(new Dimension(800, 600));
        
        // Ensure the component is visible
        setOpaque(true);
        setBackground(Color.BLACK);
        setVisible(true);
        
        // Subscribe to the FILE_REFERENCES_TOPIC and CONVERSATION_SELECTION_TOPIC
        MessageBusConnection msgBusConnection = project.getMessageBus().connect();

        msgBusConnection.subscribe(AppTopics.FILE_REFERENCES_TOPIC, this);
        msgBusConnection.subscribe(AppTopics.CONVERSATION_SELECTION_TOPIC, this);
        // Subscribe webViewController to MCP log messages
        msgBusConnection.subscribe(AppTopics.MCP_LOGGING_MSG, webViewController);
    }

    /**
     * Show the welcome content.
     */
    public void showWelcome() {
        webViewController.loadWelcomeContent(resourceBundle);
    }

    /**
     * Add a chat message to the conversation.
     *
     * @param chatMessageContext The chat message context
     */
    public void addChatMessage(ChatMessageContext chatMessageContext) {
        webViewController.addChatMessage(chatMessageContext);
    }

    /**
     * Clear the conversation content.
     */
    public void clear() {
        webViewController.clearConversation();
        showWelcome();
    }
    
    /**
     * Clear the conversation content without showing the welcome message.
     * Used when restoring conversation history.
     */
    public void clearWithoutWelcome() {
        webViewController.clearConversation();
    }

    public void loadConversationHistory() {
        historyPanel.loadConversations();
    }

    /**
     * Called when custom prompts change - updates the welcome content if it's visible.
     */
    @Override
    public void onCustomPromptsChanged() {
        webViewController.updateCustomPrompts(resourceBundle);
    }

    /**
     * Create the button panel.
     *
     * @return the button panel
     */
    private @NotNull JPanel createButtonPanel() {
        JPanel headerButtonsPanel = new JPanel();

        conversationLabel = new JLabel("New conversation " + getCurrentTimestamp());

        headerButtonsPanel.add(conversationLabel, BorderLayout.WEST);

        JPanel conversationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(10), 0));
        conversationButtonPanel.add(createActionButton(PlusIcon, e -> startNewConversation()));
        conversationButtonPanel.add(createActionButton(ClockIcon, e -> showConversationHistory()));

        settingsBtn = createActionButton(CogIcon, e -> SettingsDialogUtil.showSettingsDialog(project));
        conversationButtonPanel.add(settingsBtn);

        headerButtonsPanel.add(conversationButtonPanel, BorderLayout.EAST);
        return headerButtonsPanel;
    }

    /**
     * Update the conversation label with new timestamp.
     */
    public void updateNewConversationLabel() {
        conversationLabel.setText("New conversation " + getCurrentTimestamp());
    }

    private void showConversationHistory() {
        // Always reload the conversation history before showing it
        loadConversationHistory();
        
        JBPopup historyPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(historyPanel, null)
                .setTitle("Conversation History")
                .setMovable(true)
                .setResizable(true)
                .setMinSize(new Dimension(500, 400))
                .createPopup();

        // Calculate the position for the popup
        int x = settingsBtn.getX() + settingsBtn.getWidth() - 500;
        int y = settingsBtn.getY() + settingsBtn.getHeight();

        // Convert to screen coordinates
        Point screenPoint = new Point(x, y);
        SwingUtilities.convertPointToScreen(screenPoint, settingsBtn.getParent());

        // Show the popup at the calculated position
        historyPopup.show(new RelativePoint(screenPoint));
        
        log.debug("Conversation history popup shown with freshly loaded conversations");
    }

    @Override
    public void onNewConversation(ChatMessageContext chatMessageContext) {
        // Reload the conversation history when a new conversation is created
        ApplicationManager.getApplication().invokeLater(() -> {
            loadConversationHistory();
            log.debug("Conversation history reloaded after new conversation event");
        });
    }

    /**
     * Start a new conversation.
     * Clear the conversation panel, prompt input area, prompt output panel, file list and chat memory.
     */
    @Override
    public void startNewConversation() {
        FileListManager.getInstance().clear(project);
        ChatMemoryService.getInstance().clearMemory(project);

        chatService.startNewConversation("");

        ApplicationManager.getApplication().invokeLater(() -> {
            updateNewConversationLabel();
            clear();
            
            // Make sure all panels know this is a new conversation
            for (PromptOutputPanel panel : PromptPanelRegistry.getInstance().getPanels(project)) {
                // The clear() method already resets isNewConversation = true
                panel.clear();
            }
        });
    }

    @Override
    public void onConversationSelected(Conversation conversation) {
        log.debug("Starting conversation restoration for ID: {}", conversation.getId());
        
        // Update the conversation label with the title
        conversationLabel.setText(conversation.getTitle());

        // Check if conversation has any messages
        List<ChatMessage> messages = conversation.getMessages();
        if (messages == null || messages.isEmpty()) {
            log.warn("Selected conversation has no messages to restore");
            return;
        }
        
        log.debug("Restoring conversation with {} messages", messages.size());
        
        // Clear the current conversation in the web view without showing welcome screen
        clearWithoutWelcome();
        
        // Mark this as not a new conversation in any panel that might be registered
        for (PromptOutputPanel panel : PromptPanelRegistry.getInstance().getPanels(project)) {
            panel.markConversationAsStarted();
        }
        
        // Make sure browser is fully initialized before adding messages
        if (!webViewController.isInitialized()) {
            log.info("Browser not yet fully initialized, waiting before restoration");
            webViewController.ensureBrowserInitialized(() -> restoreConversationMessages(conversation, messages));
        } else {
            restoreConversationMessages(conversation, messages);
        }
    }
    
    /**
     * Helper method to restore the messages of a conversation
     */
    private void restoreConversationMessages(Conversation conversation, List<ChatMessage> messages) {
        // Check if the conversation has any messages
        if (messages.isEmpty()) {
            log.warn("No messages to restore");
            return;
        }

        // Clear any existing DOM content first to prevent duplicate messages
        webViewController.clearConversation();
        
        // Process all messages
        int messageIndex = 0;
        long baseTimestamp = System.currentTimeMillis();

        // If the first message is an AI message, handle it specially
        if (messageIndex < messages.size() && !messages.get(messageIndex).isUser()) {
            log.info("First message is an AI message, creating an empty user message");
            
            // Create context for an empty user message paired with this AI message
            ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .executionTimeMs(conversation.getExecutionTimeMs() > 0 ? conversation.getExecutionTimeMs() : 1000)
                .build();
            
            // Set a stable, unique message ID based on conversation ID + message index
            String messageId = conversation.getId() + "_msg_" + messageIndex;
            context.setId(messageId);
            
            // Set empty user prompt
            context.setUserPrompt(""); 
            
            // Set the AI message
            ChatMessage aiMessage = messages.get(messageIndex);
            context.setAiMessage(AiMessage.from(aiMessage.getContent()));
            
            // Add LLM information
            populateModelInfo(context, conversation);
            
            // Add the message pair
            addCompleteChatMessage(context);
            
            // Advance to next message
            messageIndex++;
        }

        // Process remaining messages
        while (messageIndex < messages.size()) {
            // Get the current message
            ChatMessage currentMessage = messages.get(messageIndex);
            
            // Create the context for this message or message pair
            ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .executionTimeMs(conversation.getExecutionTimeMs() > 0 ? conversation.getExecutionTimeMs() : 1000)
                .build();
            
            // Set a stable, unique message ID based on conversation ID + message index
            // This ensures IDs are consistent if we restore the same conversation multiple times
            String messageId = conversation.getId() + "_msg_" + messageIndex;
            context.setId(messageId);
            
            // Add LLM information
            populateModelInfo(context, conversation);
            
            if (currentMessage.isUser()) {
                // Handle user message
                context.setUserPrompt(currentMessage.getContent());
                
                // Check if there's a next message that's an AI response
                if (messageIndex + 1 < messages.size() && !messages.get(messageIndex + 1).isUser()) {
                    // We have a matching pair
                    ChatMessage aiMessage = messages.get(messageIndex + 1);
                    context.setAiMessage(AiMessage.from(aiMessage.getContent()));
                    
                    // Add the complete message pair
                    addCompleteChatMessage(context);
                    
                    // Advance by 2 messages
                    messageIndex += 2;
                } else {
                    // Just a user message with no response
                    addUserMessageOnly(context);
                    messageIndex++;
                }
            } else {
                // Handle standalone AI message that wasn't paired with a user message
                log.warn("Found unpaired AI message at position {}", messageIndex);
                context.setUserPrompt(""); // Empty user prompt
                context.setAiMessage(AiMessage.from(currentMessage.getContent()));
                
                // Add the message pair with empty user message
                addCompleteChatMessage(context);
                messageIndex++;
            }
        }
            
        // After adding all messages, scroll to the top
        scrollToTop();
    }
    
    /**
     * Adds a complete chat message (user + AI) in a thread-safe way
     */
    private void addCompleteChatMessage(ChatMessageContext context) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                webViewController.addChatMessage(context);
                log.debug("Successfully added message pair with ID: {}", context.getId());
            } catch (Exception e) {
                log.error("Error adding chat message: {}", e.getMessage(), e);
            }
        });
        
        // Small delay to ensure proper rendering between messages
        sleep(75);
    }
    
    /**
     * Adds just a user message without AI response
     */
    private void addUserMessageOnly(ChatMessageContext context) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                webViewController.addUserPromptMessage(context);
                log.debug("Added user-only message with ID: {}", context.getId());
            } catch (Exception e) {
                log.error("Error adding user message: {}", e.getMessage(), e);
            }
        });
        
        // Small delay to ensure proper rendering
        sleep(50);
    }
    
    /**
     * Helper to populate model information 
     */
    private void populateModelInfo(ChatMessageContext context, Conversation conversation) {
        if (conversation.getModelName() != null && !conversation.getModelName().isEmpty()) {
            com.devoxx.genie.model.LanguageModel model = new com.devoxx.genie.model.LanguageModel();
            model.setModelName(conversation.getModelName());
            
            // Add provider information if available
            if (conversation.getLlmProvider() != null && !conversation.getLlmProvider().isEmpty()) {
                try {
                    model.setProvider(com.devoxx.genie.model.enumarations.ModelProvider.valueOf(conversation.getLlmProvider()));
                } catch (IllegalArgumentException e) {
                    // Handle case where provider name might have changed
                    log.warn("Unknown provider name in saved conversation: {}", conversation.getLlmProvider());
                }
            }
            
            model.setApiKeyUsed(conversation.getApiKeyUsed() != null ? conversation.getApiKeyUsed() : false);
            model.setDisplayName(conversation.getModelName());
            context.setLanguageModel(model);
        }
    }
    
    /**
     * Simple sleep helper
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Scrolls to the top of the conversation
     */
    private void scrollToTop() {
        // Small delay before scrolling to ensure all message rendering is complete
        ApplicationManager.getApplication().invokeLater(() -> {
            sleep(300);
            webViewController.executeJavaScript("window.scrollTo(0, 0);");
            log.debug("Conversation restoration completed and scrolled to top");
        });
    }
    
    /**
     * Handle file references being available for a chat message.
     * This is called when the non-streaming response handler wants to add file references.
     *
     * @param chatMessageContext The chat message context
     * @param files The list of files referenced in the chat
     */
    @Override
    public void onFileReferencesAvailable(@NotNull ChatMessageContext chatMessageContext, @NotNull List<VirtualFile> files) {
        log.debug("File references available for chat message: {}, files: {}", 
            chatMessageContext.getId(), files.size());
        
        // Use the web view controller to add file references to the conversation
        if (!files.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                webViewController.addFileReferences(chatMessageContext, files);
            });
        }
    }
    
    /**
     * Scrolls the conversation view to the bottom.
     * This is used both when a user submits a prompt and when a response is received.
     */
    public void scrollToBottom() {
        ApplicationManager.getApplication().invokeLater(() -> {
            webViewController.executeJavaScript("window.scrollTo(0, document.body.scrollHeight);");
        });
    }
    
    /**
     * Add a user prompt message to the conversation immediately.
     * This is used to show the user's message right away before the AI response begins.
     * 
     * @param chatMessageContext The chat message context with the user prompt
     */
    public void addUserPromptMessage(@NotNull ChatMessageContext chatMessageContext) {
        webViewController.addUserPromptMessage(chatMessageContext);
        scrollToBottom();
    }
    
    /**
     * Updates a message that was previously added as a user prompt with the full AI response.
     * This is used specifically for non-streaming responses.
     *
     * @param chatMessageContext The chat message context with the complete AI response
     */
    public void updateUserPromptWithResponse(@NotNull ChatMessageContext chatMessageContext) {
        webViewController.updateAiMessageContent(chatMessageContext);
        scrollToBottom();
    }
}
