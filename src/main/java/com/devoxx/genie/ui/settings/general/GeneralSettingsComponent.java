package com.devoxx.genie.ui.settings.general;

import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.service.analytics.DevoxxGenieSettingsChangedTopic;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.HyperlinkLabel;
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

    private static final String ANALYTICS_SOURCE_URL =
            "https://github.com/devoxx/DevoxxGenieIDEAPlugin/blob/master/" +
                    "src/main/java/com/devoxx/genie/service/analytics/AnalyticsEventBuilder.java";

    private final JPanel panel;
    private final JCheckBox analyticsEnabledCheckBox;

    public GeneralSettingsComponent() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        analyticsEnabledCheckBox = new JCheckBox("Help improve DevoxxGenie by sending anonymous usage data");
        analyticsEnabledCheckBox.setSelected(Boolean.TRUE.equals(state.getAnalyticsEnabled()));

        JBLabel intro = new JBLabel(
                "<html><body style='width:480px'>" +
                        "We're a small open-source team, and <b>anonymous</b> usage data is the only way " +
                        "we know which features are actually worth our time. " +
                        "<b>No prompts, no code, no file paths, no API keys</b> — ever. " +
                        "Just things like which LLM provider you picked and whether RAG is enabled." +
                        "</body></html>");

        HyperlinkLabel sourceLink = new HyperlinkLabel("See exactly what we send: AnalyticsEventBuilder.java");
        sourceLink.setHyperlinkTarget(ANALYTICS_SOURCE_URL);

        JBLabel notSentHeader = new JBLabel("<html><b>What is never sent:</b></html>");
        JBLabel notSentList = new JBLabel(
                "<html><ul style='margin-left:18px'>" +
                        "<li>Prompt text, response text, conversation history</li>" +
                        "<li>File content, file paths, project name, git remote</li>" +
                        "<li>MCP server names, URLs, commands, tool names, or environment variables</li>" +
                        "<li>User-defined custom prompt names or bodies</li>" +
                        "<li>API keys, credentials, user name, email</li>" +
                        "<li>Token counts or cost data</li>" +
                        "</ul></html>");

        JBLabel sentHeader = new JBLabel("<html><b>What is sent</b> (per LLM prompt, model selection, or session):</html>");
        JBLabel sentList = new JBLabel(
                "<html><ul style='margin-left:18px'>" +
                        "<li>An anonymous install ID (UUID), generated once and stored locally</li>" +
                        "<li>A per-launch session ID (random 10-digit number)</li>" +
                        "<li>Plugin version and IDE version</li>" +
                        "<li>LLM provider name (e.g. anthropic, ollama)</li>" +
                        "<li>LLM model name (e.g. claude-3-5-sonnet)</li>" +
                        "<li>Which optional features are enabled (RAG, Agent, MCP, Web Search, streaming) " +
                        "and coarse counts (e.g. bucketed number of configured MCP servers or custom prompts)</li>" +
                        "<li>Which features are actually used during a prompt " +
                        "(feature identifiers only, never prompt text or file content)</li>" +
                        "</ul></html>");

        Color subtle = UIUtil.getContextHelpForeground();
        for (JBLabel l : new JBLabel[]{intro, sentHeader, sentList, notSentHeader, notSentList}) {
            l.setForeground(subtle);
        }

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(12));

        analyticsEnabledCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        sourceLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        sentHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        sentList.setAlignmentX(Component.LEFT_ALIGNMENT);
        notSentHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        notSentList.setAlignmentX(Component.LEFT_ALIGNMENT);

        String version = PropertiesService.getInstance().getVersion();
        JBLabel versionLabel = new JBLabel("Plugin version: " + (version != null ? version : "unknown"));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionLabel.setForeground(UIUtil.getContextHelpForeground());

        panel.add(intro);
        panel.add(Box.createVerticalStrut(10));
        panel.add(analyticsEnabledCheckBox);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sourceLink);
        panel.add(Box.createVerticalStrut(6));
        panel.add(versionLabel);
        panel.add(Box.createVerticalStrut(16));
        panel.add(notSentHeader);
        panel.add(notSentList);
        panel.add(Box.createVerticalStrut(8));
        panel.add(sentHeader);
        panel.add(sentList);
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
        DevoxxGenieSettingsChangedTopic.notifySettingsChanged();
    }

    public void reset() {
        analyticsEnabledCheckBox.setSelected(
                Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAnalyticsEnabled()));
    }
}
