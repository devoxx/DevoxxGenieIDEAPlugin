package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.HelpUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.AiMessage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public class PromptOutputPanel extends JBPanel<PromptOutputPanel> implements CustomPromptChangeListener {

    private final Project project;
    private final ResourceBundle resourceBundle;

    private final JPanel container = new JPanel();
    private final WelcomePanel welcomePanel;
    private final HelpPanel helpPanel;
    private final WaitingPanel waitingPanel = new WaitingPanel();
    private final JBScrollPane scrollPane;


    public PromptOutputPanel(Project project, ResourceBundle resourceBundle) {
        super();

        this.project = project;
        this.resourceBundle = resourceBundle;
        welcomePanel = new WelcomePanel(resourceBundle);
        helpPanel = new HelpPanel(HelpUtil.getHelpMessage(resourceBundle));

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        scrollPane = new JBScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(200, 200)); // Adjust these values as needed
        showWelcomeText();
    }

    public void clear() {
        container.removeAll();
        showWelcomeText();
        scrollToBottom();
    }

    public void showWelcomeText() {
        welcomePanel.showMsg();
        container.add(welcomePanel);
        scrollToBottom();
    }

    public void showHelpText() {
        container.remove(welcomePanel);
        container.add(helpPanel);
        scrollToBottom();
    }

    private void addFiller(String name) {
        container.add(new FillerPanel(name));
    }

    public void addUserPrompt(ChatMessageContext chatMessageContext) {
        container.remove(welcomePanel);

        UserPromptPanel userPromptPanel = new UserPromptPanel(container, chatMessageContext);

        if (!DevoxxGenieStateService.getInstance().getStreamMode()) {
            waitingPanel.showMsg();
            userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);
        }

        addFiller(chatMessageContext.getId());
        container.add(userPromptPanel);
        scrollToBottom();
    }

    public void addChatResponse(@NotNull ChatMessageContext chatMessageContext) {
        waitingPanel.hideMsg();
        addFiller(chatMessageContext.getId());
        container.add(new ChatResponsePanel(chatMessageContext));
        scrollToBottom();
    }

    public void addStreamResponse(ChatStreamingResponsePanel chatResponseStreamingPanel) {
        container.add(chatResponseStreamingPanel);
        scrollToBottom();
    }

    public void addStreamFileReferencesResponse(ExpandablePanel fileListPanel) {
        container.add(fileListPanel);
        scrollToBottom();
    }

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

    public void updateHelpText() {
        helpPanel.updateHelpText(HelpUtil.getHelpMessage(resourceBundle));
    }

    public void displayConversation(Project project, Conversation conversation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String conversationId = UUID.randomUUID().toString();
            for (ChatMessage message : conversation.getMessages()) {
                conversation.setId(conversationId);
                ChatMessageContext chatMessageContext = createChatMessageContext(project, conversation, message);
                if (message.isUser()) {
                    addUserPrompt(chatMessageContext);
                } else {
                    addChatResponse(chatMessageContext);
                }
            }
            scrollToBottom();
        });
    }

    private ChatMessageContext createChatMessageContext(Project project,
                                                        @NotNull Conversation conversation,
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

    @Override
    public void onCustomPromptsChanged() {
        // Update the help panel or any other UI components that display custom prompts
        ApplicationManager.getApplication().invokeLater(this::updateHelpText);
    }

}
