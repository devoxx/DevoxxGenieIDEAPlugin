package com.devoxx.genie.ui.webview;

import com.devoxx.genie.model.request.ChatMessageContext;
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
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

/**
 * Controller for managing a single WebView that can display entire conversations.
 * This class handles the interaction between the Java code and the WebView, appending
 * new content to the conversation without creating new WebView instances.
 */
public class ConversationWebViewController {
    private static final Logger LOG = Logger.getInstance(ConversationWebViewController.class);

    @Getter
    private final JBCefBrowser browser;
    private final WebServer webServer;
    private boolean isLoaded = false;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Creates a new ConversationWebViewController with a fresh browser.
     */
    public ConversationWebViewController() {
        this.webServer = WebServer.getInstance();
        
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
    public void updateAiMessageContent(ChatMessageContext chatMessageContext) {
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
    private void doUpdateAiMessageContent(ChatMessageContext chatMessageContext) {
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
}
