package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.component.JEditorPaneUtils;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.panel.WelcomeWebViewPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.DevoxxGenieColorsUtil;
import com.devoxx.genie.ui.util.HelpUtil;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.EditorCssFontResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import dev.langchain4j.data.message.AiMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.HTMLEditorKit;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_TEXT_COLOR;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.UUID;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * This class represents the output panel for displaying chat prompts and responses.
 * It manages the user interface components related to displaying conversation history,
 * help messages, and user prompts.
 */
@Slf4j
public class PromptOutputPanel extends JBPanel<PromptOutputPanel> implements CustomPromptChangeListener, MCPLoggingMessage {

    private final transient Project project;

    private final JPanel container = new JPanel();
    @Getter
    private final WelcomeWebViewPanel welcomePanel;
    private final HelpPanel helpPanel;
    private final WaitingPanel waitingPanel = new WaitingPanel();
    private final JBScrollPane scrollPane;

    /**
     * Constructor for PromptOutputPanel.
     *
     * @param project       The current project.
     * @param resourceBundle The resource bundle for localization.
     */
    public PromptOutputPanel(Project project, ResourceBundle resourceBundle) {
        super();

        this.project = project;

        welcomePanel = new WelcomeWebViewPanel(resourceBundle);
        helpPanel = new HelpPanel(HelpUtil.getHelpMessage());

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        scrollPane = new JBScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(200, 200));
        showWelcomeText();

        ApplicationManager.getApplication().invokeLater(() ->
                MessageBusUtil.connect(project, connection ->
                    MessageBusUtil.subscribe(connection, AppTopics.MCP_LOGGING_MSG, this)
                ));
    }

    /**
     * Clears the container and shows the welcome text.
     */
    public void clear() {
        container.removeAll();
        showWelcomeText();
        scrollToBottom();
    }

    /**
     * Displays the welcome message in the panel.
     */
    public void showWelcomeText() {
        welcomePanel.showMsg();
        container.add(welcomePanel);
        scrollToBottom();
    }

    /**
     * Displays help text in the panel.
     */
    public void showHelpText() {
        container.remove(welcomePanel);
        container.add(helpPanel);
        scrollToBottom();
    }

    /**
     * Adds a filler component to the container.
     *
     * @param name The name of the filler.
     */
    private void addFiller(String name) {
        container.add(new FillerPanel(name));
    }

    /**
     * Adds a user prompt to the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void addUserPrompt(ChatMessageContext chatMessageContext) {
        container.remove(welcomePanel);

        // UserPromptPanel userPromptPanel = new UserPromptPanel(chatMessageContext);

//        if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getStreamMode())) {
//            waitingPanel.showMsg();
//            userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);
//        }

        addFiller(chatMessageContext.getId());
        // container.add(userPromptPanel);
        scrollToBottom();
    }

    /**
     * Adds a chat response to the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void addChatResponse(@NotNull ChatMessageContext chatMessageContext) {
        waitingPanel.hideMsg();
        addFiller(chatMessageContext.getId());

        // Special handling for find command
        if (FIND_COMMAND.equals(chatMessageContext.getCommandName()) &&
            chatMessageContext.getSemanticReferences() != null &&
            !chatMessageContext.getSemanticReferences().isEmpty()) {
            container.add(new FindResultsPanel(project, chatMessageContext.getSemanticReferences()));
        } else {
            container.add(new ChatResponsePanel(chatMessageContext));
        }

        container.revalidate();
        container.repaint();
        ApplicationManager.getApplication().invokeLater(this::scrollToBottom);
    }

    /**
     * Adds a streaming response to the panel.
     *
     * @param chatResponseStreamingPanel The streaming response panel.
     */
    public void addStreamResponse(ChatStreamingResponsePanel chatResponseStreamingPanel) {
        container.add(chatResponseStreamingPanel);
        scrollToBottom();
    }

    /**
     * Adds file references from a streaming response to the panel.
     *
     * @param fileListPanel The panel containing file references.
     */
    public void addStreamFileReferencesResponse(ExpandablePanel fileListPanel) {
        container.add(fileListPanel);
        scrollToBottom();
    }

    /**
     * Removes the last user prompt from the panel.
     *
     * @param chatMessageContext The context of the chat message.
     */
    public void removeLastUserPrompt(ChatMessageContext chatMessageContext) {
        for (Component component : container.getComponents()) {
            if (component instanceof UserPromptPanel && component.getName().equals(chatMessageContext.getId())) {
                container.remove(component);
                break;
            }
        }
        revalidate();
        repaint();
        scrollToBottom();
    }

    /**
     * Scrolls the view to the bottom of the scroll pane.
     */
    private void scrollToBottom() {
        ApplicationManager.getApplication().invokeLater(() -> {
            Timer timer = new Timer(100, e -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    /**
     * Updates the help text displayed in the help panel.
     */
    public void updateHelpText() {
        helpPanel.updateHelpText(HelpUtil.getHelpMessage());
    }

    /**
     * Displays an entire conversation in the panel.
     *
     * @param conversation The conversation to be displayed.
     */
    public void displayConversation(Conversation conversation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String conversationId = UUID.randomUUID().toString();
            for (ChatMessage message : conversation.getMessages()) {
                conversation.setId(conversationId);
                ChatMessageContext chatMessageContext = createChatMessageContext(conversation, message);
                if (!message.isUser()) {
                    addChatResponse(chatMessageContext);
                }
            }
            scrollToBottom();
        });
    }

    /**
     * Creates a ChatMessageContext from a conversation and a chat message.
     *
     * @param conversation The conversation containing the message.
     * @param message      The chat message.
     * @return The created ChatMessageContext.
     */
    private ChatMessageContext createChatMessageContext(@NotNull Conversation conversation,
                                                        @NotNull ChatMessage message) {
        return ChatMessageContext.builder()
                .id(conversation.getId())
                .project(project)
                .userPrompt(message.isUser() ? message.getContent() : "")
                .aiMessage(message.isUser() ? null : AiMessage.aiMessage(message.getContent()))
                .executionTimeMs(conversation.getExecutionTimeMs())
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.valueOf(conversation.getLlmProvider()))
                        .modelName(conversation.getModelName())
                        .apiKeyUsed(conversation.getApiKeyUsed())
                        .inputCost(conversation.getInputCost() == null ? 0 : conversation.getInputCost())
                        .outputCost(conversation.getOutputCost() == null ? 0 : conversation.getOutputCost())
                        .inputMaxTokens(conversation.getContextWindow() == null ? 0 : conversation.getContextWindow())
                        .build())
                .cost(0).build();
    }

    /**
     * Called when custom prompts have changed. Updates the help text accordingly.
     */
    @Override
    public void onCustomPromptsChanged() {
        ApplicationManager.getApplication().invokeLater(() -> {
            updateHelpText();
            // Also notify the welcome panel about the changes to update the prompt list
            welcomePanel.onCustomPromptsChanged();
        });
    }

    @Override
    public void onMCPLoggingMessage(@NotNull MCPMessage message) {
        if (message.getType().equals(MCPType.AI_MSG)) {

            // Parse and render markdown content
            String markdownContent = "⦿ " + message.getContent();
            
            // Create parser and renderer for markdown
            Parser parser = Parser.builder().build();
            HtmlRenderer renderer = HtmlRenderer
                .builder()
                .nodeRendererFactory(context -> {
                    AtomicReference<CodeBlockNodeRenderer> codeBlockRenderer = new AtomicReference<>();
                    ApplicationManager.getApplication().runReadAction((Computable<CodeBlockNodeRenderer>) () ->
                            codeBlockRenderer.getAndSet(new CodeBlockNodeRenderer(project, context))
                    );
                    return codeBlockRenderer.get();
                })
                .escapeHtml(true)
                .build();
            
            // Convert markdown to HTML
            String htmlContent = renderer.render(parser.parse(markdownContent));

            // Create and configure the editor pane
            JEditorPane htmlJEditorPane = new JEditorPane();
            htmlJEditorPane.setContentType("text/html");
            htmlJEditorPane.setEditable(false);
            
            // Set up HTML editor kit with proper styling
            HTMLEditorKitBuilder htmlEditorKitBuilder =
                new HTMLEditorKitBuilder()
                    .withWordWrapViewFactory()
                    .withFontResolver(EditorCssFontResolver.getGlobalInstance());

            HTMLEditorKit editorKit = htmlEditorKitBuilder.build();
            editorKit.getStyleSheet().addStyleSheet(StyleSheetsFactory.createParagraphStyleSheet());
            htmlJEditorPane.setEditorKit(editorKit);
            
            // Set background and foreground colors
            htmlJEditorPane.setBackground(PROMPT_BG_COLOR);
            htmlJEditorPane.setForeground(PROMPT_TEXT_COLOR);
            
            // Set the HTML content
            htmlJEditorPane.setText(htmlContent);
            
            // Add to container and update UI
            container.add(htmlJEditorPane);
            container.revalidate();
            container.repaint();
            ApplicationManager.getApplication().invokeLater(this::scrollToBottom);
        }
    }
}
