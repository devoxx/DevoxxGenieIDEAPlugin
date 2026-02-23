package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.CodeLanguageUtil;
import com.devoxx.genie.util.ThreadUtils;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles updating AI messages in the WebView.
 * This class is responsible for updating the content of AI responses.
 */
@Slf4j
public class WebViewAIMessageUpdater {

    private final WebViewJavaScriptExecutor jsExecutor;
    private final AtomicBoolean initialized;

    public WebViewAIMessageUpdater(WebViewJavaScriptExecutor jsExecutor, AtomicBoolean initialized) {
        this.jsExecutor = jsExecutor;
        this.initialized = initialized;
    }

    /**
     * Updates just the AI response part of an existing message in the conversation view.
     * Used for streaming responses to avoid creating multiple message pairs.
     *
     * @param chatMessageContext The chat message context
     */
    public void updateAiMessageContent(@NotNull ChatMessageContext chatMessageContext) {
        if (chatMessageContext.getAiMessage() == null) {
            log.warn("No AI message to update for context: {}", chatMessageContext.getId());
            return;
        }

        if (!jsExecutor.isLoaded()) {
            log.warn("Browser not loaded yet, waiting before updating message");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                ThreadUtils.sleep(100);
                doUpdateAiMessageContent(chatMessageContext);
            });
        } else {
            doUpdateAiMessageContent(chatMessageContext);
        }
    }

    /**
     * Performs the actual update of just the AI response content.
     * Handles streaming and non-streaming modes differently:
     * - Non-streaming: MCP activity is in a separate sibling div (mcp-{id}), just set assistant content
     * - Streaming: Put MCP activity at top of assistant-message with mcp-inline class
     *
     * @param chatMessageContext The chat message context
     */
    private void doUpdateAiMessageContent(@NotNull ChatMessageContext chatMessageContext) {
        String messageId = chatMessageContext.getId();

        // Parse and render the markdown content
        Parser markdownParser = Parser.builder().build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().escapeHtml(true).build();

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

        // Add metadata div
        contentHtml.append("<div class=\"metadata-info\">")
                .append(timestamp)
                .append(" · ")
                .append(modelName)
                .append(String.format(" · ϟ %.2fs", chatMessageContext.getExecutionTimeMs() / 1000.0)
                        // Add metadata div
                )
                .append("</div>")
                .append("<button class=\"copy-response-button\" onclick=\"copyMessageResponse(this)\"><img src=\"/icons/copy.svg\" alt=\"Copy\" class=\"copy-icon\"></button>");

        // Add content
        Node node = document.getFirstChild();
        while (node != null) {
            if (node instanceof FencedCodeBlock fencedCodeBlock) {
                String code = fencedCodeBlock.getLiteral();
                String language = fencedCodeBlock.getInfo();
                contentHtml.append("<pre><code class=\"language-")
                        .append(CodeLanguageUtil.mapLanguageToPrism(language))
                        .append("\">")
                        .append(jsExecutor.escapeHtml(code))
                        .append("</code></pre>\n");
            } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                String code = indentedCodeBlock.getLiteral();
                contentHtml.append("<pre><code class=\"language-plaintext\">")
                        .append(jsExecutor.escapeHtml(code))
                        .append("</code></pre>\n");
            } else {
                contentHtml.append(htmlRenderer.render(node));
            }
            node = node.getNext();
        }

        boolean isStreaming = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode());
        String escapedContent = jsExecutor.escapeJS(contentHtml.toString());
        String escapedMessageId = jsExecutor.escapeJS(messageId);

        // Build mode-aware JavaScript to update the assistant message content
        String js;
        // JS helper: detach intermediate agent text from DOM before content replacement,
        // then re-attach it at the top afterward (pure DOM node manipulation, no innerHTML for this part)
        String detachIntermediateJs =
                "      var _intEl = assistantMessage.querySelector('[id^=\"agent-intermediate-\"]');" +
                "      if (_intEl) _intEl.parentNode.removeChild(_intEl);";
        String reattachIntermediateJs =
                "      if (_intEl) assistantMessage.insertBefore(_intEl, assistantMessage.firstChild);";

        if (isStreaming) {
            // Streaming: preserve MCP activity at top of assistant-message with mcp-inline class
            js = "try {" +
                    "  const messagePair = document.getElementById('" + escapedMessageId + "');" +
                    "  if (messagePair) {" +
                    "    const assistantMessage = messagePair.querySelector('.assistant-message');" +
                    "    if (assistantMessage) {" +
                    detachIntermediateJs +
                    "      const loadingIndicator = assistantMessage.querySelector('.loading-indicator');" +
                    "      const hasMcpContent = loadingIndicator && loadingIndicator.querySelector('.mcp-outer-container');" +
                    "      if (hasMcpContent) {" +
                    "        loadingIndicator.classList.add('mcp-inline');" +
                    "        loadingIndicator.style.display = 'block';" +
                    "        const mcpHtml = loadingIndicator.outerHTML;" +
                    "        assistantMessage.innerHTML = mcpHtml + `" + escapedContent + "`;" +
                    "      } else {" +
                    "        if (loadingIndicator) loadingIndicator.style.display = 'none';" +
                    "        assistantMessage.innerHTML = `" + escapedContent + "`;" +
                    "      }" +
                    reattachIntermediateJs +
                    "      setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);" +
                    "      if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }" +
                    "    }" +
                    "  }" +
                    "} catch (error) {" +
                    "  console.error('Error updating AI message:', error);" +
                    "}";
        } else {
            // Non-streaming: MCP activity is in separate mcp-{id} div, just set content directly
            js = "try {" +
                    "  const thinkingText = document.getElementById('loading-" + escapedMessageId + "');" +
                    "  if (thinkingText) thinkingText.style.display = 'none';" +
                    "  const messagePair = document.getElementById('" + escapedMessageId + "');" +
                    "  if (messagePair) {" +
                    "    const assistantMessage = messagePair.querySelector('.assistant-message');" +
                    "    if (assistantMessage) {" +
                    detachIntermediateJs +
                    "      assistantMessage.innerHTML = `" + escapedContent + "`;" +
                    reattachIntermediateJs +
                    "      setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);" +
                    "      if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }" +
                    "    }" +
                    "  }" +
                    "} catch (error) {" +
                    "  console.error('Error updating AI message:', error);" +
                    "}";
        }

        log.info("Executing JavaScript to update AI message");
        jsExecutor.executeJavaScript(js);
    }

    /**
     * Adds just the user message to the conversation view without waiting for the AI response.
     * This is used to show the user's message immediately when they submit a prompt.
     *
     * @param chatMessageContext The chat message context containing the user prompt
     */
    public void addUserPromptMessage(@NotNull ChatMessageContext chatMessageContext) {
        if (!jsExecutor.isLoaded()) {
            log.warn("Browser not loaded yet, waiting before adding user message");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                while (!initialized.get()) {
                   ThreadUtils.sleep(100);
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
     * In non-streaming mode, a separate MCP activity section is added between user and assistant messages.
     *
     * @param chatMessageContext The chat message context
     */
    private void doAddUserPromptMessage(@NotNull ChatMessageContext chatMessageContext) {
        // Create a div for the message pair with the user message and an empty assistant message
        String messageId = chatMessageContext.getId();

        String userPrompt = chatMessageContext.getUserPrompt() == null ? "" : chatMessageContext.getUserPrompt();

        // Parse and render the user message as markdown
        Parser markdownParser = Parser.builder().build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().escapeHtml(true).build();
        Node userDocument = markdownParser.parse(userPrompt);
        String userMessageContent = htmlRenderer.render(userDocument);

        // Format the user message as HTML
        String userMessage = "<div class=\"user-message\">" + userMessageContent + "</div>";

        boolean isStreaming = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode());

        // Format the AI message placeholder with a loading indicator that shows MCP will be displayed
        String aiMessagePlaceholder = "<div class=\"assistant-message\"><div class=\"loading-indicator\" id=\"loading-"
                + jsExecutor.escapeHtml(messageId) + "\">Thinking...</div></div>";

        // For non-streaming mode, add a separate MCP activity section between user and assistant messages
        String mcpSection = "";
        if (!isStreaming) {
            mcpSection = "<div class=\"mcp-activity-section\" id=\"mcp-" + jsExecutor.escapeHtml(messageId) + "\"></div>";
        }

        // Create the complete message pair HTML
        String messagePairHtml =
                "<div class=\"message-pair\" id=\"" + jsExecutor.escapeHtml(messageId) + "\">\n" +
                userMessage + "\n" +
                mcpSection + "\n" +
                aiMessagePlaceholder + "\n" +
                "</div>";

        // JavaScript to add the message to the conversation
        // First check if the message ID already exists to avoid duplicates
        String js = "try {\n" +
                    "  if (!document.getElementById('" + jsExecutor.escapeJS(messageId) + "')) {\n" +
                    "    const container = document.getElementById('conversation-container');\n" +
                    "    const tempDiv = document.createElement('div');\n" +
                    "    tempDiv.innerHTML = `" + jsExecutor.escapeJS(messagePairHtml) + "`;\n" +
                    "    while (tempDiv.firstChild) {\n" +
                    "      container.appendChild(tempDiv.firstChild);\n" +
                    "    }\n" +
                    "    if (container.childElementCount === 1) {\n" +
                    "      const firstMessage = container.firstElementChild;\n" +
                    "      if (firstMessage) {\n" +
                    "        firstMessage.style.marginTop = '30px';\n" +
                    "      }\n" +
                    "      window.scrollTo(0, 0);\n" +
                    "    } else {\n" +
                    "      setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);\n" +
                    "    }\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error adding user message:', error);\n" +
                    "}\n";

        log.info("Executing JavaScript to add user message");
        jsExecutor.executeJavaScript(js);
    }
}
