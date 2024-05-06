package com.devoxx.genie.ui;

import com.devoxx.genie.ui.util.WorkingMessage;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieColors.GRAY_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieColors.PROMPT_BG_COLOR;

public class WaitingPanel extends JBPanel<WaitingPanel> {

    public WaitingPanel() {
        super(new BorderLayout());
        andTransparent();
        withBackground(PROMPT_BG_COLOR);
        withMaximumSize(500, 30);
        getInsets().set(5, 5, 5, 5);

        JBLabel workingLabel = new JBLabel(WorkingMessage.getWorkingMessage());
        workingLabel.setFont(workingLabel.getFont().deriveFont(12f));
        workingLabel.setForeground(GRAY_COLOR);
        workingLabel.setMaximumSize(new Dimension(500, 30));
        add(workingLabel, BorderLayout.SOUTH);
    }

    public void hideMsg() {
        setVisible(false);
    }

    public void showMsg() {
        setVisible(true);
    }
}
