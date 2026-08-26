package com.devoxx.genie.ui.panel.log;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression coverage for the bounded, bulk-updated Activity Log list (TASK-260). */
class AgentMcpLogPanelPerformanceTest {

    @Test
    void listRowPreview_collapsesLinesAndKeepsWithinItsTotalLimit() {
        String preview = AgentMcpLogPanel.formatForListRow("first\r\nsecond\nthird");

        assertThat(preview).isEqualTo("first ↵ second ↵ third");
        assertThat(preview).doesNotContain("\n", "\r");
        assertThat(AgentMcpLogPanel.formatForListRow("output\n\n")).isEqualTo("output");
    }

    @Test
    void listRowPreview_stopsAtBoundForVeryLargeMcpPayload() {
        String preview = AgentMcpLogPanel.formatForListRow("x".repeat(1_000_000));

        assertThat(preview).hasSize(500).endsWith("…");
    }

    @Test
    void listModel_appendsAndPrunesUsingOneEventPerBulkOperation() {
        AgentMcpLogPanel.ActivityLogListModel<String> model =
                new AgentMcpLogPanel.ActivityLogListModel<>();
        model.addAll(IntStream.range(0, 1_000).mapToObj(i -> "old-" + i).toList());
        AtomicInteger additions = new AtomicInteger();
        AtomicInteger removals = new AtomicInteger();
        model.addListDataListener(countingListener(additions, removals));

        List<String> incoming = IntStream.range(0, 20).mapToObj(i -> "new-" + i).toList();
        model.appendAllAndRemoveFirst(incoming, 20);

        assertThat(model.size()).isEqualTo(1_000);
        assertThat(Collections.list(model.elements()))
                .startsWith("old-20", "old-21")
                .endsWith("new-18", "new-19");
        assertThat(removals).hasValue(1);
        assertThat(additions).hasValue(1);
    }

    @Test
    void listModel_replacesFilteredContentsUsingBulkEvents() {
        AgentMcpLogPanel.ActivityLogListModel<String> model =
                new AgentMcpLogPanel.ActivityLogListModel<>();
        model.addAll(List.of("mcp-1", "agent-1", "mcp-2"));
        AtomicInteger additions = new AtomicInteger();
        AtomicInteger removals = new AtomicInteger();
        model.addListDataListener(countingListener(additions, removals));

        model.replaceAll(List.of("mcp-1", "mcp-2"));

        assertThat(Collections.list(model.elements())).containsExactly("mcp-1", "mcp-2");
        assertThat(removals).hasValue(1);
        assertThat(additions).hasValue(1);
    }

    @Test
    void retention_countsOnlyRemovedEntriesVisibleUnderCurrentFilter() {
        List<String> fullLogs = new ArrayList<>(List.of("mcp-1", "agent-1", "mcp-2", "agent-2"));

        int visibleRemoved = AgentMcpLogPanel.trimOldestAndCountMatching(
                fullLogs, 3, entry -> entry.startsWith("mcp"));

        assertThat(visibleRemoved).isEqualTo(2);
        assertThat(fullLogs).containsExactly("agent-2");
    }

    @Test
    void tailFollowing_requiresVisibleListAtItsBottom() {
        DefaultBoundedRangeModel atBottom = new DefaultBoundedRangeModel(90, 10, 0, 100);
        DefaultBoundedRangeModel scrolledUp = new DefaultBoundedRangeModel(50, 10, 0, 100);

        assertThat(AgentMcpLogPanel.isFollowingTail(true, false, atBottom)).isTrue();
        assertThat(AgentMcpLogPanel.isFollowingTail(true, false, scrolledUp)).isFalse();
        assertThat(AgentMcpLogPanel.isFollowingTail(false, false, atBottom)).isFalse();
        assertThat(AgentMcpLogPanel.isFollowingTail(true, true, scrolledUp)).isTrue();
    }

    private static ListDataListener countingListener(AtomicInteger additions, AtomicInteger removals) {
        return new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                additions.incrementAndGet();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                removals.incrementAndGet();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                // No-op: bulk add/remove should not degrade into per-row content changes.
            }
        };
    }
}
