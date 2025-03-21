package com.devoxx.genie.controller;

import com.devoxx.genie.controller.listener.PromptExecutionListener;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.PromptExecutionService;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class PromptExecutionController implements PromptExecutionListener {

    private final Project project;
    private final PromptExecutionService promptExecutionService;
    private final PromptCommandProcessor commandProcessor;
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
        this.promptExecutionService = PromptExecutionService.getInstance(project);
        this.commandProcessor = PromptCommandProcessor.getInstance();
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
        Optional<String> processedPrompt = commandProcessor.processCommands(currentChatMessageContext, promptOutputPanel);
        
        processedPrompt.ifPresentOrElse(
                command -> executePromptWithContext(),
                () -> {
                    // Command handling indicated execution should stop
                    response.set(false);
                    endPromptExecution();
                }
        );

        return response.get();
    }

    private void executePromptWithContext() {
        promptExecutionService.executePrompt(
                currentChatMessageContext, 
                promptOutputPanel, 
                () -> {
                    endPromptExecution();
                    promptInputArea.clear();
                    promptInputArea.requestInputFocus();
                });
    }

    @Override
    public void stopPromptExecution() {
        promptExecutionService.stopExecution(project);
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
