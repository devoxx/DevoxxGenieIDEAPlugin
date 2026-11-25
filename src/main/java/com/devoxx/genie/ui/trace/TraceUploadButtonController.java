package com.devoxx.genie.ui.trace;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.trace.AgentTraceListener;
import com.devoxx.genie.service.trace.TraceSettings;
import com.devoxx.genie.ui.compose.ComposeConversationViewController;
import com.devoxx.genie.ui.compose.model.TraceUploadUiState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Drives one chat tab's "Upload to Hugging Face" toolbar button (task-280): shows it while
 * agent trace recording is on, enables it once the tab's conversation has a recorded trace,
 * and runs the upload on click. State is recomputed off the EDT when a trace turn is written,
 * the settings change, or the tab switches conversation ({@link #refresh()}).
 */
@Slf4j
public final class TraceUploadButtonController {

    private final Project project;
    private final ComposeConversationViewController view;
    private final Supplier<@Nullable Conversation> currentConversation;
    private final MessageBusConnection connection;
    private volatile boolean uploading;

    public TraceUploadButtonController(@NotNull Project project, @NotNull ComposeConversationViewController view,
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
        view.setOnTraceUploadClick(this::onClick);
        refresh();
    }

    /** Recomputes the button state in the background and pushes it to the view on the EDT. */
    public void refresh() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            TraceUploadUiState state;
            try {
                boolean visible = TraceSettings.current().enabled();
                Conversation conversation = currentConversation.get();
                boolean hasTrace = visible && conversation != null && conversation.getId() != null
                        && ConversationStorageService.getInstance().hasTraceEvents(conversation.getId());
                state = new TraceUploadUiState(visible, hasTrace, uploading);
            } catch (RuntimeException e) {
                log.debug("Could not refresh the trace upload button: {}", e.toString());
                state = TraceUploadUiState.HIDDEN;
            }
            TraceUploadUiState finalState = state;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    view.setTraceUploadState(finalState);
                }
            });
        });
    }

    private void onClick() {
        Conversation conversation = currentConversation.get();
        if (conversation == null || uploading) {
            return;
        }
        HfTraceUploader.upload(project, List.of(conversation),
                () -> {
                    uploading = true;
                    refresh();
                },
                () -> {
                    uploading = false;
                    refresh();
                });
    }

    public void dispose() {
        connection.disconnect();
    }
}
