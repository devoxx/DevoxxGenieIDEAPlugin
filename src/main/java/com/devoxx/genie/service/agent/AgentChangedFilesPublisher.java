package com.devoxx.genie.service.agent;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.compose.ConversationViewController;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hands the files an agent run changed to the chat, so the response can offer a post-hoc
 * review (issue #705). Called from both the streaming and non-streaming completion paths.
 */
@Slf4j
public final class AgentChangedFilesPublisher {

    private AgentChangedFilesPublisher() {
    }

    /**
     * Drains the run's recorded changes onto the finished message. Always drains — even when
     * the feature is switched off — so a disabled run cannot leak its snapshots into the next
     * one's review.
     */
    public static void publish(@NotNull ChatMessageContext context,
                               @Nullable ConversationViewController viewController) {
        if (context.getProject() == null) {
            return;
        }

        try {
            List<AgentFileChangeTracker.FileChange> changes =
                    AgentFileChangeTracker.getInstance(context.getProject()).drainInto(context.getId());

            if (changes.isEmpty()
                    || viewController == null
                    || !Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentShowChangedFiles())) {
                return;
            }

            log.debug("Agent run {} changed {} file(s)", context.getId(), changes.size());
            ApplicationManager.getApplication().invokeLater(() ->
                    viewController.addChangedFiles(context, changes));
        } catch (Exception e) {
            log.debug("Could not publish agent changed files", e);
        }
    }
}
