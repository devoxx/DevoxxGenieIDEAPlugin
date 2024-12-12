package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.HelpUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import dev.langchain4j.data.message.AiMessage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
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
public class PromptOutputPanel extends JBPanel<PromptOutputPanel> implements CustomPromptChangeListener {

    private final transient Project project;
    private final transient ResourceBundle resourceBundle;

    private final JPanel container = new JPanel();
    private final WelcomePanel welcomePanel;
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
        this.resourceBundle = resourceBundle;
        welcomePanel = new WelcomePanel(resourceBundle);
        helpPanel = new HelpPanel(HelpUtil.getHelpMessage());

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        scrollPane = new JBScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(200, 200));
        showWelcomeText();
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

        UserPromptPanel userPromptPanel = new UserPromptPanel(container, chatMessageContext);

        if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getStreamMode())) {
            waitingPanel.showMsg();
            userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);
        }

        addFiller(chatMessageContext.getId());
        container.add(userPromptPanel);
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
                if (message.isUser()) {
                    addUserPrompt(chatMessageContext);
                } else {
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
                .totalFileCount(0)
                .executionTimeMs(conversation.getExecutionTimeMs())
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.valueOf(conversation.getLlmProvider()))
                        .modelName(conversation.getModelName())
                        .apiKeyUsed(conversation.getApiKeyUsed())
                        .inputCost(conversation.getInputCost() == null ? 0 : conversation.getInputCost())
                        .outputCost(conversation.getOutputCost() == null ? 0 : conversation.getOutputCost())
                        .contextWindow(conversation.getContextWindow() == null ? 0 : conversation.getContextWindow())
                        .build())
                .cost(0).build();
    }

    /**
     * Called when custom prompts have changed. Updates the help text accordingly.
     */
    @Override
    public void onCustomPromptsChanged() {
        ApplicationManager.getApplication().invokeLater(this::updateHelpText);
    }
}
