package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.devoxx.genie.ui.webview.WebServer;
import com.knuddels.jtokkit.api.Encoding;
import dev.langchain4j.model.output.TokenUsage;
import org.commonmark.node.Block;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.devoxx.genie.ui.util.CodeLanguageUtil;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Template for generating HTML for chat messages.
 */
public class ChatMessageTemplate extends HtmlTemplate {

    private final ChatMessageContext chatMessageContext;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    /**
     * Constructor with WebServer and ChatMessageContext dependencies.
     *
     * @param webServer The web server instance for resource URLs
     * @param chatMessageContext The chat message context to render
     */
    public ChatMessageTemplate(WebServer webServer, ChatMessageContext chatMessageContext) {
        super(webServer);
        this.chatMessageContext = chatMessageContext;
        this.markdownParser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    @Override
    public @NotNull String generate() {
        // Load the template from resources
        String messageTemplate = ResourceLoader.loadResource("webview/html/chatmessage.html");
        
        // Generate the assistant's content by parsing and rendering markdown
        StringBuilder assistantContentBuilder = new StringBuilder();
        Node document = markdownParser.parse(chatMessageContext.getAiMessage() == null ? "" : chatMessageContext.getAiMessage().text());
        Node node = document.getFirstChild();

        while (node != null) {
            if (node instanceof FencedCodeBlock fencedCodeBlock) {
                assistantContentBuilder.append(renderCodeBlock(fencedCodeBlock));
            } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                assistantContentBuilder.append(renderCodeBlock(indentedCodeBlock));
            } else {
                assistantContentBuilder.append(htmlRenderer.render(node));
            }
            node = node.getNext();
        }
        
        // Parse and render the user message as markdown
        String userPrompt = chatMessageContext.getUserPrompt() == null ? "" : chatMessageContext.getUserPrompt();
        Node userDocument = markdownParser.parse(userPrompt);
        String userMessageContent = htmlRenderer.render(userDocument);
        
        // Replace the template variables with actual content
        return messageTemplate
                .replace("${messageId}", chatMessageContext.getId())
                .replace("${userPrompt}", userMessageContent)
                .replace("${metadata}", formatMetadata())
                .replace("${assistantContent}", assistantContentBuilder.toString());
    }
    
    /**
     * Format metadata information for display in the WebView.
     * Includes date, LLM model, execution time, and token usage.
     *
     * @return HTML string with formatted metadata
     */
    private @NotNull String formatMetadata() {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM ''yy HH:mm");
        String timestamp = dateTime.format(formatter);
        
        String modelName = "Unknown";
        if (chatMessageContext.getLanguageModel() != null) {
            modelName = chatMessageContext.getLanguageModel().getModelName();
        }
        
        // Add metrics data (execution time and token usage)
        StringBuilder metricInfo = new StringBuilder();
        metricInfo.append(String.format(" · ϟ %.2fs", chatMessageContext.getExecutionTimeMs() / 1000.0));
        
        // Add token usage information if available
        TokenUsage tokenUsage = chatMessageContext.getTokenUsage();
        if (tokenUsage != null) {
            // Calculate token counts (special handling for Ollama)
            if (chatMessageContext.getLanguageModel().getProvider() == ModelProvider.Ollama && 
                chatMessageContext.getFilesContext() != null) {
                Encoding encoding = ProjectContentService
                    .getEncodingForProvider(chatMessageContext.getLanguageModel().getProvider());
                int inputContextTokens = encoding.encode(chatMessageContext.getFilesContext()).size();
                tokenUsage = new TokenUsage(
                    tokenUsage.inputTokenCount() + inputContextTokens, 
                    tokenUsage.outputTokenCount()
                );
            }
            
            // Format token counts with locale-specific number formatting
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            String formattedInputTokens = numberFormat.format(tokenUsage.inputTokenCount());
            String formattedOutputTokens = numberFormat.format(tokenUsage.outputTokenCount());
            
            metricInfo.append(String.format(" · Tokens ↑ %s ↓ %s", formattedInputTokens, formattedOutputTokens));
            
            // Add cost information if applicable for API-based services
            if (DefaultLLMSettingsUtil.isApiKeyBasedProvider(
                    chatMessageContext.getLanguageModel().getProvider())) {
                metricInfo.append(String.format(" · $%.5f", chatMessageContext.getCost()));
            }
        }
        
        return "<div class=\"metadata-info\">" + timestamp + " · " + modelName + metricInfo.toString() + "</div>";
    }
    
    /**
     * Render a code block with PrismJS syntax highlighting.
     *
     * @param codeBlock The code block to render
     * @return HTML representation of the code block
     */
    private @NotNull String renderCodeBlock(@NotNull Block codeBlock) {
        StringBuilder sb = new StringBuilder();
        
        String code;
        String language = "";
        
        if (codeBlock instanceof FencedCodeBlock fencedCodeBlock) {
            code = fencedCodeBlock.getLiteral();
            language = fencedCodeBlock.getInfo();
        } else if (codeBlock instanceof IndentedCodeBlock indentedCodeBlock) {
            code = indentedCodeBlock.getLiteral();
        } else {
            return ""; // Unsupported code block type
        }

        // Create HTML for code block with PrismJS classes
        sb.append("<pre><code class=\"language-").append(CodeLanguageUtil.mapLanguageToPrism(language)).append("\">")
                .append(escapeHtml(code))
                .append("</code></pre>\n");
        
        return sb.toString();
    }
}