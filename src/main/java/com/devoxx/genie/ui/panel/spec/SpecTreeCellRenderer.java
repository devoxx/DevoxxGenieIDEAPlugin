package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Collections;
import java.util.Set;

/**
 * Custom tree cell renderer for task specs that shows status icons, priority, and task labels.
 * Optionally renders checkboxes for "To Do" tasks when a checked-set is provided.
 */
public class SpecTreeCellRenderer extends ColoredTreeCellRenderer {

    private static final Color HIGH_PRIORITY_COLOR = new JBColor(new Color(220, 50, 50), new Color(255, 80, 80));
    private static final Color MEDIUM_PRIORITY_COLOR = new JBColor(new Color(200, 150, 0), new Color(230, 180, 50));
    private static final Color LOW_PRIORITY_COLOR = new JBColor(new Color(100, 100, 100), new Color(160, 160, 160));

    private static final Color STATUS_TODO_COLOR = new JBColor(new Color(100, 100, 200), new Color(130, 130, 230));
    private static final Color STATUS_IN_PROGRESS_COLOR = new JBColor(new Color(200, 150, 0), new Color(230, 180, 50));
    private static final Color STATUS_DONE_COLOR = new JBColor(new Color(50, 160, 50), new Color(80, 200, 80));

    private Set<String> checkedTaskIds = Collections.emptySet();

    /**
     * Set the IDs of tasks that are currently checked (selected for batch run).
     */
    public void setCheckedTaskIds(@NotNull Set<String> checkedTaskIds) {
        this.checkedTaskIds = checkedTaskIds;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                       Object value,
                                       boolean selected,
                                       boolean expanded,
                                       boolean leaf,
                                       int row,
                                       boolean hasFocus) {
        if (!(value instanceof DefaultMutableTreeNode node)) {
            return;
        }

        Object userObject = node.getUserObject();

        if (userObject instanceof TaskSpec spec) {
            renderTaskSpec(spec);
        } else if (userObject instanceof String groupLabel) {
            renderGroupLabel(groupLabel, node);
        }
    }

    private void renderTaskSpec(@NotNull TaskSpec spec) {
        // Checkbox icon for To Do tasks
        if ("To Do".equalsIgnoreCase(spec.getStatus()) && spec.getId() != null) {
            boolean checked = checkedTaskIds.contains(spec.getId());
            Icon checkIcon = checked ? AllIcons.Actions.Checked : AllIcons.Actions.Unselectall;
            setIcon(checkIcon);
        }

        // Priority indicator
        SimpleTextAttributes priorityAttr = getPriorityAttributes(spec.getPriority());

        // Task ID
        if (spec.getId() != null) {
            append(spec.getId(), SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            append("  ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        // Task title
        SimpleTextAttributes titleAttr = "Done".equalsIgnoreCase(spec.getStatus())
                ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, null)
                : priorityAttr;
        append(spec.getTitle() != null ? spec.getTitle() : "Untitled", titleAttr);

        // Acceptance criteria progress
        if (spec.getAcceptanceCriteria() != null && !spec.getAcceptanceCriteria().isEmpty()) {
            long checked = spec.getCheckedAcceptanceCriteriaCount();
            int total = spec.getAcceptanceCriteria().size();
            append("  [" + checked + "/" + total + "]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    private void renderGroupLabel(@NotNull String label, @NotNull DefaultMutableTreeNode node) {
        int childCount = node.getChildCount();
        Color statusColor = getStatusColor(label);

        append(label, new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, statusColor));
        append(" (" + childCount + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    private @NotNull SimpleTextAttributes getPriorityAttributes(@Nullable String priority) {
        if (priority == null) {
            return SimpleTextAttributes.REGULAR_ATTRIBUTES;
        }
        return switch (priority.toLowerCase()) {
            case "high" -> new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, HIGH_PRIORITY_COLOR);
            case "medium" -> new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, MEDIUM_PRIORITY_COLOR);
            case "low" -> new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, LOW_PRIORITY_COLOR);
            default -> SimpleTextAttributes.REGULAR_ATTRIBUTES;
        };
    }

    private @NotNull Color getStatusColor(@NotNull String status) {
        String lower = status.toLowerCase();
        if (lower.contains("done") || lower.contains("complete")) {
            return STATUS_DONE_COLOR;
        } else if (lower.contains("progress") || lower.contains("doing")) {
            return STATUS_IN_PROGRESS_COLOR;
        } else {
            return STATUS_TODO_COLOR;
        }
    }
}
