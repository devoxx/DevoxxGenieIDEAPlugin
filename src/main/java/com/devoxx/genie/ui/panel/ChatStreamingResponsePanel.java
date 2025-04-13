package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.devoxx.genie.ui.panel.chatresponse.ResponseHeaderPanel;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.EditorCssFontResolver;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_TEXT_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieFontsUtil.SOURCE_CODE_PRO_FONT;

public class ChatStreamingResponsePanel extends BackgroundPanel {

    private final JEditorPane editorPane = createEditorPane();
    private final StringBuilder markdownContent = new StringBuilder();

    private final transient Parser parser;
    private final transient HtmlRenderer renderer;

    /**
     * Create a new chat response panel.
     *
     * @param chatMessageContext the chat message context
     */
    public ChatStreamingResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new ResponseHeaderPanel(chatMessageContext));

        editorPane.setBackground(PROMPT_BG_COLOR);
        editorPane.setForeground(PROMPT_TEXT_COLOR);

        add(editorPane);

        setMaxWidth();

        if (!FileListManager.getInstance().isEmpty(chatMessageContext.getProject())) {
            java.util.List<VirtualFile> files = FileListManager.getInstance().getFiles(chatMessageContext.getProject());
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
                ApplicationManager.getApplication().runReadAction((Computable<CodeBlockNodeRenderer>) () ->
                        codeBlockRenderer.getAndSet(new CodeBlockNodeRenderer(chatMessageContext.getProject(), context))
                );
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
        JEditorPane theEditorPane = new JEditorPane();
        theEditorPane.setContentType("text/html");
        theEditorPane.setEditable(false);
        
        // Set up HTML editor kit with proper styling to match non-streaming responses
        HTMLEditorKitBuilder htmlEditorKitBuilder =
            new HTMLEditorKitBuilder()
                .withWordWrapViewFactory()
                .withFontResolver(EditorCssFontResolver.getGlobalInstance());

        HTMLEditorKit editorKit = htmlEditorKitBuilder.build();
        
        // Apply the same stylesheet that's used for regular chat responses
        editorKit.getStyleSheet().addStyleSheet(StyleSheetsFactory.createParagraphStyleSheet());
        theEditorPane.setEditorKit(editorKit);
        
        return theEditorPane;
    }
}

