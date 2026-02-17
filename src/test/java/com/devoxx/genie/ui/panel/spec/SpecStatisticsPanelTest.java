package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SpecStatisticsPanel statistics computation and rendering.
 */
class SpecStatisticsPanelTest {

    @Test
    void update_withEmptyList_showsNoTasksMessage() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of());

        String text = extractAllText(panel);
        assertThat(text).contains("No tasks found");
    }

    @Test
    void update_showsTotalTaskCount() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("To Do", "high"),
                taskWithStatus("In Progress", "medium"),
                taskWithStatus("Done", "low")
        ));

        String text = extractAllText(panel);
        assertThat(text).contains("3 tasks");
    }

    @Test
    void update_showsCompletionPercentage() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("Done", "medium"),
                taskWithStatus("Done", "medium"),
                taskWithStatus("To Do", "medium"),
                taskWithStatus("In Progress", "medium")
        ));

        String text = extractAllText(panel);
        assertThat(text).contains("50% complete");
    }

    @Test
    void update_showsZeroPercentWhenNoDone() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("To Do", "high"),
                taskWithStatus("In Progress", "medium")
        ));

        String text = extractAllText(panel);
        assertThat(text).contains("0% complete");
    }

    @Test
    void update_showsHundredPercentWhenAllDone() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("Done", "high"),
                taskWithStatus("Done", "low")
        ));

        String text = extractAllText(panel);
        assertThat(text).contains("100% complete");
    }

    @Test
    void update_showsStatusBreakdown() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("Done", "high"),
                taskWithStatus("In Progress", "medium"),
                taskWithStatus("In Progress", "medium"),
                taskWithStatus("To Do", "low"),
                taskWithStatus("To Do", "low"),
                taskWithStatus("To Do", "low")
        ));

        String text = extractAllText(panel);
        assertThat(text).contains("1 done");
        assertThat(text).contains("2 in progress");
        assertThat(text).contains("3 to do");
    }

    @Test
    void update_showsPriorityBreakdown() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("To Do", "high"),
                taskWithStatus("To Do", "high"),
                taskWithStatus("To Do", "medium"),
                taskWithStatus("To Do", "low")
        ));

        String text = extractAllText(panel);
        assertThat(text).contains("2 high");
        assertThat(text).contains("1 medium");
        assertThat(text).contains("1 low");
    }

    @Test
    void update_showsAcceptanceCriteriaProgress() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        TaskSpec task1 = taskWithStatus("To Do", "medium");
        task1.setAcceptanceCriteria(List.of(
                AcceptanceCriterion.builder().index(0).text("AC1").checked(true).build(),
                AcceptanceCriterion.builder().index(1).text("AC2").checked(false).build()
        ));
        TaskSpec task2 = taskWithStatus("Done", "medium");
        task2.setAcceptanceCriteria(List.of(
                AcceptanceCriterion.builder().index(0).text("AC3").checked(true).build()
        ));

        panel.update(List.of(task1, task2));

        String text = extractAllText(panel);
        assertThat(text).contains("2/3 acceptance criteria");
    }

    @Test
    void update_showsDefinitionOfDoneProgress() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        TaskSpec task = taskWithStatus("In Progress", "high");
        task.setDefinitionOfDone(List.of(
                DefinitionOfDoneItem.builder().index(0).text("Tests pass").checked(true).build(),
                DefinitionOfDoneItem.builder().index(1).text("Reviewed").checked(false).build(),
                DefinitionOfDoneItem.builder().index(2).text("Deployed").checked(false).build()
        ));

        panel.update(List.of(task));

        String text = extractAllText(panel);
        assertThat(text).contains("1/3 definition of done");
    }

    @Test
    void update_hidesChecklistRow_whenNoChecklistItems() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(taskWithStatus("To Do", "medium")));

        String text = extractAllText(panel);
        assertThat(text).doesNotContain("acceptance criteria");
        assertThat(text).doesNotContain("definition of done");
    }

    @Test
    void update_handlesCustomStatuses() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("Done", "medium"),
                taskWithStatus("Review", "medium"),
                taskWithStatus("To Do", "medium")
        ));

        String text = extractAllText(panel);
        assertThat(text).contains("3 tasks");
        assertThat(text).contains("1 other");
    }

    @Test
    void update_handlesNullStatusAsToDo() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        TaskSpec task = TaskSpec.builder().title("Null status").priority("medium").build();
        task.setStatus(null);

        panel.update(List.of(task));

        String text = extractAllText(panel);
        assertThat(text).contains("1 tasks");
    }

    @Test
    void update_showsArchivedCount_whenPositive() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("To Do", "high"),
                taskWithStatus("Done", "medium")
        ), 5);

        String text = extractAllText(panel);
        assertThat(text).contains("5 archived");
    }

    @Test
    void update_hidesArchivedCount_whenZero() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(
                taskWithStatus("To Do", "high")
        ), 0);

        String text = extractAllText(panel);
        assertThat(text).doesNotContain("archived");
    }

    @Test
    void update_showsNoTasksMessage_whenEmptyAndNoArchived() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();
        panel.update(List.of(), 0);

        String text = extractAllText(panel);
        assertThat(text).contains("No tasks found");
    }

    @Test
    void update_canBeCalledMultipleTimes() {
        SpecStatisticsPanel panel = new SpecStatisticsPanel();

        panel.update(List.of(taskWithStatus("To Do", "high")));
        String text1 = extractAllText(panel);
        assertThat(text1).contains("1 tasks");

        panel.update(List.of(
                taskWithStatus("Done", "high"),
                taskWithStatus("Done", "low")
        ));
        String text2 = extractAllText(panel);
        assertThat(text2).contains("2 tasks");
        assertThat(text2).contains("100% complete");
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static TaskSpec taskWithStatus(String status, String priority) {
        return TaskSpec.builder()
                .title("Task")
                .status(status)
                .priority(priority)
                .build();
    }

    /**
     * Recursively extract all visible text from JLabels and JTextAreas in the panel.
     */
    private static String extractAllText(Container container) {
        StringBuilder sb = new StringBuilder();
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel label) {
                sb.append(label.getText()).append(" ");
            } else if (c instanceof JTextArea area) {
                sb.append(area.getText()).append(" ");
            } else if (c instanceof Container child) {
                sb.append(extractAllText(child));
            }
        }
        return sb.toString();
    }
}
