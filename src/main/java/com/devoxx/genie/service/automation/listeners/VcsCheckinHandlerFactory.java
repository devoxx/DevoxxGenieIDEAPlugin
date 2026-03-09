package com.devoxx.genie.service.automation.listeners;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Factory registered in plugin.xml that creates {@link VcsCommitListener}
 * instances for each commit operation.
 */
public class VcsCheckinHandlerFactory extends CheckinHandlerFactory {

    @Override
    public @NotNull CheckinHandler createHandler(@NotNull CheckinProjectPanel panel,
                                                  @NotNull CommitContext commitContext) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        if (!Boolean.TRUE.equals(state.getEventAutomationEnabled())) {
            return CheckinHandler.DUMMY;
        }
        return new VcsCommitListener(
                panel.getProject(),
                new ArrayList<>(panel.getSelectedChanges())
        );
    }
}
