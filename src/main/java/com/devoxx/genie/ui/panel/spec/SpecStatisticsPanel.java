package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact statistics panel for the Spec Browser showing aggregate progress at a glance.
 * Displays task counts by status, completion percentage, priority breakdown,
 * and acceptance criteria / Definition of Done progress.
 */
public class SpecStatisticsPanel extends JPanel {

    private static final JBColor BAR_TODO = new JBColor(new Color(108, 117, 125), new Color(108, 117, 125));
    private static final JBColor BAR_IN_PROGRESS = new JBColor(new Color(0, 123, 255), new Color(60, 140, 255));
    private static final JBColor BAR_DONE = new JBColor(new Color(40, 167, 69), new Color(60, 180, 90));
    private static final JBColor BAR_OTHER = new JBColor(new Color(255, 193, 7), new Color(200, 160, 30));
    private static final JBColor BAR_BACKGROUND = new JBColor(new Color(233, 236, 239), new Color(60, 63, 65));
    public static final String PROJECT_OVERVIEW = "Project Overview";

    private final JPanel contentPanel;
    private boolean collapsed = false;
    private final JButton toggleButton;

    public SpecStatisticsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        // Header with toggle
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JBLabel titleLabel = new JBLabel(PROJECT_OVERVIEW);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() - 1f));
        header.add(titleLabel, BorderLayout.WEST);

        toggleButton = new JButton("Hide");
        toggleButton.setFont(toggleButton.getFont().deriveFont(toggleButton.getFont().getSize() - 2f));
        toggleButton.setMargin(new Insets(0, 4, 0, 4));
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusable(false);
        toggleButton.addActionListener(e -> toggleCollapsed());
        header.add(toggleButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 4, 5));
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Update the statistics display with the current list of tasks.
     */
    public void update(@NotNull List<TaskSpec> specs) {
        update(specs, 0);
    }

    /**
     * Update the statistics display with the current list of tasks and archived task count.
     */
    public void update(@NotNull List<TaskSpec> specs, int archivedCount) {
        contentPanel.removeAll();

        if (specs.isEmpty() && archivedCount == 0) {
            addEmptyState();
            return;
        }

        Map<String, Integer> statusCounts = countByStatus(specs);
        Map<String, Integer> priorityCounts = countByPriority(specs);
        int total = specs.size();
        int doneCount = statusCounts.getOrDefault("Done", 0);
        int inProgressCount = statusCounts.getOrDefault("In Progress", 0);
        int todoCount = statusCounts.getOrDefault("To Do", 0);
        int otherCount = total - doneCount - inProgressCount - todoCount;
        double completionPct = total > 0 ? (doneCount * 100.0) / total : 0;

        addSummaryRow(total, doneCount, inProgressCount, todoCount, otherCount, completionPct, archivedCount);
        addProgressBarRow(todoCount, inProgressCount, doneCount, otherCount);
        addPriorityRow(priorityCounts);
        addChecklistRow(specs);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void addEmptyState() {
        JBLabel emptyLabel = new JBLabel("No tasks found");
        emptyLabel.setForeground(JBColor.GRAY);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(emptyLabel.getFont().getSize() - 1f));
        emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(emptyLabel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void addSummaryRow(int total, int doneCount, int inProgressCount, int todoCount,
                               int otherCount, double completionPct, int archivedCount) {
        String otherSuffix = otherCount > 0 ? ", " + otherCount + " other" : "";
        String archivedSuffix = archivedCount > 0 ? "  |  " + archivedCount + " archived" : "";
        String summaryText = String.format("%d tasks  |  %d%% complete  (%d done, %d in progress, %d to do%s)%s",
                total, Math.round(completionPct), doneCount, inProgressCount, todoCount,
                otherSuffix, archivedSuffix);
        JBLabel summaryLabel = new JBLabel(summaryText);
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(summaryLabel.getFont().getSize() - 1f));
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(summaryLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 3)));
    }

    private void addProgressBarRow(int todoCount, int inProgressCount, int doneCount, int otherCount) {
        StatusProgressBar progressBar = new StatusProgressBar(todoCount, inProgressCount, doneCount, otherCount);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        progressBar.setPreferredSize(new Dimension(200, 12));
        contentPanel.add(progressBar);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 3)));
    }

    private void addPriorityRow(Map<String, Integer> priorityCounts) {
        JBLabel priorityLabel = new JBLabel(buildPriorityText(priorityCounts));
        priorityLabel.setFont(priorityLabel.getFont().deriveFont(priorityLabel.getFont().getSize() - 1f));
        priorityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(priorityLabel);
    }

    private static String buildPriorityText(Map<String, Integer> priorityCounts) {
        List<String> standardPriorities = List.of("high", "medium", "low");
        StringBuilder text = new StringBuilder("Priority:  ");
        boolean first = true;
        for (String priority : standardPriorities) {
            int count = priorityCounts.getOrDefault(priority, 0);
            if (count > 0) {
                if (!first) text.append("  |  ");
                text.append(count).append(" ").append(priority);
                first = false;
            }
        }
        for (Map.Entry<String, Integer> entry : priorityCounts.entrySet()) {
            if (!standardPriorities.contains(entry.getKey()) && entry.getValue() > 0) {
                if (!first) text.append("  |  ");
                text.append(entry.getValue()).append(" ").append(entry.getKey());
                first = false;
            }
        }
        return text.toString();
    }

    private void addChecklistRow(@NotNull List<TaskSpec> specs) {
        long totalAc = 0;
        long checkedAc = 0;
        long totalDod = 0;
        long checkedDod = 0;
        for (TaskSpec spec : specs) {
            if (spec.getAcceptanceCriteria() != null) {
                totalAc += spec.getAcceptanceCriteria().size();
                checkedAc += spec.getAcceptanceCriteria().stream().filter(AcceptanceCriterion::isChecked).count();
            }
            if (spec.getDefinitionOfDone() != null) {
                totalDod += spec.getDefinitionOfDone().size();
                checkedDod += spec.getDefinitionOfDone().stream().filter(DefinitionOfDoneItem::isChecked).count();
            }
        }
        if (totalAc == 0 && totalDod == 0) return;

        contentPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        JBLabel checklistLabel = new JBLabel(buildChecklistText(checkedAc, totalAc, checkedDod, totalDod));
        checklistLabel.setFont(checklistLabel.getFont().deriveFont(checklistLabel.getFont().getSize() - 1f));
        checklistLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(checklistLabel);
    }

    private static String buildChecklistText(long checkedAc, long totalAc, long checkedDod, long totalDod) {
        StringBuilder text = new StringBuilder("Checklists:  ");
        boolean hasAc = totalAc > 0;
        if (hasAc) {
            text.append(checkedAc).append("/").append(totalAc).append(" acceptance criteria");
        }
        if (totalDod > 0) {
            if (hasAc) text.append("  |  ");
            text.append(checkedDod).append("/").append(totalDod).append(" definition of done");
        }
        return text.toString();
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        contentPanel.setVisible(!collapsed);
        toggleButton.setText(collapsed ? "Show" : "Hide");
    }

    private static @NotNull Map<String, Integer> countByStatus(@NotNull List<TaskSpec> specs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TaskSpec spec : specs) {
            String status = spec.getStatus() != null ? spec.getStatus() : "To Do";
            counts.merge(status, 1, Integer::sum);
        }
        return counts;
    }

    private static @NotNull Map<String, Integer> countByPriority(@NotNull List<TaskSpec> specs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TaskSpec spec : specs) {
            String priority = spec.getPriority() != null ? spec.getPriority().toLowerCase() : "medium";
            counts.merge(priority, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * A simple segmented horizontal bar showing task distribution by status.
     */
    private static class StatusProgressBar extends JPanel {
        private final int todo;
        private final int inProgress;
        private final int done;
        private final int other;
        private final int total;

        StatusProgressBar(int todo, int inProgress, int done, int other) {
            this.todo = todo;
            this.inProgress = inProgress;
            this.done = done;
            this.other = other;
            this.total = todo + inProgress + done + other;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (total == 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int arc = getHeight();

            // Background
            g2.setColor(BAR_BACKGROUND);
            g2.fillRoundRect(0, 0, w, arc, arc, arc);

            // Segments: Done | In Progress | Other | To Do
            int x = 0;
            x = drawSegment(g2, x, arc, w, arc, done, BAR_DONE);
            x = drawSegment(g2, x, arc, w, arc, inProgress, BAR_IN_PROGRESS);
            x = drawSegment(g2, x, arc, w, arc, other, BAR_OTHER);
            drawSegment(g2, x, arc, w, arc, todo, BAR_TODO);

            g2.dispose();
        }

        private int drawSegment(Graphics2D g2, int x, int h, int totalWidth, int arc, int count, Color color) {
            if (count <= 0) return x;
            int segmentWidth = Math.max(1, (int) ((count / (double) total) * totalWidth));
            // Clamp to remaining width
            segmentWidth = Math.min(segmentWidth, totalWidth - x);
            if (segmentWidth <= 0) return x;

            g2.setColor(color);
            if (x == 0 && (x + segmentWidth) >= totalWidth) {
                // Full bar
                g2.fillRoundRect(x, 0, segmentWidth, h, arc, arc);
            } else if (x == 0) {
                // Left rounded
                g2.fillRoundRect(x, 0, segmentWidth + arc / 2, h, arc, arc);
                g2.fillRect(segmentWidth, 0, arc / 2, h); // fill the gap
                // Actually simpler: just clip. Let's do a simple rect for middle segments.
                g2.fillRoundRect(x, 0, segmentWidth, h, arc, arc);
            } else {
                // Middle or right segment: simple rect
                g2.fillRect(x, 0, segmentWidth, h);
            }
            return x + segmentWidth;
        }
    }
}
