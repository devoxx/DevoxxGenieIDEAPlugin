package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Inline progress indicator panel for the spec task runner.
 * Shows current task progress and a determinate progress bar.
 */
public class SpecTaskRunnerProgressPanel extends JPanel {

    private final JBLabel statusLabel;
    private final JProgressBar progressBar;

    public SpecTaskRunnerProgressPanel() {
        super(new BorderLayout(8, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        statusLabel = new JBLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(120, 14));
        progressBar.setStringPainted(false);

        add(statusLabel, BorderLayout.CENTER);
        add(progressBar, BorderLayout.EAST);

        setVisible(false);
    }

    /**
     * Update progress for a running task.
     */
    public void update(@NotNull TaskSpec task, int index, int total) {
        String label = String.format("Running task %d/%d: %s", index + 1, total, task.getDisplayLabel());
        statusLabel.setText(label);
        progressBar.setMinimum(0);
        progressBar.setMaximum(total);
        progressBar.setValue(index);
        setVisible(true);
    }

    /**
     * Show completion summary.
     */
    public void showCompleted(int completed, int skipped, int total) {
        String text;
        if (skipped > 0) {
            text = String.format("Finished: %d/%d completed, %d skipped", completed, total, skipped);
        } else {
            text = String.format("Finished: %d/%d completed", completed, total);
        }
        statusLabel.setText(text);
        progressBar.setMaximum(total);
        progressBar.setValue(total);
        setVisible(true);

        // Auto-hide after 10 seconds
        Timer hideTimer = new Timer(10_000, e -> hidePanel());
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    /**
     * Hide the progress panel.
     * Note: named hidePanel() to avoid collision with Component.hide()
     * which setVisible(false) delegates to, causing infinite recursion.
     */
    public void hidePanel() {
        setVisible(false);
    }
}
