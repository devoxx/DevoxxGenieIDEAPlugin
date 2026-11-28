package com.devoxx.genie.ui.trace.view;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.trace.view.AgentRun;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory file that opens one conversation's agent run in an editor tab (TASK-283). The run is
 * built before the file is opened; {@link AgentRunEditorProvider} shows it as {@link AgentRunPanel}.
 */
public final class AgentRunFile extends LightVirtualFile {

    private static final int TITLE_LIMIT = 40;

    private final Conversation conversation;
    private final AgentRun run;

    AgentRunFile(@NotNull Conversation conversation, @NotNull AgentRun run) {
        super(tabName(conversation));
        this.conversation = conversation;
        this.run = run;
        setWritable(false);
    }

    @NotNull Conversation conversation() {
        return conversation;
    }

    @NotNull AgentRun run() {
        return run;
    }

    /** The editor tab title: "Agent run: <conversation title>", without characters a file name can't hold. */
    static @NotNull String tabName(@NotNull Conversation conversation) {
        String title = conversation.getTitle() == null || conversation.getTitle().isBlank()
                ? conversation.getId() : conversation.getTitle();
        String clean = AgentRunText.oneLine(title.replaceAll("[\\\\/:*?\"<>|]", " "), TITLE_LIMIT);
        return "Agent run: " + clean;
    }
}
