package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.ChatMessageContext;
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
     * @param chatMessageContext the prompt context
     */
    public void addUserPrompt(ChatMessageContext chatMessageContext) {
        container.remove(welcomePanel);
        waitingPanel.showMsg();
        UserPromptPanel userPromptPanel = new UserPromptPanel(chatMessageContext);
        userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);
        addFiller(chatMessageContext.getName());
        container.add(userPromptPanel);
        moveToBottom();
    }

    /**
     * Add a response to the panel.
     *
     * @param chatMessageContext  the prompt context
     */
    public void addChatResponse(ChatMessageContext chatMessageContext) {
        waitingPanel.hideMsg();

        addFiller(chatMessageContext.getName());

        ResponsePromptPanel responsePromptPanel = new ResponsePromptPanel(chatMessageContext);
        responsePromptPanel.add(new ResponseHeaderPanel(chatMessageContext, container).withBackground(PROMPT_BG_COLOR), BorderLayout.NORTH);
        responsePromptPanel.add(getResponsePane(chatMessageContext), BorderLayout.CENTER);
        container.add(responsePromptPanel);

        moveToBottom();
    }

    /**
     * Add a warning text to the panel.
     *
     * @param chatMessageContext the prompt context
     * @param text          the warning text
     */
    public void addWarningText(ChatMessageContext chatMessageContext, String text) {
        welcomePanel.setVisible(false);
        addFiller("warning");
        container.add(new WarningPanel(text, chatMessageContext, text));
    }

    /**
     * Get the response pane with rendered HTML.
     * @param chatMessageContext the chat message context
     * @return the response pane
     */
    private @NotNull JEditorPane getResponsePane(ChatMessageContext chatMessageContext) {

        Node node = Parser.builder().build().parse(chatMessageContext.getAiMessage().text());

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
