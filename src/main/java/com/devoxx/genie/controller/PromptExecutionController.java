package com.devoxx.genie.controller;

import com.devoxx.genie.controller.listener.PromptExecutionListener;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatPromptExecutor;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.util.concurrent.atomic.AtomicBoolean;

public class PromptExecutionController implements PromptExecutionListener {

    private final Project project;
    private final ChatPromptExecutor chatPromptExecutor;
    private final PromptInputArea promptInputArea;
    private final PromptOutputPanel promptOutputPanel;
    private final ActionButtonsPanel actionButtonsPanel;
    private boolean isPromptRunning = false;
    private ChatMessageContext currentChatMessageContext;

    public PromptExecutionController(Project project,
                                     PromptInputArea promptInputArea,
                                     PromptOutputPanel promptOutputPanel,
                                     ActionButtonsPanel actionButtonsPanel) {
        this.project = project;
        this.promptInputArea = promptInputArea;
        this.promptOutputPanel = promptOutputPanel;
        this.chatPromptExecutor = new ChatPromptExecutor(promptInputArea);
        this.actionButtonsPanel = actionButtonsPanel;
    }

    public boolean isPromptRunning() {
        return isPromptRunning;
    }

    public boolean handlePromptSubmission(ChatMessageContext currentChatMessageContext) {
        this.currentChatMessageContext = currentChatMessageContext;

        if (isPromptRunning) {
            stopPromptExecution();
            return true;
        }

        startPromptExecution();

        AtomicBoolean response = new AtomicBoolean(true);
        chatPromptExecutor.updatePromptWithCommandIfPresent(currentChatMessageContext, promptOutputPanel)
                .ifPresentOrElse(
                        this::executePromptWithContext,
                        () -> response.set(false) // TODO Throw exception instead of returning false
                );

        return response.get();
    }

    private void executePromptWithContext(String command) {
        chatPromptExecutor.executePrompt(currentChatMessageContext, promptOutputPanel, () -> {
            endPromptExecution();
            ApplicationManager.getApplication().invokeLater(() -> {
                promptInputArea.clear();
                promptInputArea.requestInputFocus();
            });
        });
    }

    @Override
    public void stopPromptExecution() {
        chatPromptExecutor.stopPromptExecution(project);
        endPromptExecution();
    }

    @Override
    public void startPromptExecution() {
        isPromptRunning = true;
        actionButtonsPanel.disableSubmitBtn();
        actionButtonsPanel.disableButtons();
        actionButtonsPanel.startGlowing();
    }

    @Override
    public void endPromptExecution() {
        isPromptRunning = false;
        actionButtonsPanel.enableButtons();
    }
}
