package com.devoxx.genie.service.automation.listeners;

import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import com.devoxx.genie.service.automation.EventAutomationService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * VCS checkin handler that fires BEFORE_COMMIT automations with the staged diff.
 * <p>
 * Returns {@link ReturnResult#COMMIT} so it never blocks the commit —
 * the agent output is advisory only.
 */
@Slf4j
public class VcsCommitListener extends CheckinHandler {

    private final Project project;
    private final List<Change> changes;

    public VcsCommitListener(@NotNull Project project, @NotNull List<Change> changes) {
        this.project = project;
        this.changes = changes;
    }

    @Override
    public ReturnResult beforeCheckin() {
        if (project.isDisposed() || changes.isEmpty()) {
            return ReturnResult.COMMIT;
        }

        EventAutomationService automationService = EventAutomationService.getInstance();
        if (automationService == null) {
            return ReturnResult.COMMIT;
        }

        List<String> filePaths = new ArrayList<>();
        StringBuilder diffContent = new StringBuilder();

        for (Change change : changes) {
            ContentRevision afterRevision = change.getAfterRevision();
            ContentRevision beforeRevision = change.getBeforeRevision();

            String path = afterRevision != null
                    ? afterRevision.getFile().getPath()
                    : (beforeRevision != null ? beforeRevision.getFile().getPath() : "unknown");
            filePaths.add(path);

            diffContent.append("--- ").append(change.getType().name()).append(": ").append(path).append('\n');

            // Include actual file content for line-level review
            try {
                String beforeContent = beforeRevision != null ? beforeRevision.getContent() : null;
                String afterContent = afterRevision != null ? afterRevision.getContent() : null;

                if (beforeContent != null && afterContent != null) {
                    diffContent.append("BEFORE:\n").append(beforeContent).append('\n');
                    diffContent.append("AFTER:\n").append(afterContent).append('\n');
                } else if (afterContent != null) {
                    // New file
                    diffContent.append("NEW FILE:\n").append(afterContent).append('\n');
                } else if (beforeContent != null) {
                    // Deleted file
                    diffContent.append("DELETED FILE:\n").append(beforeContent).append('\n');
                }
            } catch (com.intellij.openapi.vcs.VcsException e) {
                log.debug("Could not read content for {}: {}", path, e.getMessage());
            }
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("changeCount", String.valueOf(changes.size()));

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.BEFORE_COMMIT)
                .content(diffContent.toString().trim())
                .filePaths(filePaths)
                .metadata(metadata)
                .build();

        automationService.onEvent(project, ctx);

        // Never block the commit
        return ReturnResult.COMMIT;
    }
}
