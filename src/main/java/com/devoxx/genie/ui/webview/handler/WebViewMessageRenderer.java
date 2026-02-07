package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.welcome.WelcomeContent;
import com.devoxx.genie.service.welcome.WelcomeContentService;
import com.devoxx.genie.ui.webview.WebServer;
import com.devoxx.genie.ui.webview.template.ChatMessageTemplate;
import com.devoxx.genie.ui.webview.template.WelcomeTemplate;
import com.devoxx.genie.util.ThreadUtils;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles rendering of messages in the WebView.
 * This class is responsible for adding and updating chat messages in the conversation.
 */
@Slf4j
public class WebViewMessageRenderer {

    private final WebServer webServer;
    private final WebViewJavaScriptExecutor jsExecutor;
    private final AtomicBoolean initialized;
    private final AtomicBoolean welcomeLoadCancelled = new AtomicBoolean(false);
    
    public WebViewMessageRenderer(WebServer webServer, WebViewJavaScriptExecutor jsExecutor, AtomicBoolean initialized) {
        this.webServer = webServer;
        this.jsExecutor = jsExecutor;
        this.initialized = initialized;
    }
    
    /**
     * Load welcome content in the conversation view.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public void loadWelcomeContent(ResourceBundle resourceBundle) {
        welcomeLoadCancelled.set(false);
        if (!jsExecutor.isLoaded()) {
            // Wait for browser to load before injecting content
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                while (!initialized.get() && !welcomeLoadCancelled.get()) {
                    ThreadUtils.sleep(100);
                }
                if (!welcomeLoadCancelled.get()) {
                    showWelcomeContent(resourceBundle);
                } else {
                    log.debug("Welcome content load cancelled before browser initialized");
                }
            });
        } else {
            showWelcomeContent(resourceBundle);
        }
    }
    
    /**
     * Display welcome content in the WebView.
     * 
     * @param resourceBundle The resource bundle for i18n
     */
    private void showWelcomeContent(ResourceBundle resourceBundle) {
        if (welcomeLoadCancelled.get()) {
            log.debug("Welcome content load cancelled, skipping injection");
            return;
        }

        // Use the WelcomeTemplate to generate HTML, with remote content if available
        WelcomeContent remoteContent = WelcomeContentService.getInstance().getWelcomeContent();
        WelcomeTemplate welcomeTemplate = new WelcomeTemplate(webServer, resourceBundle, remoteContent);
        String welcomeContent = welcomeTemplate.generate();

        // Only inject welcome content if no chat messages are already present (defense-in-depth).
        // Content is from trusted internal WelcomeTemplate, not user input.
        jsExecutor.executeJavaScript(
                "(function() {" +
                "  var container = document.getElementById('conversation-container');" +
                "  if (container && container.querySelectorAll('.message-pair').length === 0) {" +
                "    container.innerHTML = `" + jsExecutor.escapeJS(welcomeContent) + "`;" +
                "    if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }" +
                "    window.scrollTo(0, 0);" +
                "  }" +
                "})();");
    }

    /**
     * Cancel any pending welcome content load.
     * This prevents a deferred welcome load from overwriting chat messages.
     */
    public void cancelPendingWelcomeLoad() {
        welcomeLoadCancelled.set(true);
        log.debug("Pending welcome content load cancelled");
    }
    
    /**
     * Add a chat message to the conversation view.
     *
     * @param chatMessageContext The chat message context
     */
    public void addChatMessage(ChatMessageContext chatMessageContext) {
        // Use the ChatMessageTemplate to generate HTML
        ChatMessageTemplate messageTemplate = new ChatMessageTemplate(webServer, chatMessageContext);
        String messageHtml = messageTemplate.generate();
        
        // Log for debugging
        log.info("Adding chat message to conversation view: " + chatMessageContext.getId());
        
        if (!jsExecutor.isLoaded()) {
            log.warn("Browser not loaded yet, waiting before adding message");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                while (!initialized.get()) {
                    ThreadUtils.sleep(100);
                }
                doAddChatMessage(messageHtml);
            });
        } else {
            doAddChatMessage(messageHtml);
        }
    }
    
    /**
     * Performs the actual operation of adding a chat message to the conversation.
     * Insert the message HTML at the end of the conversation container.
     * 
     * @param messageHtml The HTML of the message to add
     */
    private void doAddChatMessage(@NotNull String messageHtml) {
        // Extract the message ID from the HTML
        // The message ID is in the format: <div class=\"message-pair\" id=\"SOME_ID\">
        String messageId = null;
        int idStartIndex = messageHtml.indexOf("id=\"") + 4;
        if (idStartIndex > 4) {
            int idEndIndex = messageHtml.indexOf("\"", idStartIndex);
            if (idEndIndex > idStartIndex) {
                messageId = messageHtml.substring(idStartIndex, idEndIndex);
            }
        }

        // Create JavaScript that checks if the message already exists
        String js;
        if (messageId != null) {
            // If we have an ID, check if it exists before adding
            js = "try {" +
                 "  if (!document.getElementById('" + jsExecutor.escapeJS(messageId) + "')) {" +
                 "    const container = document.getElementById('conversation-container');" +
                 "    const tempDiv = document.createElement('div');" +
                 "    tempDiv.innerHTML = `" + jsExecutor.escapeJS(messageHtml) + "`;" +
                 "    while (tempDiv.firstChild) {" +
                 "      container.appendChild(tempDiv.firstChild);" +
                 "    }" +
                 "  } else {" +
                 "    console.log('Message with ID " + jsExecutor.escapeJS(messageId) + " already exists, skipping addition');" +
                 "  }" +
                 "  window.scrollTo(0, document.body.scrollHeight);" +
                 "  if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }" +
                 "} catch (error) {" +
                 "  console.error('Error adding chat message:', error);" +
                 "}";
        } else {
            // If we couldn't extract an ID, fall back to the original behavior
            js = "try {" +
                 "  const container = document.getElementById('conversation-container');" +
                 "  const tempDiv = document.createElement('div');" +
                 "  tempDiv.innerHTML = `" + jsExecutor.escapeJS(messageHtml) + "`;" +
                 "  while (tempDiv.firstChild) {" +
                 "    container.appendChild(tempDiv.firstChild);" +
                 "  }" +
                 "  window.scrollTo(0, document.body.scrollHeight);" +
                 "  if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }" +
                 "} catch (error) {" +
                 "  console.error('Error adding chat message:', error);" +
                 "}";
        }

        log.info("Executing JavaScript to add message" + (messageId != null ? " with ID: " + messageId : ""));
        jsExecutor.executeJavaScript(js);
    }
}