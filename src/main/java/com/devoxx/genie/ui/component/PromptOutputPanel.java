package com.devoxx.genie.ui.component;

import com.devoxx.genie.model.request.PromptContext;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.devoxx.genie.ui.util.HelpUtil;
import com.devoxx.genie.ui.util.WelcomeUtil;
import com.devoxx.genie.ui.util.WorkingMessage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import static com.devoxx.genie.ui.util.DevoxxGenieColors.*;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.DevoxxIcon;
import static java.util.Collections.emptyList;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public class PromptOutputPanel extends JBPanel {

    private final transient ResourceBundle resourceBundle;
    private final JBPanel container = new JBPanel();
    private final JBPanel welcomePanel = new JBPanel(new BorderLayout());
    private JBPanel waitingPanel;
    private final transient Project project;
    private final JBScrollPane scrollPane;

    public PromptOutputPanel(Project project, ResourceBundle resourceBundle) {
        super();
        this.project = project;
        this.resourceBundle = resourceBundle;

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        scrollPane = new JBScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);

        welcomePanel.add(new JBLabel(WelcomeUtil.getWelcomeText(resourceBundle)), BorderLayout.NORTH);
        showWelcomeText();
    }

    public void clear() {
        container.removeAll();
        showWelcomeText();

        revalidate();
        repaint();

        // TODO The list of ChatMessages should also be cleared
    }

    /**
     * Show the welcome text.
     */
    public void showWelcomeText() {
        welcomePanel.setVisible(true);
        container.add(welcomePanel);
    }

    /**
     * Show the help text.
     */
    public void showHelpText() {
        addFiller();

        JBPanel helpPanel = getBgPanel();
        helpPanel.setMaximumSize(new Dimension(1500, 125));
        helpPanel.add(new JBLabel(HelpUtil.getHelpMessage(resourceBundle)), BorderLayout.CENTER);
        container.add(helpPanel);

        revalidate();
        repaint();
    }

    /**
     * Add a filler to the panel.
     */
    private void addFiller() {
        container.add(Box.createRigidArea(new Dimension(20, 10)));
    }

    /**
     * Add a user prompt to the panel.
     *
     * @param userPrompt the user prompt
     */
    public void addUserPrompt(String userPrompt) {
        welcomePanel.setVisible(false);

        JBPanel userPromptPanel = getBgPanel();
        userPromptPanel.setMaximumSize(new Dimension(1500, 50));
        userPromptPanel.setOpaque(false);

        JBLabel userPromptLabel = new JBLabel("<html>" + userPrompt.replace("\n", "<br/>") + "</html>", DevoxxIcon, SwingConstants.LEFT);
        userPromptPanel.add(userPromptLabel, BorderLayout.CENTER);
        userPromptPanel.setBackground(PROMPT_BG_COLOR);

        waitingPanel = new JBPanel<>(new BorderLayout());
        waitingPanel.getInsets().set(5, 5, 5, 5);
        waitingPanel.setOpaque(true);
        waitingPanel.setMaximumSize(new Dimension(500, 30));
        waitingPanel.setBackground(PROMPT_BG_COLOR);

        JBLabel workingLabel = new JBLabel(WorkingMessage.getWorkingMessage());
        workingLabel.setFont(workingLabel.getFont().deriveFont(12f));
        workingLabel.setForeground(GRAY_COLOR);
        workingLabel.setMaximumSize(new Dimension(500, 30));
        waitingPanel.add(workingLabel, BorderLayout.SOUTH);

        userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);

        addFiller();
        container.add(userPromptPanel);

        scrollToBottom();
    }

    private static @NotNull JBPanel getBgPanel() {
        JBPanel userPromptPanel = new JBPanel<>(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.dispose();
            }
        };
        userPromptPanel.setBackground(DEFAULT_BG_COLOR);
        userPromptPanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(GRAY_COLOR, 1, 5),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return userPromptPanel;
    }

    /**
     * Add a response to the panel.
     *
     * @param promptContext  the prompt context
     * @param promptResponse the completion response
     */
    public void addChatResponse(PromptContext promptContext, String promptResponse) {
        welcomePanel.setVisible(false);
        waitingPanel.setVisible(false);
        addFiller();

        JBPanel responsePanel = getBgPanel();
        responsePanel.setBackground(PROMPT_BG_COLOR);
        responsePanel.setOpaque(false);

        JPanel topPanel = new JPanel(new BorderLayout());

        JBPanel infoPanel = getInfoPanel(promptContext);
        infoPanel.setBackground(PROMPT_BG_COLOR);

        topPanel.add(infoPanel, BorderLayout.NORTH);
        if (promptContext.getEditorInfo().getSelectedFiles() != null &&
            !promptContext.getEditorInfo().getSelectedFiles().isEmpty()) {
            ExpandablePanel fileListPanel = new ExpandablePanel(promptContext);
            topPanel.add(fileListPanel, BorderLayout.SOUTH);
        }

        responsePanel.add(topPanel, BorderLayout.NORTH);
        responsePanel.add(getResponsePane(promptResponse), BorderLayout.CENTER);

        container.add(responsePanel);

        revalidate();
        repaint();
    }

    /**
     * Add a warning text to the panel.
     *
     * @param promptContext the prompt context
     * @param text          the warning text
     */
    public void addWarningText(PromptContext promptContext, String text) {
        welcomePanel.setVisible(false);

        JBPanel panel = new JBPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(JBColor.RED, 1, 5),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        panel.setMaximumSize(new Dimension(1500, 75));

        JBLabel jLabel = new JBLabel(text, SwingConstants.LEFT);

        JBScrollPane scrollPane = new JBScrollPane(jLabel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(getInfoPanel(promptContext), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        addFiller();
        container.add(panel);
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
            200,
            emptyList(),
            BrowserHyperlinkListener.INSTANCE);
    }

    /**
     * The info panel shows the timestamp and the model provider & name.
     *
     * @param promptContext the prompt context
     * @return the info panel
     */
    private static @NotNull JBPanel getInfoPanel(PromptContext promptContext) {
        JBPanel responseHeader = new JBPanel<>(new BorderLayout());
        responseHeader.setOpaque(true);
        responseHeader.setMaximumSize(new Dimension(500, 30));
        responseHeader.setPreferredSize(new Dimension(500, 30));

        JBLabel createdOnLabel = new JBLabel(promptContext.getCreatedOn().format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")));
        createdOnLabel.setFont(createdOnLabel.getFont().deriveFont(12f));
        responseHeader.add(createdOnLabel, BorderLayout.WEST);

        String label = (promptContext.getLlmProvider() != null ? promptContext.getLlmProvider() : "") +
            (promptContext.getModelName() != null ? " - " + promptContext.getModelName() : "");

        JBLabel modelLabel = new JBLabel(label);
        modelLabel.setFont(modelLabel.getFont().deriveFont(12f));
        responseHeader.add(modelLabel, BorderLayout.EAST);
        return responseHeader;
    }

    /**
     * Scroll to the bottom of the panel after repainting the new content.
     */
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
}
