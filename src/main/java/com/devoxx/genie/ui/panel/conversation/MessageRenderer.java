package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.listener.FileReferencesListener;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Handles rendering of messages in the conversation UI.
 * This class encapsulates all interactions with the web view controller for message rendering.
 */
@Slf4j
public class MessageRenderer implements FileReferencesListener {

    private final ConversationWebViewController webViewController;

    /**
     * Creates a new message renderer.
     *
     * @param project The active project
     * @param webViewController The web view controller to use for rendering
     */
    public MessageRenderer(@NotNull Project project,
                           ConversationWebViewController webViewController) {
        this.webViewController = webViewController;
        
        // Subscribe to file references topic
        MessageBusConnection msgBusConnection = project.getMessageBus().connect();
        msgBusConnection.subscribe(AppTopics.FILE_REFERENCES_TOPIC, this);
    }

    /**
     * Add a complete chat message (user message + AI response) to the conversation.
     *
     * @param chatMessageContext The chat message context containing both user and AI messages
     */
    public void addChatMessage(ChatMessageContext chatMessageContext) {
        webViewController.addChatMessage(chatMessageContext);
        scrollToBottom();
    }

    /**
     * Add a user prompt message to the conversation immediately.
     * This is used to show the user's message right away before the AI response begins.
     * 
     * @param chatMessageContext The chat message context with the user prompt
     */
    public void addUserPromptMessage(@NotNull ChatMessageContext chatMessageContext) {
        webViewController.addUserPromptMessage(chatMessageContext);
        // Don't automatically scroll to bottom for first messages in a new conversation
        // This is handled specifically for the first message to maintain header spacing
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

    /**
     * Show the welcome content in the conversation view.
     */
    public void showWelcome(ResourceBundle resourceBundle) {
        webViewController.loadWelcomeContent(resourceBundle);
    }

    /**
     * Clear the conversation content.
     */
    public void clear() {
        webViewController.clearConversation();
    }

    /**
     * Clear the conversation content without showing welcome message.
     * Used when restoring conversation history.
     */
    public void clearWithoutWelcome() {
        // Cancel any pending deferred welcome load to prevent it from overwriting chat messages
        webViewController.cancelPendingWelcomeLoad();
        // Set the restoration flag to prevent welcome content during theme changes
        webViewController.setRestoringConversation(true);
        webViewController.clearConversation();
    }

    /**
     * Clear the restoration flag after conversation restoration is complete.
     * This allows normal welcome content loading to resume.
     */
    public void clearRestorationFlag() {
        webViewController.setRestoringConversation(false);
        log.debug("Cleared conversation restoration flag");
    }
    
    /**
     * Set whether a conversation restoration is in progress.
     * This prevents welcome content from being shown during restoration.
     * 
     * @param inProgress true if restoration is in progress, false otherwise
     */
    public void setRestorationInProgress(boolean inProgress) {
        webViewController.setRestoringConversation(inProgress);
        log.debug("Set conversation restoration in progress: {}", inProgress);
    }

    /**
     * Update custom prompts in the welcome screen.
     */
    public void updateCustomPrompts(ResourceBundle resourceBundle) {
        webViewController.updateCustomPrompts(resourceBundle);
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
     * Scrolls the conversation view to the top.
     * This is used after restoring a conversation.
     */
    public void scrollToTop() {
        // Small delay before scrolling to ensure all message rendering is complete
        ApplicationManager.getApplication().invokeLater(() -> {
            ThreadUtils.sleep(300);
            webViewController.executeJavaScript("window.scrollTo(0, 0);");
            log.debug("Scrolled conversation to top");
        });
    }

    /**
     * Adds a complete chat message (user + AI) in a thread-safe way.
     * Used primarily during conversation restoration.
     */
    public void addCompleteChatMessage(ChatMessageContext context) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                webViewController.addChatMessage(context);
                log.debug("Successfully added message pair with ID: {}", context.getId());
            } catch (Exception e) {
                log.error("Error adding chat message: {}", e.getMessage(), e);
            }
        });
        
        // Small delay to ensure proper rendering between messages
        ThreadUtils.sleep(75);
    }
    
    /**
     * Adds just a user message without AI response.
     * Used primarily during conversation restoration.
     */
    public void addUserMessageOnly(ChatMessageContext context) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                webViewController.addUserPromptMessage(context);
                log.debug("Added user-only message with ID: {}", context.getId());
            } catch (Exception e) {
                log.error("Error adding user message: {}", e.getMessage(), e);
            }
        });
        
        // Small delay to ensure proper rendering
        ThreadUtils.sleep(50);
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
     * Check if the web view controller is initialized
     */
    public boolean isInitialized() {
        return webViewController.isInitialized();
    }
    
    /**
     * Check if there are black screen issues with the webview.
     * This delegates to the webview controller to check for rendering problems.
     * 
     * @return true if black screen issues are detected, false otherwise
     */
    public boolean hasBlackScreenIssues() {
        try {
            return webViewController.hasBlackScreenIssues();
        } catch (Exception e) {
            log.error("Error checking for black screen issues", e);
            return false;
        }
    }
    
    /**
     * Ensure the browser is initialized before executing a runnable
     */
    public void ensureBrowserInitialized(Runnable runnable) {
        webViewController.ensureBrowserInitialized(runnable);
    }
}
