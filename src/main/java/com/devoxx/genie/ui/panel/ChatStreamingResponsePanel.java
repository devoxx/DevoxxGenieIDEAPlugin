package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.ui.util.DevoxxGenieFonts.SourceCodeProFontPlan14;

public class ChatStreamingResponsePanel extends BackgroundPanel {

    private final JEditorPane editorPane = createEditorPane();
    private final StringBuilder markdownContent = new StringBuilder();

    private final Parser parser;
    private final HtmlRenderer renderer;

    /**
     * Create a new chat response panel.
     *
     * @param chatMessageContext the chat message context
     */
    public ChatStreamingResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new ResponseHeaderPanel(chatMessageContext));
        add(editorPane);

        parser = Parser.builder().build();
        renderer = HtmlRenderer
            .builder()
            .nodeRendererFactory(context -> {
                AtomicReference<CodeBlockNodeRenderer> codeBlockRenderer = new AtomicReference<>();
                ApplicationManager.getApplication().runReadAction(() -> {
                    codeBlockRenderer.getAndSet(new CodeBlockNodeRenderer(chatMessageContext.getProject(), context));
                });
                return codeBlockRenderer.get();
            })
            .escapeHtml(true)
            .build();
    }

    /**
     * Insert token into document stream
     * @param token the LLM string token
     */
    public void insertToken(String token) {
        SwingUtilities.invokeLater(() -> {
            markdownContent.append(token);
            String fullHtmlContent = renderer.render(parser.parse(markdownContent.toString()));
            editorPane.setText(fullHtmlContent);
        });
    }

    /**
     * Create an editor pane.
     * @return the editor pane
     */
    private @NotNull JEditorPane createEditorPane() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setFont(SourceCodeProFontPlan14);
        return editorPane;
    }
}

