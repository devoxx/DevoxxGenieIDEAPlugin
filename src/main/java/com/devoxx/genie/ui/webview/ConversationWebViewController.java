package com.devoxx.genie.ui.webview;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.util.ThemeChangeNotifier;
import com.devoxx.genie.ui.webview.handler.*;
import com.devoxx.genie.ui.webview.template.ConversationTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for managing a single WebView that can display entire conversations.
 * This class handles the interaction between the Java code and the WebView, appending
 * new content to the conversation without creating new WebView instances.
 */
@Slf4j
public class ConversationWebViewController implements ThemeChangeNotifier, MCPLoggingMessage {
    private static final Logger LOG = Logger.getInstance(ConversationWebViewController.class);

    @Getter
    private final JBCefBrowser browser;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    // Specialized handlers
    private final WebViewJavaScriptExecutor jsExecutor;
    private final WebViewThemeManager themeManager;
    private final WebViewMessageRenderer messageRenderer;
    private final WebViewAIMessageUpdater aiMessageUpdater;
    private final WebViewFileReferenceManager fileReferenceManager;
    private final WebViewMCPLogHandler mcpLogHandler;
    private final WebViewBrowserInitializer browserInitializer;

    /**
     * Creates a new ConversationWebViewController with a fresh browser.
     */
    public ConversationWebViewController() {
        WebServer webServer = WebServer.getInstance();

        // Ensure web server is running
        if (!webServer.isRunning()) {
            webServer.start();
        }

        // Create initial HTML content using the template
        ConversationTemplate template = new ConversationTemplate(webServer);
        String htmlContent = template.generate();

        // Register content with the web server to get a URL
        String resourceId = webServer.addDynamicResource(htmlContent);
        String resourceUrl = webServer.getResourceUrl(resourceId);

        LOG.info("Loading ConversationWebView content from: " + resourceUrl);

        // Create browser and load content
        browser = WebViewFactory.createBrowser(resourceUrl);

        // Set minimum size to ensure visibility
        browser.getComponent().setMinimumSize(new Dimension(600, 400));
        browser.getComponent().setPreferredSize(new Dimension(800, 600));

        // Initialize specialized handlers
        jsExecutor = new WebViewJavaScriptExecutor(browser);
        messageRenderer = new WebViewMessageRenderer(webServer, jsExecutor, initialized);
        aiMessageUpdater = new WebViewAIMessageUpdater(jsExecutor, initialized);
        fileReferenceManager = new WebViewFileReferenceManager(jsExecutor);
        mcpLogHandler = new WebViewMCPLogHandler(jsExecutor);
        browserInitializer = new WebViewBrowserInitializer(initialized, jsExecutor);
        themeManager = new WebViewThemeManager(browser, webServer, jsExecutor, this::showWelcomeContent);

        // Setup JavaScript bridge to handle file opening
        setupJavaScriptBridge();
        
        // Setup file opening polling mechanism
        setupFileOpeningPolling();

        // Add load handler to detect when page is fully loaded
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                jsExecutor.setLoaded(true);
                initialized.set(true);
                LOG.info("ConversationWebView loaded with status: " + httpStatusCode);
            }
        }, browser.getCefBrowser());
    }

    /**
     * Setup JavaScript bridge to handle file opening.
     */
    private void setupJavaScriptBridge() {
        // Use a custom handler in JS to communicate with Java
        browser.getCefBrowser().executeJavaScript(
                "window.openFileFromJava = function(path) {" +
                        "  console.log('Opening file: ' + path);" +
                        "  if (window.java_fileOpened) {" +
                        "    window.java_fileOpened(path);" +
                        "  }" +
                        "}",
                browser.getCefBrowser().getURL(), 0
        );

        // Set up a handler to open files when clicked and override the fileOpened function
        jsExecutor.executeJavaScript(
                "window.java_fileOpened = function(filePath) {" +
                        "  console.log('Request to open file in IDE: ' + filePath);" +
                        "  // Set the file path in a global variable that our polling will detect" +
                        "  window.fileToOpen = filePath;" +
                        "  // Also store the last found path to make it easier to capture" +
                        "  window.lastFoundPath = filePath;" +
                        "};"
        );

        // Define the openFile function to handle file opening when a file is clicked
        browser.getCefBrowser().executeJavaScript(
                "window.openFile = function(elementId) { " +
                        "  const element = document.getElementById(elementId); " +
                        "  if (element && element.dataset.filePath) { " +
                        "    console.log('Request to open file: ' + element.dataset.filePath);" +
                        "    // Call our openFileFromJava function to handle file opening" +
                        "    openFileFromJava(element.dataset.filePath);" +
                        "  }" +
                        "};",
                browser.getCefBrowser().getURL(), 0
        );
    }

    /**
     * Setup polling mechanism to check for file open requests.
     */
    private void setupFileOpeningPolling() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while (true) {
                try {
                    Thread.sleep(100); // Check every 100ms
                    // Create a mechanism to store the file path in a global variable
                    // that we can poll for
                    String checkJs =
                            "var path = null;" +
                                    "if (window.fileToOpen) {" +
                                    "  path = window.fileToOpen;" +
                                    "  window.fileToOpen = null;" +
                                    "}" +
                                    "if (path) { console.log('Found file to open: ' + path); }" +
                                    "return path;";

                    // Execute the JavaScript to check for a file to open and capture the result as a string
                    browser.getCefBrowser().executeJavaScript(
                            "(() => { " + checkJs + " })();",
                            browser.getCefBrowser().getURL(),
                            0
                    );

                    // After executing, check if there's a window.lastFoundPath that might have been set
                    browser.getCefBrowser().executeJavaScript(
                            "if (window.lastFoundPath) { " +
                                    "  const path = window.lastFoundPath; " +
                                    "  window.lastFoundPath = null; " +
                                    "  console.log('Opening file from path variable: ' + path); " +
                                    "  window.java_fileOpened(path); " +
                                    "}",
                            browser.getCefBrowser().getURL(),
                            0
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.error("Interrupted while polling for file open requests", e);
                    break;
                }
            }
        });
    }

    /**
     * Check if the browser is fully initialized and ready for use
     */
    public boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * Ensures the browser is initialized before executing a callback.
     */
    public void ensureBrowserInitialized(Runnable callback) {
        browserInitializer.ensureBrowserInitialized(callback);
    }

    /**
     * Load welcome content in the conversation view.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public void loadWelcomeContent(ResourceBundle resourceBundle) {
        messageRenderer.loadWelcomeContent(resourceBundle);
    }
    
    /**
     * Display welcome content in the WebView.
     * 
     * @param resourceBundle The resource bundle for i18n
     */
    private void showWelcomeContent(ResourceBundle resourceBundle) {
        messageRenderer.loadWelcomeContent(resourceBundle);
    }

    /**
     * Add a chat message to the conversation view.
     *
     * @param chatMessageContext The chat message context
     */
    public void addChatMessage(ChatMessageContext chatMessageContext) {
        messageRenderer.addChatMessage(chatMessageContext);
    }
    
    /**
     * Updates just the AI response part of an existing message in the conversation view.
     *
     * @param chatMessageContext The chat message context
     */
    public void updateAiMessageContent(ChatMessageContext chatMessageContext) {
        aiMessageUpdater.updateAiMessageContent(chatMessageContext);
    }
    
    /**
     * Execute JavaScript in the browser.
     *
     * @param script The JavaScript to execute
     */
    public void executeJavaScript(String script) {
        jsExecutor.executeJavaScript(script);
    }
    
    /**
     * Clear the conversation content.
     */
    public void clearConversation() {
        jsExecutor.executeJavaScript("document.getElementById('conversation-container').innerHTML = '';");
    }
    
    /**
     * Update the custom prompt commands in the welcome panel.
     */
    public void updateCustomPrompts(ResourceBundle resourceBundle) {
        showWelcomeContent(resourceBundle);
    }
    
    /**
     * Called when the IDE theme changes.
     * Delegated to the theme manager.
     *
     * @param isDarkTheme true if the new theme is dark, false if it's light
     */
    @Override
    public void themeChanged(boolean isDarkTheme) {
        themeManager.themeChanged(isDarkTheme);
    }
    
    /**
     * Adds just the user message to the conversation view without waiting for the AI response.
     *
     * @param chatMessageContext The chat message context containing the user prompt
     */
    public void addUserPromptMessage(ChatMessageContext chatMessageContext) {
        aiMessageUpdater.addUserPromptMessage(chatMessageContext);
        // Set the active message ID for MCP logging
        mcpLogHandler.setActiveMessageId(chatMessageContext.getId());
    }

    /**
     * Implements the MCPLoggingMessage interface to receive MCP log messages.
     * Delegated to the MCP log handler.
     *
     * @param message The MCP message received
     */
    @Override
    public void onMCPLoggingMessage(MCPMessage message) {
        log.info("Received MCP logging message: {}", message.getContent());
        mcpLogHandler.onMCPLoggingMessage(message);
    }

    /**
     * Add file references to the conversation.
     *
     * @param chatMessageContext The chat message context
     * @param files The list of files to reference
     */
    public void addFileReferences(ChatMessageContext chatMessageContext, List<VirtualFile> files) {
        fileReferenceManager.addFileReferences(chatMessageContext, files);
    }
}