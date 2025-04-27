package com.devoxx.genie.ui.webview;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.util.ThemeChangeNotifier;
import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.template.ChatMessageTemplate;
import com.devoxx.genie.ui.webview.template.ConversationTemplate;
import com.devoxx.genie.ui.webview.template.WelcomeTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.Getter;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import static java.lang.Thread.sleep;

/**
 * Controller for managing a single WebView that can display entire conversations.
 * This class handles the interaction between the Java code and the WebView, appending
 * new content to the conversation without creating new WebView instances.
 */
public class ConversationWebViewController implements ThemeChangeNotifier {
    private static final Logger LOG = Logger.getInstance(ConversationWebViewController.class);

    @Getter
    private final JBCefBrowser browser;
    private final WebServer webServer;
    private boolean isLoaded = false;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Project project;

    /**
     * Creates a new ConversationWebViewController with a fresh browser.
     *
     * @param project The project context
     */
    public ConversationWebViewController(Project project) {
        this.project = project;
        this.webServer = WebServer.getInstance();
        
        // Ensure web server is running
        if (!webServer.isRunning()) {
            webServer.start();
        }
        
        // TODO: Review : Register for theme change notifications via ThemeDetector
        ThemeDetector.addThemeChangeListener(this::themeChanged);
        
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
        
        // Setup JavaScript bridge to handle file opening
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
        executeJavaScript(
            "window.java_fileOpened = function(filePath) {" +
            "  console.log('Request to open file in IDE: ' + filePath);" +
            "  // Set the file path in a global variable that our polling will detect" +
            "  window.fileToOpen = filePath;" +
            "  // Also store the last found path to make it easier to capture" +
            "  window.lastFoundPath = filePath;" +
            "};"
        );
        
        // Setup a polling mechanism to check for file open requests
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
        
        // Add load handler to detect when page is fully loaded
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                isLoaded = true;
                initialized.set(true);
                LOG.info("ConversationWebView loaded with status: " + httpStatusCode);
            }
        }, browser.getCefBrowser());
    }

    /**
     * Load welcome content in the conversation view.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public void loadWelcomeContent(ResourceBundle resourceBundle) {
        if (!isLoaded) {
            // Wait for browser to load before injecting content
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                while (!initialized.get()) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.error("Interrupted while waiting for browser to load", e);
                        return;
                    }
                }
                showWelcomeContent(resourceBundle);
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
        // Use the WelcomeTemplate to generate HTML
        WelcomeTemplate welcomeTemplate = new WelcomeTemplate(webServer, resourceBundle);
        String welcomeContent = welcomeTemplate.generate();
        
        // Execute JavaScript to update the content
        executeJavaScript("document.getElementById('conversation-container').innerHTML = `" + escapeJS(welcomeContent) + "`;");
        executeJavaScript("if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }");
        executeJavaScript("window.scrollTo(0, 0);");
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
        LOG.info("Adding chat message to conversation view: " + chatMessageContext.getId());
        
        if (!isLoaded) {
            LOG.warn("Browser not loaded yet, waiting before adding message");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                while (!initialized.get()) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.error("Interrupted while waiting for browser to load", e);
                        return;
                    }
                }
                doAddChatMessage(messageHtml);
            });
        } else {
            doAddChatMessage(messageHtml);
        }
    }
    
    /**
     * Updates just the AI response part of an existing message in the conversation view.
     * Used for streaming responses to avoid creating multiple message pairs.
     *
     * @param chatMessageContext The chat message context
     */
    public void updateAiMessageContent(@NotNull ChatMessageContext chatMessageContext) {
        if (chatMessageContext.getAiMessage() == null) {
            LOG.warn("No AI message to update for context: " + chatMessageContext.getId());
            return;
        }
        
        if (!isLoaded) {
            LOG.warn("Browser not loaded yet, waiting before updating message");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                while (!initialized.get()) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.error("Interrupted while waiting for browser to load", e);
                        return;
                    }
                }
                doUpdateAiMessageContent(chatMessageContext);
            });
        } else {
            doUpdateAiMessageContent(chatMessageContext);
        }
    }
    
    /**
     * Performs the actual update of just the AI response content.
     * 
     * @param chatMessageContext The chat message context
     */
    private void doUpdateAiMessageContent(@NotNull ChatMessageContext chatMessageContext) {
        String messageId = chatMessageContext.getId();
        
        // Parse and render the markdown content
        Parser markdownParser = Parser.builder().build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
        
        String aiMessageText = chatMessageContext.getAiMessage() == null ? "" : chatMessageContext.getAiMessage().text();
        Node document = markdownParser.parse(aiMessageText);
        
        StringBuilder contentHtml = new StringBuilder();
        
        // Format metadata information
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM ''yy HH:mm");
        String timestamp = dateTime.format(formatter);
        
        String modelName = "Unknown";
        if (chatMessageContext.getLanguageModel() != null) {
            modelName = chatMessageContext.getLanguageModel().getModelName();
        }
        
        // Add metrics data
        StringBuilder metricInfo = new StringBuilder();
        metricInfo.append(String.format(" · ϟ %.2fs", chatMessageContext.getExecutionTimeMs() / 1000.0));
        
        // Add metadata div
        contentHtml.append("<div class=\"metadata-info\">")
                .append(timestamp)
                .append(" · ")
                .append(modelName)
                .append(metricInfo.toString())
                .append("</div>")
                .append("<button class=\"copy-response-button\" onclick=\"copyMessageResponse(this)\">Copy</button>");
        
        // Add content
        Node node = document.getFirstChild();
        while (node != null) {
            if (node instanceof FencedCodeBlock fencedCodeBlock) {
                String code = fencedCodeBlock.getLiteral();
                String language = fencedCodeBlock.getInfo();
                String prismLanguage = mapLanguageToPrism(language);
                
                contentHtml.append("<pre><code class=\"language-")
                        .append(prismLanguage)
                        .append("\">")
                        .append(escapeHtml(code))
                        .append("</code></pre>\n");
            } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                String code = indentedCodeBlock.getLiteral();
                contentHtml.append("<pre><code class=\"language-plaintext\">")
                        .append(escapeHtml(code))
                        .append("</code></pre>\n");
            } else {
                contentHtml.append(htmlRenderer.render(node));
            }
            node = node.getNext();
        }
        
        // JavaScript to update just the assistant message content
        String js = "try {" +
                   "  const messagePair = document.getElementById('" + escapeJS(messageId) + "');" +
                   "  if (messagePair) {" +
                   "    const assistantMessage = messagePair.querySelector('.assistant-message');" +
                   "    if (assistantMessage) {" +
                   "      assistantMessage.innerHTML = `" + escapeJS(contentHtml.toString()) + "`;" +
                   "      window.scrollTo(0, document.body.scrollHeight);" +
                   "      if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }" +
                   "    } else {" +
                   "      console.error('Assistant message element not found in message pair');" +
                   "    }" +
                   "  } else {" +
                   "    console.error('Message pair not found: " + escapeJS(messageId) + "');" +
                   "  }" +
                   "} catch (error) {" +
                   "  console.error('Error updating AI message:', error);" +
                   "}";
        
        LOG.info("Executing JavaScript to update AI message");
        executeJavaScript(js);
    }
    
    /**
     * Map language identifier to PrismJS language class.
     * This is a copy of the method in ChatMessageTemplate to keep functionality consistent.
     */
    private @NotNull String mapLanguageToPrism(@Nullable String languageInfo) {
        if (languageInfo == null || languageInfo.isEmpty()) {
            return "plaintext";
        }
        
        String lang = languageInfo.trim().toLowerCase();
        
        // Map common language identifiers to PrismJS language classes
        return switch (lang) {
            case "js", "javascript" -> "javascript";
            case "ts", "typescript" -> "typescript";
            case "py", "python" -> "python";
            case "java" -> "java";
            case "c#", "csharp", "cs" -> "csharp";
            case "c++" -> "cpp";
            case "go" -> "go";
            case "rust" -> "rust";
            case "rb", "ruby" -> "ruby";
            case "kt", "kotlin" -> "kotlin";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "html" -> "markup";
            case "css" -> "css";
            case "sh", "bash" -> "bash";
            case "md", "markdown" -> "markdown";
            case "sql" -> "sql";
            case "docker", "dockerfile" -> "docker";
            case "dart" -> "dart";
            case "graphql" -> "graphql";
            case "hcl" -> "hcl";
            case "nginx" -> "nginx";
            case "powershell", "ps" -> "powershell";
            // Add more language mappings as needed
            default -> "plaintext";
        };
    }
    
    /**
     * Escape HTML special characters.
     * This is a copy of the method in ChatMessageTemplate to keep functionality consistent.
     */
    private @NotNull String escapeHtml(@NotNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
    /**
     * Performs the actual operation of adding a chat message to the conversation.
     * Insert the message HTML at the end of the conversation container.
     * Using a more robust approach with createElement and appendChild instead of innerHTML +=
     * which can cause issues with event handlers on subsequent messages
     * 
     * @param messageHtml The HTML of the message to add
     */
private void doAddChatMessage(String messageHtml) {
        // First add the message to the conversation
        String js = "try {" +
                    "  const container = document.getElementById('conversation-container');" +
                    "  const tempDiv = document.createElement('div');" +
                    "  tempDiv.innerHTML = `" + escapeJS(messageHtml) + "`;" +
                    "  while (tempDiv.firstChild) {" +
                    "    container.appendChild(tempDiv.firstChild);" +
                    "  }" +
                    "  window.scrollTo(0, document.body.scrollHeight);" +
                    "  if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }" +
                    "} catch (error) {" +
                    "  console.error('Error adding chat message:', error);" +
                    "}";
        
        LOG.info("Executing JavaScript to add message");
        executeJavaScript(js);
    }
    
    /**
     * Execute JavaScript in the browser.
     *
     * @param script The JavaScript to execute
     */
    public void executeJavaScript(String script) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isLoaded) {
                browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
            } else {
                LOG.warn("Browser not loaded, cannot execute JavaScript");
            }
        });
    }
    
    /**
     * Escape JavaScript string literals.
     * This prevents issues when inserting HTML into JavaScript template literals.
     *
     * @param text The text to escape
     * @return Escaped text suitable for use in JavaScript
     */
    public @NotNull String escapeJS(@NotNull String text) {
        return text.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${");
    }
    
    /**
     * Clear the conversation content.
     */
    public void clearConversation() {
        executeJavaScript("document.getElementById('conversation-container').innerHTML = '';");
    }
    
    /**
     * Update the custom prompt commands in the welcome panel.
     */
    public void updateCustomPrompts(ResourceBundle resourceBundle) {
        showWelcomeContent(resourceBundle);
    }
    
    /**
     * Called when the IDE theme changes.
     * Refresh the web view with new styling based on the current theme.
     *
     * @param isDarkTheme true if the new theme is dark, false if it's light
     */
    @Override
    public void themeChanged(boolean isDarkTheme) {
        LOG.info("Theme changed to " + (isDarkTheme ? "dark" : "light") + " mode, refreshing web view");
        
        // Reload the content with the new theme
        ConversationTemplate template = new ConversationTemplate(webServer);
        String htmlContent = template.generate();
        
        // Update the browser content - this will reload with the new theme styles
        if (isLoaded) {
            // Create a new resource with the updated HTML content
            String resourceId = webServer.addDynamicResource(htmlContent);
            String resourceUrl = webServer.getResourceUrl(resourceId);
            
            // Set a flag to indicate that we should reload welcome content after the browser loads
            final boolean[] welcomeReloaded = {false};
            
            // Add a temporary load handler to reload the welcome content after the theme change
            browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                    if (!welcomeReloaded[0]) {
                        welcomeReloaded[0] = true;
                        LOG.info("Browser reloaded after theme change, restoring welcome content");
                        ApplicationManager.getApplication().invokeLater(() -> {
                            ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
                            showWelcomeContent(resourceBundle);
                        });
                    }
                }
            }, browser.getCefBrowser());
            
            // Reload the browser with the new content
            browser.loadURL(resourceUrl);
        }
    }
    
    /**
     * Adds just the user message to the conversation view without waiting for the AI response.
     * This is used to show the user's message immediately when they submit a prompt.
     *
     * @param chatMessageContext The chat message context containing the user prompt
     */
    public void addUserPromptMessage(@NotNull ChatMessageContext chatMessageContext) {
        if (!isLoaded) {
            LOG.warn("Browser not loaded yet, waiting before adding user message");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                while (!initialized.get()) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.error("Interrupted while waiting for browser to load", e);
                        return;
                    }
                }
                doAddUserPromptMessage(chatMessageContext);
            });
        } else {
            doAddUserPromptMessage(chatMessageContext);
        }
    }

    /**
     * Performs the actual operation of adding a user message to the conversation.
     * This creates a message pair with just the user's message and a loading indicator for the AI response.
     *
     * @param chatMessageContext The chat message context
     */
    private void doAddUserPromptMessage(@NotNull ChatMessageContext chatMessageContext) {
        // Create a div for the message pair with the user message and an empty assistant message
        String messageId = chatMessageContext.getId();
        String userPrompt = chatMessageContext.getUserPrompt() == null ? "" : chatMessageContext.getUserPrompt();
        
        // Format the user message as HTML with proper escaping
        String userMessage = "<div class=\"user-message\">" + escapeHtml(userPrompt) + "</div>";
        
        // Format the AI message placeholder with a loading indicator
        String aiMessagePlaceholder = "<div class=\"assistant-message\"><div class=\"loading-indicator\">Thinking...</div></div>";
        
        // Create the complete message pair HTML
        String messagePairHtml = 
                "<div class=\"message-pair\" id=\"" + escapeHtml(messageId) + "\">\n" +
                userMessage + "\n" +
                aiMessagePlaceholder + "\n" +
                "</div>";
        
        // JavaScript to add the message to the conversation and scroll to bottom
        String js = "try {\n" +
                    "  const container = document.getElementById('conversation-container');\n" +
                    "  const tempDiv = document.createElement('div');\n" +
                    "  tempDiv.innerHTML = `" + escapeJS(messagePairHtml) + "`;\n" +
                    "  while (tempDiv.firstChild) {\n" +
                    "    container.appendChild(tempDiv.firstChild);\n" +
                    "  }\n" +
                    "  window.scrollTo(0, document.body.scrollHeight);\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error adding user message:', error);\n" +
                    "}";
        
        LOG.info("Executing JavaScript to add user message");
        executeJavaScript(js);
    }

    public void addFileReferences(ChatMessageContext chatMessageContext, List<VirtualFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        
        LOG.info("Adding file references to conversation: " + files.size() + " files");
        
        // Create HTML for the expandable file references component
        StringBuilder fileReferencesHtml = new StringBuilder();
        fileReferencesHtml.append("<div class=\"file-references-container\" id=\"file-refs-")
                .append(escapeHtml(chatMessageContext.getId()))
                .append("\">\n");
                
        // Add the collapsible header
        fileReferencesHtml.append("  <div class=\"file-references-header\" onclick=\"toggleFileReferences(this)\">\n")
                .append("    <span class=\"file-references-icon\">📂</span>\n")
                .append("    <span class=\"file-references-title\">Referenced Files (")
                .append(files.size())
                .append(")</span>\n")
                .append("    <span class=\"file-references-toggle\">▶</span>\n")
                .append("  </div>\n");
                
        // Add the file list container (initially collapsed)
        fileReferencesHtml.append("  <div class=\"file-references-content\" style=\"display: none;\">\n")
                .append("    <ul class=\"file-list\">\n");
                
        // Add each file as a list item - make items clickable with data attributes to store path
        for (int i = 0; i < files.size(); i++) {
            VirtualFile file = files.get(i);
            String fileId = "file-" + escapeHtml(chatMessageContext.getId()) + "-" + i;
            fileReferencesHtml.append("      <li class=\"file-item\" id=\"")
                    .append(fileId)
                    .append("\" data-file-path=\"")
                    .append(escapeHtml(file.getPath()))
                    .append("\" style=\"cursor: pointer;\" onclick=\"openFile('")
                    .append(escapeJS(fileId)) // Use escapeJS here since it's inside JavaScript
                    .append("')\">\n")
                    .append("        <span class=\"file-name\">")
                    .append(escapeHtml(file.getName()))
                    .append("</span>\n")
                    .append("        <span class=\"file-path\">")
                    .append(escapeHtml(file.getPath().replace(chatMessageContext.getProject().getBasePath() + "/", "")))
                    .append("</span>\n")
                    .append("      </li>\n");
        }
        
        fileReferencesHtml.append("    </ul>\n")
                .append("  </div>\n")
                .append("</div>\n");
                
        // JavaScript to add the file references after the message pair
        String js = "try {\n" +
                    "  const messagePair = document.getElementById('" + escapeJS(chatMessageContext.getId()) + "');\n" +
                    "  if (messagePair) {\n" +
                    "    // Create a container for the file references\n" +
                    "    const fileRefsContainer = document.createElement('div');\n" +
                    "    fileRefsContainer.innerHTML = `" + escapeJS(fileReferencesHtml.toString()) + "`;\n" +
                    "    \n" +
                    "    // Insert the file references after the message pair\n" +
                    "    messagePair.parentNode.insertBefore(fileRefsContainer, messagePair.nextSibling);\n" +
                    "    \n" +
                    "    // If the file opening functionality isn't already defined, add it\n" +
                    "    if (!window.openFile) {\n" +
                    "      window.openFile = function(fileId) {\n" +
                    "        const fileElement = document.getElementById(fileId);\n" +
                    "        if (fileElement && fileElement.dataset.filePath) {\n" +
                    "          // Post a message to Java to open the file\n" +
                    "          console.log('Opening file: ' + fileElement.dataset.filePath);\n" +
                    "          openFileFromJava(fileElement.dataset.filePath);\n" +
                    "        }\n" +
                    "      };\n" +
                    "    }\n" +
                    "    \n" +
                    "    // If the toggle functionality isn't already defined, add it\n" +
                    "    if (!window.toggleFileReferences) {\n" +
                    "      window.toggleFileReferences = function(header) {\n" +
                    "        const content = header.nextElementSibling;\n" +
                    "        const toggle = header.querySelector('.file-references-toggle');\n" +
                    "        if (content.style.display === 'none') {\n" +
                    "          content.style.display = 'block';\n" +
                    "          toggle.textContent = '▼';\n" +
                    "        } else {\n" +
                    "          content.style.display = 'none';\n" +
                    "          toggle.textContent = '▶';\n" +
                    "        }\n" +
                    "      };\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Add styles if they don't exist yet\n" +
                    "    if (!document.getElementById('file-references-styles')) {\n" +
                    "      const styleEl = document.createElement('style');\n" +
                    "      styleEl.id = 'file-references-styles';\n" +
                    "      styleEl.textContent = `\n" +
                    "        .file-references-container { margin: 10px 0; background-color: " + (ThemeDetector.isDarkTheme() ? "#1e1e1e" : "#f5f5f5") + "; border-radius: 4px; border-left: 4px solid " + (ThemeDetector.isDarkTheme() ? "#64b5f6" : "#2196F3") + "; }\n" +
                    "        .file-references-header { padding: 10px 8px; cursor: pointer; display: flex; align-items: center; }\n" +
                    "        .file-references-header:hover { background-color: " + (ThemeDetector.isDarkTheme() ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.05)") + "; }\n" +
                    "        .file-references-icon { margin-right: 8px; }\n" +
                    "        .file-references-title { flex-grow: 1; font-weight: bold; }\n" +
                    "        .file-references-toggle { margin-left: 8px; }\n" +
                    "        .file-references-content { padding: 10px 8px; border-top: 1px solid " + (ThemeDetector.isDarkTheme() ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)") + "; }\n" +
                    "        .file-list { list-style-type: none; padding: 0; margin: 0; }\n" +
                    "        .file-item { padding: 5px 0; }\n" +
                    "        .file-name { font-weight: bold; margin-right: 8px; }\n" +
                    "        .file-path { color: " + (ThemeDetector.isDarkTheme() ? "#aaaaaa" : "#666666") + "; font-style: italic; font-size: 0.9em; }\n" +
                    "      `;\n" +
                    "      document.head.appendChild(styleEl);\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Scroll to show the file references\n" +
                    "    window.scrollTo(0, document.body.scrollHeight);\n" +
                    "  } else {\n" +
                    "    console.error('Message pair not found: " + escapeJS(chatMessageContext.getId()) + "');\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error adding file references:', error);\n" +
                    "}";
                
        LOG.info("Executing JavaScript to add file references");
        executeJavaScript(js);
    }
}
