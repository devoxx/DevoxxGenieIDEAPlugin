package com.devoxx.genie.ui.trace;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.trace.AgentTraceListener;
import com.devoxx.genie.service.trace.TraceSettings;
import com.devoxx.genie.ui.compose.ComposeConversationViewController;
import com.devoxx.genie.ui.compose.model.AgentRunUiState;
import com.devoxx.genie.ui.trace.view.AgentRunViewer;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Drives one chat tab's "View agent run" toolbar button (TASK-284): shows it while agent trace
 * recording is on, enables it once the tab's conversation has a recorded trace, and opens the
 * run view on click. Saving and uploading the trace happen from that view's Export menu. State
 * is recomputed off the EDT when a trace turn is written, the settings change, or the tab
 * switches conversation ({@link #refresh()}); the toolbar itself disables the button while a
 * response is in flight.
 */
@Slf4j
public final class AgentRunButtonController {

    private final Project project;
    private final ComposeConversationViewController view;
    private final Supplier<@Nullable Conversation> currentConversation;
    private final MessageBusConnection connection;

    public AgentRunButtonController(@NotNull Project project, @NotNull ComposeConversationViewController view,
                                    @NotNull Supplier<@Nullable Conversation> currentConversation) {
        this.project = project;
        this.view = view;
        this.currentConversation = currentConversation;
        this.connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(AgentTraceListener.TOPIC, new AgentTraceListener() {
            @Override
            public void traceWritten(@NotNull String conversationId) {
                Conversation current = currentConversation.get();
                if (current != null && conversationId.equals(current.getId())) {
                    refresh();
                }
            }

            @Override
            public void settingsChanged() {
                refresh();
            }
        });
        view.setOnViewAgentRunClick(this::onClick);
        refresh();
    }

    /**
     * The button state: hidden while recording is off; otherwise enabled only when the
     * conversation has been saved and has trace rows. {@code hasTraceEvents} is only asked
     * when it matters.
     */
    static @NotNull AgentRunUiState stateFor(boolean recordingOn, @Nullable Conversation conversation,
                                             @NotNull Predicate<String> hasTraceEvents) {
        if (!recordingOn) {
            return AgentRunUiState.HIDDEN;
        }
        boolean hasTrace = conversation != null && conversation.getId() != null
                && hasTraceEvents.test(conversation.getId());
        return new AgentRunUiState(true, hasTrace);
    }

    /** Recomputes the button state in the background and pushes it to the view on the EDT. */
    public void refresh() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            AgentRunUiState state;
            try {
                state = stateFor(TraceSettings.current().enabled(), currentConversation.get(),
                        id -> ConversationStorageService.getInstance().hasTraceEvents(id));
            } catch (RuntimeException e) {
                log.debug("Could not refresh the agent run button: {}", e.toString());
                state = AgentRunUiState.HIDDEN;
            }
            AgentRunUiState finalState = state;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    view.setAgentRunState(finalState);
                }
            });
        });
    }

    private void onClick() {
        Conversation conversation = currentConversation.get();
        if (conversation == null) {
            // Never fail silently: say why there is nothing to show yet.
            NotificationUtil.sendNotification(project,
                    "No agent run yet: this conversation hasn't been saved. Its run can be viewed once a response has completed.");
            refresh();
            return;
        }
        AgentRunViewer.open(project, conversation);
    }

    public void dispose() {
        connection.disconnect();
    }
}
