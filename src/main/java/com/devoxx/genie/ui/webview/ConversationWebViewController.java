package com.devoxx.genie.ui.webview;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.util.ThemeChangeNotifier;
import com.devoxx.genie.ui.webview.handler.*;
import com.devoxx.genie.ui.webview.template.ConversationTemplate;
import com.devoxx.genie.util.ThreadUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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

    @Getter
    private JBCefBrowser browser;
    private JComponent fallbackComponent;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    // Specialized handlers
    private WebViewJavaScriptExecutor jsExecutor;
    private final WebViewThemeManager themeManager;
    private final WebViewMessageRenderer messageRenderer;
    private final WebViewAIMessageUpdater aiMessageUpdater;
    private final WebViewFileReferenceManager fileReferenceManager;
    private final WebViewMCPLogHandler mcpLogHandler;
    private final WebViewBrowserInitializer browserInitializer;
    private final WebViewSleepWakeRecoveryHandler sleepWakeRecoveryHandler;
    private final WebViewExternalLinkHandler externalLinkHandler;

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

        log.info("Loading ConversationWebView content from: {}", resourceUrl);

        boolean jcefAvailable = JCEFChecker.isJCEFAvailable();
        
        if (jcefAvailable) {
            try {
                // Create browser and load content
                browser = new JBCefBrowser();
                
                // Set minimum size to ensure visibility
                browser.getComponent().setMinimumSize(new Dimension(600, 400));
                browser.getComponent().setPreferredSize(new Dimension(800, 600));
                
                // Initialize specialized handlers
                jsExecutor = new WebViewJavaScriptExecutor(browser);
                
                // Load the content
                browser.loadURL(resourceUrl);
                
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
                        log.info("ConversationWebView loaded with status: " + httpStatusCode);
                        
                        // Initialize external link handling after page load
                        if (externalLinkHandler != null) {
                            externalLinkHandler.injectLinkHandlingScript();
                        }
                    }
                }, browser.getCefBrowser());
            } catch (Exception e) {
                log.error("Error initializing JCEF browser despite JCEF being available: {}", e.getMessage());
                // Set jcefAvailable to false to use fallback mode
                jcefAvailable = false;
            }
        }
        
        // If JCEF is not available or browser initialization failed, create dummy objects
        if (!jcefAvailable) {
            // Create a dummy browser for reference, but we'll use fallback component
            try {
                // Use a no-op mock implementation instead of actual JBCefBrowser
                fallbackComponent = WebViewFactory.createFallbackComponent(
                    "DevoxxGenie needs JCEF support to display its web-based UI components. " +
                    "This feature is not available in your current IDE environment.\n\n" +
                    "Follow the instructions below to enable JCEF support and get the full experience. " +
                    "This is applicable for Android Studio and any JetBrains IDE where JCEF is not enabled by default.");
                    
                // Create a no-op executor
                jsExecutor = new WebViewJavaScriptExecutor(null);
                log.warn("JCEF is not available, created fallback component");
            } catch (Exception e) {
                log.error("Error creating fallback component: {}", e.getMessage());
                // Create a simple JPanel with an error message if everything else fails
                fallbackComponent = new JPanel(new BorderLayout());
                JLabel errorLabel = new JLabel("<html><body style='padding:10px;'>JCEF is not available.<br>Please enable JCEF in your IDE settings.</body></html>");
                errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                fallbackComponent.add(errorLabel, BorderLayout.CENTER);
                
                // Create a no-op executor
                jsExecutor = new WebViewJavaScriptExecutor(null);
            }
        }

        // These handlers should work in both modes (JCEF and fallback)
        messageRenderer = new WebViewMessageRenderer(webServer, jsExecutor, initialized);
        aiMessageUpdater = new WebViewAIMessageUpdater(jsExecutor, initialized);
        fileReferenceManager = new WebViewFileReferenceManager(jsExecutor);
        mcpLogHandler = new WebViewMCPLogHandler(jsExecutor);
        browserInitializer = new WebViewBrowserInitializer(initialized, jsExecutor);
        themeManager = new WebViewThemeManager(browser, webServer, jsExecutor, this::showWelcomeContent);
        sleepWakeRecoveryHandler = new WebViewSleepWakeRecoveryHandler(browser);
        
        // Initialize external link handler
        externalLinkHandler = new WebViewExternalLinkHandler(webServer.getServerUrl(), jsExecutor);
        
        // Setup external link handling if JCEF is available
        if (jcefAvailable && browser != null) {
            setupExternalLinkHandling();
            log.debug("External link handler initialized for webview");
        } else {
            log.warn("External link handler not initialized - JCEF not available or browser is null");
        }
    }

    /**
     * Setup JavaScript bridge to handle file opening.
     * Should only be called if JCEF is available.
     */
    private void setupJavaScriptBridge() {
        // Skip if JCEF is not available
        if (!JCEFChecker.isJCEFAvailable()) {
            log.warn("Cannot setup JavaScript bridge - JCEF is not available");
            return;
        }
        
        try {
            // Use a custom handler in JS to communicate with Java
            browser.getCefBrowser().executeJavaScript(
                    "window.openFileFromJava = function(path) {" +
                            "  if (window.java_fileOpened) {" +
                            "    window.java_fileOpened(path);" +
                            "  }" +
                            "}",
                    browser.getCefBrowser().getURL(), 0
            );
    
            // Set up a handler to open files when clicked and override the fileOpened function
            jsExecutor.executeJavaScript(
                    "window.java_fileOpened = function(filePath) {" +
                            "  window.fileToOpen = filePath;" +
                            "  window.lastFoundPath = filePath;" +
                            "};"
            );
    
            // Define the openFile function to handle file opening when a file is clicked
            browser.getCefBrowser().executeJavaScript(
                    "window.openFile = function(elementId) { " +
                            "  const element = document.getElementById(elementId); " +
                            "  if (element && element.dataset.filePath) { " +
                            "    openFileFromJava(element.dataset.filePath);" +
                            "  }" +
                            "};",
                    browser.getCefBrowser().getURL(), 0
            );
        } catch (Exception e) {
            log.error("Error setting up JavaScript bridge: {}", e.getMessage());
        }
    }

    /**
     * Setup polling mechanism to check for file open requests.
     * Should only be called if JCEF is available.
     */
    private void setupFileOpeningPolling() {
        // Skip if JCEF is not available
        if (!JCEFChecker.isJCEFAvailable()) {
            log.warn("Cannot setup file opening polling - JCEF is not available");
            return;
        }
        
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while (true) {
                try {
                    ThreadUtils.sleep(50);
                    
                    // Check if JCEF is still available - exit thread if not
                    if (!JCEFChecker.isJCEFAvailable()) {
                        log.warn("JCEF became unavailable, stopping file opening polling");
                        break;
                    }

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

                    try {
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
                                        "  window.java_fileOpened(path); " +
                                        "}",
                                browser.getCefBrowser().getURL(),
                                0
                        );
                    } catch (Exception e) {
                        log.error("Error executing JavaScript in file polling: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while polling for file open requests", e);
                    break;
                }
            }
        });
    }
    
    /**
     * Setup external link handling.
     * Should only be called if JCEF is available.
     */
    private void setupExternalLinkHandling() {
        // Skip if JCEF is not available
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            log.warn("Cannot setup external link handling - JCEF not available or browser is null");
            return;
        }
        
        try {
            // Add the request handler to intercept navigation requests
            browser.getJBCefClient().addRequestHandler(externalLinkHandler, browser.getCefBrowser());
            
            // Setup the JavaScript bridge for external link handling (visual indicators only)
            externalLinkHandler.setupExternalLinkBridge(browser);
            
            log.info("External link handling setup completed");
        } catch (Exception e) {
            log.error("Error setting up external link handling", e);
        }
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
     * Force refresh the webview when starting a new conversation.
     * This helps ensure a clean state and can recover from any rendering issues.
     */
    public void refreshForNewConversation() {
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            log.debug("Cannot refresh webview - JCEF not available or browser is null");
            return;
        }
        
        log.info("Refreshing webview for new conversation");
        
        // Don't clear content immediately - let the normal flow handle that
        // Just ensure the webview is in a good state for rendering
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Strategy 1: Light refresh - just force component revalidation
                if (attemptLightRefresh()) {
                    log.debug("Light refresh completed for new conversation");
                    return;
                }
                
                // Strategy 2: If light refresh fails, try component refresh only
                if (attemptComponentRefresh()) {
                    log.debug("Component refresh completed for new conversation");
                    return;
                }
                
                // Strategy 3: Only use recovery handler as last resort
                log.warn("Component refresh failed, using recovery handler");
                if (sleepWakeRecoveryHandler != null) {
                    sleepWakeRecoveryHandler.triggerRecovery();
                }
                
            } catch (Exception e) {
                log.error("Error during webview refresh for new conversation", e);
            }
        });
    }
    
    /**
     * Attempt a light refresh by just ensuring component validity.
     */
    private boolean attemptLightRefresh() {
        try {
            JComponent component = browser.getComponent();
            if (component.isDisplayable()) {
                // Just ensure the component is properly validated
                component.revalidate();
                component.repaint();
                return true;
            }
        } catch (Exception e) {
            log.debug("Light refresh failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Attempt component refresh without touching browser content.
     */
    private boolean attemptComponentRefresh() {
        try {
            JComponent component = browser.getComponent();
            if (component != null) {
                // Force component hierarchy refresh
                component.invalidate();
                component.revalidate();
                component.repaint();
                
                // Also refresh parent if needed
                Container parent = component.getParent();
                if (parent != null) {
                    parent.revalidate();
                    parent.repaint();
                }
                return true;
            }
        } catch (Exception e) {
            log.debug("Component refresh failed: {}", e.getMessage());
        }
        return false;
    }    
    /**
     * Attempt a soft refresh by reloading the current page.
     */
    private boolean attemptSoftRefresh() {
        try {
            if (browser.getCefBrowser() != null) {
                String currentUrl = browser.getCefBrowser().getURL();
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    browser.getCefBrowser().reload();
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Soft refresh failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Attempt a hard refresh by forcing component revalidation and browser reload.
     */
    private boolean attemptHardRefresh() {
        try {
            // Force UI component refresh
            JComponent component = browser.getComponent();
            if (component != null) {
                component.invalidate();
                component.revalidate();
                component.repaint();
                
                // Also refresh parent containers
                Container parent = component.getParent();
                if (parent != null) {
                    parent.invalidate();
                    parent.revalidate();
                    parent.repaint();
                }
            }
            
            // Then reload the browser
            if (browser.getCefBrowser() != null) {
                String currentUrl = browser.getCefBrowser().getURL();
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    browser.getCefBrowser().reload();
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Hard refresh failed: {}", e.getMessage());
        }
        return false;
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
    public void onMCPLoggingMessage(@NotNull MCPMessage message) {
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
    
    /**
     * Gets the component to display, either the browser component or a fallback text component
     * if JCEF is not available.
     * 
     * @return the component to display
     */
    public JComponent getComponent() {
        if (!JCEFChecker.isJCEFAvailable()) {
            if (fallbackComponent == null) {
                fallbackComponent = WebViewFactory.createFallbackComponent(
                        "DevoxxGenie needs JCEF support to display its web-based UI components. " +
                        "This feature is not available in your current IDE environment.\n\n" +
                        "Follow the instructions below to enable JCEF support and get the full experience. " +
                        "This is applicable for Android Studio and any JetBrains IDE where JCEF is not enabled by default.");
            }
            return fallbackComponent;
        }
        return browser.getComponent();
    }
    
    /**
     * Check if there are black screen issues with the webview.
     * This checks various indicators to determine if the webview is showing a black screen.
     * 
     * @return true if black screen issues are detected, false otherwise
     */
    public boolean hasBlackScreenIssues() {
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            return false; // Can't have black screen if JCEF isn't available
        }
        
        try {
            JComponent component = browser.getComponent();
            if (component == null) {
                return true; // Null component is definitely an issue
            }
            
            // Check if component is showing but has suspicious properties
            if (component.isShowing()) {
                // Check for invalid size
                if (component.getWidth() <= 0 || component.getHeight() <= 0) {
                    log.debug("Black screen suspected: component showing but has invalid size {}x{}", 
                             component.getWidth(), component.getHeight());
                    return true;
                }
                
                // Check for black background
                Color background = component.getBackground();
                if (background != null && background.equals(Color.BLACK)) {
                    log.debug("Black screen suspected: component has black background");
                    return true;
                }
            }
            
            // Check rendering detector if available
            if (sleepWakeRecoveryHandler != null) {
                // Access the rendering detector through the recovery handler
                // We can check the detector's hasIssues method indirectly
                try {
                    // Use reflection to access the renderingDetector field
                    java.lang.reflect.Field field = sleepWakeRecoveryHandler.getClass().getDeclaredField("renderingDetector");
                    field.setAccessible(true);
                    Object renderingDetector = field.get(sleepWakeRecoveryHandler);
                    
                    if (renderingDetector != null) {
                        java.lang.reflect.Method hasIssuesMethod = renderingDetector.getClass().getMethod("hasIssues");
                        Boolean hasIssues = (Boolean) hasIssuesMethod.invoke(renderingDetector);
                        if (hasIssues != null && hasIssues) {
                            log.debug("Black screen suspected: rendering detector reports issues");
                            return true;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not check rendering detector: {}", e.getMessage());
                    // Continue with other checks
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error checking for black screen issues", e);
            return false; // If we can't check, assume no issues
        }
    }
    
    /**
     * Manually trigger recovery from sleep/wake issues.
     * This can be called when the webview appears to be in a corrupted state.
     */
    public void triggerRecovery() {
        if (sleepWakeRecoveryHandler != null) {
            sleepWakeRecoveryHandler.triggerRecovery();
        }
    }
    
    /**
     * Dispose of resources when the controller is no longer needed.
     */
    public void dispose() {
        if (sleepWakeRecoveryHandler != null) {
            sleepWakeRecoveryHandler.dispose();
        }
    }}