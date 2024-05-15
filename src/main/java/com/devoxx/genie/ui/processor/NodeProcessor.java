package com.devoxx.genie.ui.processor;

import com.devoxx.genie.ui.component.JEditorPaneUtils;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BrowserHyperlinkListener;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.StyleSheet;
import java.util.concurrent.atomic.AtomicReference;

public interface NodeProcessor {

    /**
     * Process the node.
     * @return the panel
     */
    JPanel process();

    /**
     * Create an editor pane.
     * @param htmlResponse the HTML response
     * @return the editor pane
     */
    default JEditorPane createEditorPane(@NotNull String htmlResponse, StyleSheet styleSheet) {
        JEditorPane jEditorPane =
            JEditorPaneUtils.createHtmlJEditorPane(htmlResponse.subSequence(0, htmlResponse.length()),
                                                   BrowserHyperlinkListener.INSTANCE,
                                                   styleSheet);
        jEditorPane.setOpaque(false);
        jEditorPane.setEditable(false);
        return jEditorPane;
    }

    /**
     * Create an HTML renderer.
     * @return the HTML renderer
     */
    default HtmlRenderer createHtmlRenderer(Project project) {
        return HtmlRenderer
            .builder()
            .nodeRendererFactory(context -> {
                AtomicReference<CodeBlockNodeRenderer> codeBlockRenderer = new AtomicReference<>();
                ApplicationManager.getApplication().runReadAction(() -> {
                    codeBlockRenderer.getAndSet(new CodeBlockNodeRenderer(project, context));
                });
                return codeBlockRenderer.get();
            })
            .escapeHtml(true)
            .build();
    }
}
