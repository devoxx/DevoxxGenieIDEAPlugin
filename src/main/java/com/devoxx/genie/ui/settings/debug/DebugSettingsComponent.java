package com.devoxx.genie.ui.settings.debug;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Settings UI for the "Raw Request/Response" debug viewer (see the Activity Log tool window).
 */
public class DebugSettingsComponent {

    private final JPanel panel;
    private final JBCheckBox rawRequestResponseLoggingCheckBox;

    public DebugSettingsComponent() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        rawRequestResponseLoggingCheckBox = new JBCheckBox("Enable Raw Request/Response Logging");
        rawRequestResponseLoggingCheckBox.setSelected(Boolean.TRUE.equals(state.getRawRequestResponseLoggingEnabled()));

        JBLabel description = new JBLabel(
                "<html><body style='width:480px'>" +
                        "When enabled, the exact request sent to the LLM provider (messages, tool calls, " +
                        "model parameters) and the response received (message, tool calls, token usage) are " +
                        "captured and shown in the <b>Activity Log</b> tool window, filterable to \"Show Raw " +
                        "Only\". Double-click an entry to open the full JSON, or use \"Copy All\" for bug reports. " +
                        "Likely API keys and tokens are masked automatically, but the raw prompt and response " +
                        "text is not — avoid enabling this if your conversations may contain other secrets you " +
                        "don't want written to the tool window / clipboard." +
                        "</body></html>");
        description.setForeground(UIUtil.getContextHelpForeground());

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(12));

        rawRequestResponseLoggingCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(rawRequestResponseLoggingCheckBox);
        panel.add(Box.createVerticalStrut(6));
        panel.add(description);
        panel.add(Box.createVerticalGlue());
    }

    public JPanel getPanel() {
        return panel;
    }

    public boolean isModified() {
        return rawRequestResponseLoggingCheckBox.isSelected()
                != Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRawRequestResponseLoggingEnabled());
    }

    public void apply() {
        DevoxxGenieStateService.getInstance().setRawRequestResponseLoggingEnabled(rawRequestResponseLoggingCheckBox.isSelected());
    }

    public void reset() {
        rawRequestResponseLoggingCheckBox.setSelected(
                Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRawRequestResponseLoggingEnabled()));
    }

    public boolean isRawRequestResponseLoggingSelected() {
        return rawRequestResponseLoggingCheckBox.isSelected();
    }
}
