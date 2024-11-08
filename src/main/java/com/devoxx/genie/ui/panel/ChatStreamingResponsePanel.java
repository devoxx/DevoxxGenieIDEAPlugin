package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.ui.util.DevoxxGenieFontsUtil.SourceCodeProFontPlan14;

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
        super(chatMessageContext.getId());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new ResponseHeaderPanel(chatMessageContext));
        add(editorPane);

        setMaxWidth();

        if (chatMessageContext.hasFiles()) {
            java.util.List<VirtualFile> files = FileListManager.getInstance().getFiles();
            ExpandablePanel fileListPanel = new ExpandablePanel(chatMessageContext, files);
            add(fileListPanel);
        }

        parser = Parser.builder().build();
        renderer = createHTMLRenderer(chatMessageContext);
    }

    private static HtmlRenderer createHTMLRenderer(@NotNull ChatMessageContext chatMessageContext) {
        return HtmlRenderer
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

    private void setMaxWidth() {
        Dimension maximumSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        editorPane.setMaximumSize(maximumSize);
        editorPane.setMinimumSize(new Dimension(editorPane.getPreferredSize().width, editorPane.getPreferredSize().height));
    }

    /**
     * Insert token into document stream
     *
     * @param token the LLM string token
     */
    public void insertToken(String token) {
        ApplicationManager.getApplication().invokeLater(() -> {
            markdownContent.append(token);
            String fullHtmlContent = renderer.render(parser.parse(markdownContent.toString()));
            editorPane.setText(fullHtmlContent);
        });
    }

    /**
     * Create an editor pane.
     *
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

