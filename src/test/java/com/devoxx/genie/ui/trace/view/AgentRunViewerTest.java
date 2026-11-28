package com.devoxx.genie.ui.trace.view;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.trace.view.AgentRun;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** TASK-284: reopening a conversation's run view replaces its existing tab instead of adding one. */
class AgentRunViewerTest {

    private static final AgentRun EMPTY_RUN =
            new AgentRun(new AgentRun.Summary(List.of(), 0, 0, 0, 0, 0, 0, Set.of(), false), List.of());

    private static AgentRunFile runTab(String conversationId) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setTitle("Conversation " + conversationId);
        return new AgentRunFile(conversation, EMPTY_RUN);
    }

    @Test
    void findsEveryOpenRunTabOfTheConversation() {
        AgentRunFile first = runTab("c1");
        AgentRunFile second = runTab("c1");
        AgentRunFile other = runTab("c2");
        VirtualFile source = new LightVirtualFile("Main.java");

        List<VirtualFile> tabs = AgentRunViewer.runTabsOf(new VirtualFile[]{first, source, other, second}, "c1");

        assertThat(tabs).containsExactly(first, second);
    }

    @Test
    void findsNothingWhenTheConversationHasNoRunTab() {
        assertThat(AgentRunViewer.runTabsOf(new VirtualFile[]{runTab("c2"), new LightVirtualFile("a.txt")}, "c1"))
                .isEmpty();
        assertThat(AgentRunViewer.runTabsOf(new VirtualFile[0], "c1")).isEmpty();
    }
}
