package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.PromptContext;
import com.devoxx.genie.ui.component.JEditorPaneUtilsKt;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.devoxx.genie.ui.util.HelpUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static com.devoxx.genie.ui.util.DevoxxGenieColors.*;
import static java.util.Collections.emptyList;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public class PromptOutputPanel extends JBPanel<PromptOutputPanel> {

    private final transient Project project;
    private final JPanel container = new JPanel();
    private final WelcomePanel welcomePanel;
    private final HelpPanel helpPanel;
    private final WaitingPanel waitingPanel = new WaitingPanel();
    private final JBScrollPane scrollPane;

    /**
     * The prompt output panel.
     * @param project        the project
     * @param resourceBundle the resource bundle
     */
    public PromptOutputPanel(Project project, ResourceBundle resourceBundle) {
        super();
        this.project = project;

        welcomePanel = new WelcomePanel(resourceBundle);
        helpPanel = new HelpPanel(HelpUtil.getHelpMessage(resourceBundle));

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        scrollPane = new JBScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        showWelcomeText();
    }

    /**
     * Clear the panel and show welcome text.
     */
    public void clear() {
        container.removeAll();
        showWelcomeText();
        moveToBottom();
    }

    /**
     * Show the welcome text.
     */
    public void showWelcomeText() {
        welcomePanel.showMsg();
        container.add(welcomePanel);
    }

    /**
     * Show the help text.
     */
    public void showHelpText() {
        addFiller("help");
        container.add(helpPanel);
        moveToBottom();
    }

    /**
     * Add a filler to the panel.
     */
    private void addFiller(String name) {
        container.add(new FillerPanel(name));
    }

    /**
     * Add a user prompt to the panel.
     * @param promptContext the prompt context
     */
    public void addUserPrompt(PromptContext promptContext) {
        container.remove(welcomePanel);
        waitingPanel.showMsg();
        UserPromptPanel userPromptPanel = new UserPromptPanel(promptContext);
        userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);
        addFiller(promptContext.getName());
        container.add(userPromptPanel);
        moveToBottom();
    }

    /**
     * Add a response to the panel.
     *
     * @param promptContext  the prompt context
     * @param promptResponse the completion response
     */
    public void addChatResponse(PromptContext promptContext, String promptResponse) {
        waitingPanel.hideMsg();

        addFiller(promptContext.getName());

        ResponsePromptPanel responsePromptPanel = new ResponsePromptPanel(promptContext);
        responsePromptPanel.add(new ResponseHeaderPanel(promptContext, container).withBackground(PROMPT_BG_COLOR), BorderLayout.NORTH);
        responsePromptPanel.add(getResponsePane(promptResponse), BorderLayout.CENTER);
        container.add(responsePromptPanel);

        moveToBottom();
    }

    /**
     * Add a warning text to the panel.
     *
     * @param promptContext the prompt context
     * @param text          the warning text
     */
    public void addWarningText(PromptContext promptContext, String text) {
        welcomePanel.setVisible(false);
        addFiller("warning");
        container.add(new WarningPanel(text, promptContext, text));
    }

    /**
     * Get the response pane with rendered HTML.
     *
     * @param promptResponse the completion response
     * @return the response pane
     */
    private @NotNull JEditorPane getResponsePane(String promptResponse) {

        Node node = Parser.builder().build().parse(promptResponse);

        HtmlRenderer renderer = HtmlRenderer
            .builder()
            .nodeRendererFactory(context -> {
                final CodeBlockNodeRenderer[] codeBlockRenderer = new CodeBlockNodeRenderer[1];
                ApplicationManager.getApplication().runReadAction(() -> {
                    codeBlockRenderer[0] = new CodeBlockNodeRenderer(project, context);
                });
                return codeBlockRenderer[0];
            })
            .escapeHtml(true).build();

        String htmlResponse = renderer.render(node);

        CharSequence charSequence = htmlResponse.subSequence(0, htmlResponse.length() - 1);

        // TODO upgrade to `JBHtmlPane` in 2024.2

        return JEditorPaneUtilsKt.htmlJEditorPane(charSequence,
            emptyList(),
            BrowserHyperlinkListener.INSTANCE);
    }

    /**
     * Scroll to the bottom of the panel after repainting the new content.
     */
    private void moveToBottom() {
        // This will request a layout update.
        revalidate();
        repaint();

        // SwingUtilities.invokeLater will schedule the scrolling to happen
        // after all pending events are processed, including revalidate and repaint.
        SwingUtilities.invokeLater(() -> {
            // Ensure the viewport's contents are updated before fetching the maximum scroll value.
            scrollPane.getViewport().validate();
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
}
