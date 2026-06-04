package com.devoxx.genie.ui.panel;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for per-session search toggles above the prompt input area.
 * The RAG toggle was removed in task-222 (master setting in RAG Settings is the single source of truth).
 * The Web toggle was removed in task-223 (use the /search command instead).
 * The panel is kept as a placeholder so callers don't need structural changes.
 */
public class SearchOptionsPanel extends JPanel {

    public SearchOptionsPanel(Project project) {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
        setBorder(JBUI.Borders.empty(5, 10));
        setVisible(false);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, 0);
    }

    public void updatePanelVisibility() {
        // No switches remain; panel stays hidden.
    }
}
