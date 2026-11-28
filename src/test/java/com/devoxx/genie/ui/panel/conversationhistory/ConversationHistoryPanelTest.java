package com.devoxx.genie.ui.panel.conversationhistory;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.devoxx.genie.ui.panel.conversationhistory.ConversationHistoryPanel.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Row actions and time text of the conversation history list (TASK-283). */
class ConversationHistoryPanelTest {

    @Test
    void actionsAreHiddenOnInactiveRows() {
        for (int column : new int[]{COL_VIEW_RUN, COL_EXPORT, COL_UPLOAD, COL_DELETE}) {
            assertThat(isActionShown(column, false, true, true)).as("column %d", column).isFalse();
        }
    }

    @Test
    void activeRowShowsEveryActionWhenItHasATrace() {
        for (int column : new int[]{COL_VIEW_RUN, COL_EXPORT, COL_UPLOAD, COL_DELETE}) {
            assertThat(isActionShown(column, true, true, true)).as("column %d", column).isTrue();
        }
    }

    @Test
    void viewRunNeedsARecordedTrace() {
        assertThat(isActionShown(COL_VIEW_RUN, true, true, false)).isFalse();
        assertThat(isActionShown(COL_EXPORT, true, true, false)).isTrue();
        assertThat(isActionShown(COL_UPLOAD, true, true, false)).isTrue();
    }

    @Test
    void onlyDeleteShowsWhileTraceRecordingIsOff() {
        assertThat(isActionShown(COL_DELETE, true, false, true)).isTrue();
        assertThat(isActionShown(COL_VIEW_RUN, true, false, true)).isFalse();
        assertThat(isActionShown(COL_EXPORT, true, false, true)).isFalse();
        assertThat(isActionShown(COL_UPLOAD, true, false, true)).isFalse();
    }

    @Test
    void titleAndTimeAreNeverActions() {
        assertThat(isActionShown(COL_TITLE, true, true, true)).isFalse();
        assertThat(isActionShown(COL_TIME, true, true, true)).isFalse();
        assertThat(isActionShown(-1, true, true, true)).isFalse();
    }

    @Test
    void timeSinceIsCompact() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 28, 11, 0);
        assertThat(formatTimeSince(now.minusSeconds(30), now)).isEqualTo("Just now");
        assertThat(formatTimeSince(now.minusMinutes(1), now)).isEqualTo("1m ago");
        assertThat(formatTimeSince(now.minusMinutes(30), now)).isEqualTo("30m ago");
        assertThat(formatTimeSince(now.minusMinutes(59), now)).isEqualTo("59m ago");
        assertThat(formatTimeSince(now.minusHours(1), now)).isEqualTo("1h ago");
        assertThat(formatTimeSince(now.minusHours(23), now)).isEqualTo("23h ago");
        assertThat(formatTimeSince(now.minusDays(3), now)).isEqualTo("3d ago");
    }
}
