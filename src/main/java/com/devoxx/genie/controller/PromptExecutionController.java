package com.devoxx.genie.controller;

import com.devoxx.genie.controller.listener.PromptExecutionListener;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.PromptExecutionService;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.service.prompt.steering.PendingPromptQueue;
import com.devoxx.genie.service.prompt.steering.SteeringMessageQueue;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class PromptExecutionController implements PromptExecutionListener {

    private final Project project;
    private final PromptExecutionService promptExecutionService;
    private final PromptCommandProcessor commandProcessor;
    private final PromptInputArea promptInputArea;
    private final PromptOutputPanel promptOutputPanel;
    private final ActionButtonsPanel actionButtonsPanel;
    @Getter
    private boolean isPromptRunning = false;
    private long currentExecutionId = 0;
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

    public boolean handlePromptSubmission(ChatMessageContext currentChatMessageContext) {
        if (isPromptRunning) {
            // Issue #1241: while a task is running, a non-blank submission is QUEUED
            // as an independent prompt (the default) and executed after the current
            // run completes. Mid-loop steering is a separate, explicit action via
            // steerRunningPrompt. A blank submission still stops the run.
            String text = currentChatMessageContext != null ? currentChatMessageContext.getUserPrompt() : null;
            if (queueRunningPrompt(text)) {
                return true;
            }
            stopPromptExecution();
            return true;
        }

        this.currentChatMessageContext = currentChatMessageContext;
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
                    // Issue #1241 follow-up: clear on send, not on completion. The prompt now
                    // has its own bubble, and the input stays usable while the task runs (Queue
                    // and Steer) — leaving the text behind invites sending the same prompt twice.
                    promptInputArea.clear();
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

        // Capture execution ID so the completion callback can detect if it's stale.
        // When the task runner advances to the next task, it stops the current execution
        // and starts a new one. The old completion callback must not interfere.
        final long myExecutionId = currentExecutionId;
        promptExecutionService.executePrompt(
                currentChatMessageContext,
                promptOutputPanel,
                () -> {
                    if (myExecutionId != currentExecutionId) {
                        return; // Stale callback from a previous execution; ignore
                    }
                    endPromptExecution();
                    // Do NOT clear here: the input was already cleared on send, so anything
                    // present now is a message the user is typing while the task runs.
                    promptInputArea.requestInputFocus();
                });
    }

    /**
     * Issue #1241: queue a mid-task steering message for the running execution.
     * Public raw-text entry point: the Enter key submits via the message-bus route,
     * which only carries the prompt text. Returns false when steering is not possible
     * (not running, blank text, no running context, or no active steering consumer) —
     * the caller then falls back to its previous behavior (stop or queue).
     */
    public boolean steerRunningPrompt(String text) {
        if (!isPromptRunning || text == null || text.isBlank() || currentChatMessageContext == null) {
            return false;
        }
        String memoryKey = currentChatMessageContext.getMemoryKey();
        if (!SteeringMessageQueue.getInstance().isActive(memoryKey)) {
            return false;
        }
        String trimmed = text.trim();
        SteeringMessageQueue.getInstance().offer(memoryKey, trimmed);
        promptOutputPanel.getConversationPanel().addSteeringMessage(trimmed);
        promptInputArea.clear();
        return true;
    }

    /**
     * Issue #1241 (queue mode — the default): queue an independent prompt submitted
     * while a task is running. It gets a user bubble immediately and is executed as
     * its own prompt after the current run completes — never injected mid-loop.
     * Returns false when queueing is not possible (not running, blank, no context).
     */
    public boolean queueRunningPrompt(String text) {
        if (!isPromptRunning || text == null || text.isBlank() || currentChatMessageContext == null) {
            return false;
        }
        String trimmed = text.trim();
        PendingPromptQueue.getInstance().offer(currentChatMessageContext.getMemoryKey(), trimmed);
        promptOutputPanel.getConversationPanel().addQueuedPromptMessage(trimmed);
        promptInputArea.clear();
        return true;
    }

    @Override
    public void stopPromptExecution() {
        // Stop execution for this tab only if we have a context with tabId
        if (currentChatMessageContext != null && currentChatMessageContext.getTabId() != null) {
            promptExecutionService.stopExecution(project, currentChatMessageContext.getTabId());
        } else {
            promptExecutionService.stopExecution(project);
        }
        // Issue #1241: the user aborted the run — unconsumed steering messages
        // are corrections to work that no longer continues, and queued prompts
        // should not start either. Discard both; queued bubbles are removed so
        // the conversation doesn't show questions that will never be answered.
        if (currentChatMessageContext != null) {
            String memoryKey = currentChatMessageContext.getMemoryKey();
            SteeringMessageQueue.getInstance().drainAndDeactivate(memoryKey);
            for (String queued : PendingPromptQueue.getInstance().drain(memoryKey)) {
                promptOutputPanel.getConversationPanel().removeSteeringMessage(queued);
            }
        }
        endPromptExecution();
    }

    @Override
    public void startPromptExecution() {
        isPromptRunning = true;
        currentExecutionId++;
        actionButtonsPanel.disableSubmitBtn();
        actionButtonsPanel.disableButtons();
        actionButtonsPanel.startGlowing();
    }

    @Override
    public void endPromptExecution() {
        isPromptRunning = false;
        // enableButtons() BEFORE resubmitting: it schedules stopGlowing (and button
        // resets) on the EDT. If the queued/leftover prompt were published first, the
        // new run's synchronous startGlowing would land before the old run's pending
        // stopGlowing — which would then kill the fresh glow (and reset the stop icon).
        actionButtonsPanel.enableButtons();
        resubmitUnconsumedSteeringMessages();
    }

    /**
     * Issue #1241: when a run ends, (1) steering messages the agent loop never
     * consumed must not be silently lost — resubmit them as a new prompt; else
     * (2) start the next QUEUED prompt, one per run, so queued questions execute
     * sequentially, each as its own prompt with its own answer.
     */
    private void resubmitUnconsumedSteeringMessages() {
        if (currentChatMessageContext == null) {
            return;
        }
        String memoryKey = currentChatMessageContext.getMemoryKey();
        List<String> leftovers = SteeringMessageQueue.getInstance().drainAndDeactivate(memoryKey);
        String nextPrompt;
        if (!leftovers.isEmpty()) {
            nextPrompt = String.join("\n\n", leftovers);
        } else {
            nextPrompt = PendingPromptQueue.getInstance().pollNext(memoryKey);
            if (nextPrompt == null) {
                return;
            }
            leftovers = List.of(nextPrompt);
        }
        // The resubmitted prompt renders its own user bubble — drop the stale
        // steering/queued bubbles or the same question shows twice.
        for (String leftover : leftovers) {
            promptOutputPanel.getConversationPanel().removeSteeringMessage(leftover);
        }
        project.getMessageBus().syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)
                .onPromptSubmitted(project, nextPrompt, currentChatMessageContext.getTabId());
    }
}
