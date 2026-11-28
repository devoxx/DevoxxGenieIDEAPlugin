package com.devoxx.genie.ui.trace;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.ui.compose.model.AgentRunUiState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/** TASK-284: when the chat toolbar's "View agent run" button is hidden, disabled or enabled. */
class AgentRunButtonControllerTest {

    private final List<String> asked = new ArrayList<>();

    private Predicate<String> tracesFor(String... ids) {
        Set<String> withTrace = Set.of(ids);
        return id -> {
            asked.add(id);
            return withTrace.contains(id);
        };
    }

    private static Conversation conversation(String id) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        return conversation;
    }

    @Test
    void hiddenWhileRecordingIsOff_withoutQueryingTheDatabase() {
        assertThat(AgentRunButtonController.stateFor(false, conversation("c1"), tracesFor("c1")))
                .isEqualTo(AgentRunUiState.HIDDEN);
        assertThat(asked).isEmpty();
    }

    @Test
    void disabledBeforeTheConversationIsSaved() {
        assertThat(AgentRunButtonController.stateFor(true, null, tracesFor("c1")))
                .isEqualTo(new AgentRunUiState(true, false));
        assertThat(AgentRunButtonController.stateFor(true, conversation(null), tracesFor("c1")))
                .isEqualTo(new AgentRunUiState(true, false));
        assertThat(asked).isEmpty();
    }

    @Test
    void disabledWhileTheConversationHasNoTraceRows() {
        assertThat(AgentRunButtonController.stateFor(true, conversation("c2"), tracesFor("c1")))
                .isEqualTo(new AgentRunUiState(true, false));
        assertThat(asked).containsExactly("c2");
    }

    @Test
    void enabledOnceTheConversationHasTraceRows() {
        assertThat(AgentRunButtonController.stateFor(true, conversation("c1"), tracesFor("c1")))
                .isEqualTo(new AgentRunUiState(true, true));
    }
}
