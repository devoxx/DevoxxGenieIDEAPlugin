package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.lang.Language;
import com.intellij.lang.documentation.DocumentationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.devoxx.genie.ui.util.DevoxxGenieFonts.SourceCodeProFontPlan14;

public class StreamingChatResponsePanel extends BackgroundPanel {

    private final ChatMessageContext chatMessageContext;

    private JEditorPane editorPane = null;

    private static final StringBuilder markdownContent = new StringBuilder();
    private String fullHtmlContent = "";

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();

    /**
     * Create a new chat response panel.
     * @param chatMessageContext the chat message context
     */
    public StreamingChatResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.chatMessageContext = chatMessageContext;

        add(new ResponseHeaderPanel(chatMessageContext));

        editorPane = createEditorPane();
        add(editorPane);
    }

    /**
     * Insert token into document stream
     * @param token the LLM string token
     */
    public void insertToken(String token) {
        SwingUtilities.invokeLater(() -> {
            markdownContent.append(token);
            fullHtmlContent = renderer.render(parser.parse(markdownContent.toString()));
            editorPane.setText(fullHtmlContent);
        });
    }

    /**
     * Create an editor pane.
     * @return the editor pane
     */
    private JEditorPane createEditorPane() {
        if (editorPane == null) {
            editorPane = new JEditorPane();
            editorPane.setContentType("text/html");
            editorPane.setEditable(false);
            editorPane.setOpaque(false);
            editorPane.setFont(SourceCodeProFontPlan14);
        }
        return editorPane;
    }


    /**
     * Highlight and encode the code snippet as HTML.
     * TODO integrate this in the insertToken method
     */
    private @NotNull String appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(Language language, String codeSnippet) {
        StringBuilder highlightedAndEncodedAsHtmlCodeSnippet = new StringBuilder();

        ApplicationManager.getApplication().runReadAction(() -> {
            HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                highlightedAndEncodedAsHtmlCodeSnippet,
                chatMessageContext.getProject(),
                language,
                codeSnippet,
                false,
                DocumentationSettings.getHighlightingSaturation(true)
            );
        });

        return highlightedAndEncodedAsHtmlCodeSnippet.toString();
    }
}

