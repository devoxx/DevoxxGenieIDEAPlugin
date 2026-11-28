package com.devoxx.genie.ui.trace.view;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.view.AgentRun;
import com.devoxx.genie.service.trace.view.AgentRunModelBuilder;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Opens a conversation's agent run view (TASK-283). */
@Slf4j
public final class AgentRunViewer {

    private AgentRunViewer() {
    }

    /** Loads the trace and builds the view off the EDT, then opens it in an editor tab. Call on the EDT. */
    public static void open(@NotNull Project project, @NotNull Conversation conversation) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            AgentRun run;
            try {
                List<StoredTraceEvent> events = ConversationStorageService.getInstance().getTraceEvents(conversation.getId());
                run = events.isEmpty() ? null : AgentRunModelBuilder.build(events);
            } catch (RuntimeException e) {
                log.warn("Could not load the agent run of conversation {}: {}", conversation.getId(), e.toString());
                ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendWarningNotification(project, "Could not load the agent run: " + e.getMessage()));
                return;
            }
            AgentRun loaded = run;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                if (loaded == null) {
                    NotificationUtil.sendWarningNotification(project,
                            "No agent trace was recorded for this conversation.");
                    return;
                }
                FileEditorManager.getInstance(project).openFile(new AgentRunFile(conversation, loaded), true);
            });
        });
    }
}
