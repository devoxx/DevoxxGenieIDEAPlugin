package com.devoxx.genie.ui.settings.general;

import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.service.analytics.DevoxxGenieSettingsChangedTopic;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Settings UI for Analytics DevoxxGenie options. Currently exposes the anonymous usage
 * analytics opt-out (task-206). Help text below the checkbox enumerates every field that
 * is sent and what is never sent.
 */
public class GeneralSettingsComponent {

    private final JPanel panel;
    private final JCheckBox analyticsEnabledCheckBox;

    public GeneralSettingsComponent() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        analyticsEnabledCheckBox = new JCheckBox("Send anonymous usage statistics");
        analyticsEnabledCheckBox.setSelected(Boolean.TRUE.equals(state.getAnalyticsEnabled()));

        JBLabel sentHeader = new JBLabel("<html><b>What is sent</b> (per LLM prompt or model selection):</html>");
        JBLabel sentList = new JBLabel(
                "<html><ul style='margin-left:18px'>" +
                        "<li>An anonymous install ID (UUID), generated once and stored locally</li>" +
                        "<li>A per-launch session ID (random 10-digit number)</li>" +
                        "<li>Plugin version and IDE version</li>" +
                        "<li>LLM provider name (e.g. anthropic, ollama)</li>" +
                        "<li>LLM model name (e.g. claude-3-5-sonnet)</li>" +
                        "</ul></html>");

        JBLabel notSentHeader = new JBLabel("<html><b>What is never sent:</b></html>");
        JBLabel notSentList = new JBLabel(
                "<html><ul style='margin-left:18px'>" +
                        "<li>Prompt text, response text, conversation history</li>" +
                        "<li>File content, file paths, project name, git remote</li>" +
                        "<li>API keys, credentials, user name, email</li>" +
                        "<li>Token counts or cost data</li>" +
                        "</ul>This data is used only to guide which LLM providers and models receive engineering " +
                        "investment, and to improve features specific to often-used LLM providers.</html>");

        Color subtle = UIUtil.getContextHelpForeground();
        for (JBLabel l : new JBLabel[]{sentHeader, sentList, notSentHeader, notSentList}) {
            l.setForeground(subtle);
        }

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(12));

        analyticsEnabledCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sentHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        sentList.setAlignmentX(Component.LEFT_ALIGNMENT);
        notSentHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        notSentList.setAlignmentX(Component.LEFT_ALIGNMENT);

        String version = PropertiesService.getInstance().getVersion();
        JBLabel versionLabel = new JBLabel("Plugin version: " + (version != null ? version : "unknown"));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionLabel.setForeground(UIUtil.getContextHelpForeground());

        panel.add(analyticsEnabledCheckBox);
        panel.add(Box.createVerticalStrut(8));
        panel.add(versionLabel);
        panel.add(Box.createVerticalStrut(16));
        panel.add(sentHeader);
        panel.add(sentList);
        panel.add(Box.createVerticalStrut(8));
        panel.add(notSentHeader);
        panel.add(notSentList);
        panel.add(Box.createVerticalGlue());
    }

    public JPanel getPanel() {
        return panel;
    }

    public boolean isModified() {
        return analyticsEnabledCheckBox.isSelected()
                != Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAnalyticsEnabled());
    }

    public void apply() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        state.setAnalyticsEnabled(analyticsEnabledCheckBox.isSelected());
        // Touching the setting in the UI counts as informed acknowledgement — so we never
        // re-show the first-launch notice for users who configured the toggle explicitly.
        state.setAnalyticsNoticeAcknowledged(true);

        // Re-arm the feature-enablement snapshot (task-209).
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(DevoxxGenieSettingsChangedTopic.TOPIC)
                .settingsChanged();
    }

    public void reset() {
        analyticsEnabledCheckBox.setSelected(
                Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAnalyticsEnabled()));
    }
}
