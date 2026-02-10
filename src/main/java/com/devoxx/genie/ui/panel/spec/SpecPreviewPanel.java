package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that shows a preview of the selected task spec with metadata,
 * acceptance criteria, and action buttons.
 */
public class SpecPreviewPanel extends JPanel {

    private final Project project;
    private final JPanel contentPanel;
    private final JButton implementButton;
    private final JButton openFileButton;
    private TaskSpec currentSpec;
    private Runnable onImplementAction;

    public SpecPreviewPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        implementButton = new JButton("Implement with Agent");
        implementButton.setEnabled(false);
        implementButton.addActionListener(e -> {
            if (onImplementAction != null) {
                onImplementAction.run();
            }
        });
        buttonPanel.add(implementButton);

        openFileButton = new JButton("Open File");
        openFileButton.setEnabled(false);
        openFileButton.addActionListener(e -> openCurrentSpecFile());
        buttonPanel.add(openFileButton);

        add(buttonPanel, BorderLayout.SOUTH);

        showEmptyState();
    }

    /**
     * Set the callback for the "Implement with Agent" button.
     */
    public void setOnImplementAction(@Nullable Runnable action) {
        this.onImplementAction = action;
    }

    /**
     * Returns the currently displayed task spec.
     */
    public @Nullable TaskSpec getCurrentSpec() {
        return currentSpec;
    }

    /**
     * Display a task spec in the preview panel.
     */
    public void showSpec(@NotNull TaskSpec spec) {
        this.currentSpec = spec;
        contentPanel.removeAll();

        addField("ID", spec.getId());
        addField("Title", spec.getTitle());
        addField("Status", spec.getStatus());
        addField("Priority", spec.getPriority());

        if (spec.getAssignees() != null && !spec.getAssignees().isEmpty()) {
            addField("Assignees", String.join(", ", spec.getAssignees()));
        }
        if (spec.getLabels() != null && !spec.getLabels().isEmpty()) {
            addField("Labels", String.join(", ", spec.getLabels()));
        }
        if (spec.getDependencies() != null && !spec.getDependencies().isEmpty()) {
            addField("Dependencies", String.join(", ", spec.getDependencies()));
        }

        // Acceptance Criteria
        if (spec.getAcceptanceCriteria() != null && !spec.getAcceptanceCriteria().isEmpty()) {
            addSeparator("Acceptance Criteria");
            for (AcceptanceCriterion ac : spec.getAcceptanceCriteria()) {
                JCheckBox checkBox = new JCheckBox(ac.getText(), ac.isChecked());
                checkBox.setEnabled(false);
                checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPanel.add(checkBox);
            }
        }

        // Description preview (first 300 chars)
        if (spec.getDescription() != null && !spec.getDescription().isEmpty()) {
            addSeparator("Description");
            String preview = spec.getDescription().length() > 300
                    ? spec.getDescription().substring(0, 300) + "..."
                    : spec.getDescription();
            JTextArea descArea = new JTextArea(preview);
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            descArea.setEditable(false);
            descArea.setOpaque(false);
            descArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(descArea);
        }

        implementButton.setEnabled(true);
        openFileButton.setEnabled(true);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Show an empty state when no spec is selected.
     */
    public void showEmptyState() {
        this.currentSpec = null;
        contentPanel.removeAll();

        JBLabel emptyLabel = new JBLabel("Select a task to see its details");
        emptyLabel.setForeground(JBColor.GRAY);
        emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(emptyLabel);

        implementButton.setEnabled(false);
        openFileButton.setEnabled(false);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void addField(@NotNull String label, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JBLabel nameLabel = new JBLabel(label + ":");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        row.add(nameLabel);

        JBLabel valueLabel = new JBLabel(value);
        row.add(valueLabel);

        contentPanel.add(row);
    }

    private void addSeparator(@NotNull String title) {
        contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        JBLabel separator = new JBLabel(title);
        separator.setFont(separator.getFont().deriveFont(Font.BOLD, separator.getFont().getSize() + 1f));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(separator);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private void openCurrentSpecFile() {
        if (currentSpec == null || currentSpec.getFilePath() == null) {
            return;
        }
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(currentSpec.getFilePath());
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true);
        }
    }
}
