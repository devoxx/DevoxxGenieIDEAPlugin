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
        
        // Check if this is the first prompt in the conversation - if so, clear the welcome content first
        if (promptOutputPanel.isNewConversation()) {
            // Clear the welcome panel completely before showing the first user message
            promptOutputPanel.getConversationPanel().clearWithoutWelcome();
            
            // Mark the conversation as started (no longer new) after the first prompt
            promptOutputPanel.markConversationAsStarted();
        }
        
        AtomicBoolean response = new AtomicBoolean(true);
        String originalPrompt = currentChatMessageContext.getUserPrompt().trim();
        boolean isHelpCommand = originalPrompt.startsWith("/help");
        Optional<String> processedPrompt = commandProcessor.processCommands(currentChatMessageContext, promptOutputPanel);
        
        processedPrompt.ifPresentOrElse(
                command -> {
                    if (!isHelpCommand) {
                        // Show the resolved prompt (e.g. expanded custom skill), not the raw /command
                        promptOutputPanel.getConversationPanel().addUserPromptMessage(currentChatMessageContext);
                    }
                    executePromptWithContext();
                },
                () -> {
                    // Command handling indicated execution should stop
                    response.set(false);
                    endPromptExecution();
                }
        );

        return response.get();
    }

    private void executePromptWithContext() {
        // Only scroll to bottom for non-first messages
        // The first message should not be scrolled to preserve the spacing below the header
        if (!promptOutputPanel.isNewConversation()) {
            promptOutputPanel.scrollToBottom();
        }
        
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
