package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatService;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.listener.ConversationStarter;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.listener.FileReferencesListener;
import com.devoxx.genie.ui.panel.conversationhistory.ConversationHistoryPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ResourceBundle;

/**
 * A panel containing a single WebView that displays the entire conversation.
 * This class has been refactored to delegate responsibilities to specialized classes.
 */
@Slf4j
public class ConversationPanel
        extends JBPanel<ConversationPanel>
        implements CustomPromptChangeListener, ConversationSelectionListener, ConversationEventListener, ConversationStarter, FileReferencesListener {

    private final MessageRenderer messageRenderer;
    private final ConversationManager conversationManager;
    private final ConversationUIController uiController;
    private final MessageBusConnection messageBusConnection;

    public final ConversationWebViewController webViewController;

    /**
     * Creates a new conversation panel.
     *
     * @param project The active project
     * @param resourceBundle The resource bundle for i18n
     */
    public ConversationPanel(Project project, ResourceBundle resourceBundle) {
        super(new BorderLayout());

        webViewController = new ConversationWebViewController();
        
        messageRenderer = new MessageRenderer(project, webViewController);
        
        ConversationHistoryPanel historyPanel = new ConversationHistoryPanel(project);
        ConversationHistoryManager historyManager = new ConversationHistoryManager(project, historyPanel, messageRenderer);
        
        uiController = new ConversationUIController(
            project, 
            resourceBundle, 
            messageRenderer, 
            null, // We'll set this after creating conversationManager 
                historyManager
        );
        
        ChatService chatService = new ChatService(project);
        conversationManager = new ConversationManager(
            project, 
            chatService,
                historyManager,
            messageRenderer, 
            uiController.getConversationLabel(),
            this::refreshWebViewForNewConversation
        );
        
        // Set the conversation manager in the chat service for conversation tracking
        chatService.setConversationManager(conversationManager);        
        // Now set the conversationManager in the UI controller via reflection
        try {
            Field field = uiController.getClass().getDeclaredField("conversationManager");
            field.setAccessible(true);
            field.set(uiController, conversationManager);
        } catch (Exception e) {
            log.error("Could not set conversationManager in UIController", e);
        }

        // Setup UI
        add(uiController.createButtonPanel(), BorderLayout.NORTH);

        // Set component layout and sizing
        JComponent displayComponent = webViewController.getComponent();
        displayComponent.setOpaque(true);
        Color editorBg = UIUtil.getPanelBackground();
        displayComponent.setBackground(editorBg);
        add(displayComponent, BorderLayout.CENTER);

        // Set sizes for the panel to ensure proper display
        setMinimumSize(new Dimension(400, 300));
        setPreferredSize(new Dimension(800, 600));

        // Ensure the component is visible
        setOpaque(true);
        setBackground(editorBg);
        setVisible(true);
        
        // Subscribe to the relevant topics
        messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(AppTopics.FILE_REFERENCES_TOPIC, this);
        messageBusConnection.subscribe(AppTopics.CONVERSATION_SELECTION_TOPIC, this);
        messageBusConnection.subscribe(AppTopics.MCP_LOGGING_MSG, webViewController);
    }

    /**
     * Show the welcome content.
     */
    public void showWelcome() {
        uiController.showWelcome();
    }

    /**
     * Add a chat message to the conversation.
     *
     * @param chatMessageContext The chat message context
     */
    public void addChatMessage(ChatMessageContext chatMessageContext) {
        messageRenderer.addChatMessage(chatMessageContext);
    }

    /**
     * Clear the conversation content.
     */
    public void clear() {
        uiController.clear();
    }
    
    /**
     * Clear the conversation content without showing the welcome message.
     * Used when restoring conversation history.
     */
    public void clearWithoutWelcome() {
        uiController.clearWithoutWelcome();
    }

    /**
     * Called when custom prompts change - updates the welcome content if it's visible.
     */
    @Override
    public void onCustomPromptsChanged() {
        uiController.onCustomPromptsChanged();
    }

    /**
     * Called when a new conversation is created.
     */
    @Override
    public void onNewConversation(ChatMessageContext chatMessageContext) {
        conversationManager.onNewConversation(chatMessageContext);
    }

    /**
     * Start a new conversation.
     */
    @Override
    public void startNewConversation() {
        conversationManager.startNewConversation();
    }

    /**
     * Called when a conversation is selected from history.
     */
    @Override
    public void onConversationSelected(@NotNull Conversation conversation) {
        conversationManager.onConversationSelected(conversation);
    }
    
    /**
     * Handle file references being available for a chat message.
     */
    @Override
    public void onFileReferencesAvailable(@NotNull ChatMessageContext chatMessageContext, @NotNull List<VirtualFile> files) {
        messageRenderer.onFileReferencesAvailable(chatMessageContext, files);
    }
    
    /**
     * Scrolls the conversation view to the bottom.
     */
    public void scrollToBottom() {
        messageRenderer.scrollToBottom();
    }
    
    /**
     * Add a user prompt message to the conversation immediately.
     */
    public void addUserPromptMessage(@NotNull ChatMessageContext chatMessageContext) {
        messageRenderer.addUserPromptMessage(chatMessageContext);
    }
    
    /**
     * Updates a message that was previously added as a user prompt with the full AI response.
     */
    public void updateUserPromptWithResponse(@NotNull ChatMessageContext chatMessageContext) {
        messageRenderer.updateUserPromptWithResponse(chatMessageContext);
    }
    
    /**
     * Dispose of resources when the panel is no longer needed.
     * This should be called when the panel is being removed or when the project closes.
     */
    public void dispose() {
        if (messageBusConnection != null) {
            messageBusConnection.disconnect();
        }
        if (webViewController != null) {
            webViewController.dispose();
        }
    }
    
    /**
     * Manually trigger recovery from sleep/wake issues when the webview appears corrupted.
     * This is a convenience method that can be called externally if needed.
     */
    public void triggerWebViewRecovery() {
        if (webViewController != null) {
            webViewController.triggerRecovery();
        }
    }
    
    /**
     * Force refresh the webview for a new conversation.
     * This ensures a clean state and can help recover from any rendering issues.
     */
    public void refreshWebViewForNewConversation() {
        if (webViewController != null) {
            webViewController.refreshForNewConversation();
        }
    }
}