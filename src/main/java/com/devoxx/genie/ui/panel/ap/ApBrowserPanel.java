package com.devoxx.genie.ui.panel.ap;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Container for the Docker Agentic Platform tool window.
 *
 * <p>Three tabs: <b>New Run</b> (default — agent/project picker + prompt + streamed response),
 * <b>Sessions</b> (history with filters), and <b>Agents</b> (catalog).</p>
 */
public class ApBrowserPanel extends SimpleToolWindowPanel {

    private final ApNewRunPanel newRunPanel;
    private final ApSessionsTab sessionsTab;
    private final ApAgentsTab agentsTab;

    public ApBrowserPanel(@NotNull Project project) {
        super(true, true);

        newRunPanel = new ApNewRunPanel(project);
        sessionsTab = new ApSessionsTab(project);
        agentsTab = new ApAgentsTab();

        // Clicking an agent in the Agents tab pre-selects it on the New Run tab.
        agentsTab.setOnAgentSelected(a -> {
            if (a != null && a.name() != null) newRunPanel.selectAgentByName(a.name());
        });

        JBTabbedPane tabs = new JBTabbedPane();
        int newRunIndex = tabs.getTabCount();
        tabs.addTab("New Run", newRunPanel);
        tabs.addTab("Sessions", sessionsTab);
        tabs.addTab("Agents", agentsTab);

        // Double-click "Start a new run" in the agent details dialog: switch to New Run + preselect.
        agentsTab.setOnStartNewRun(a -> {
            if (a == null || a.name() == null) return;
            tabs.setSelectedIndex(newRunIndex);
            newRunPanel.selectAgentByName(a.name());
        });

        JPanel content = new JPanel(new BorderLayout());
        content.add(new ApPreviewRibbon(), BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);

        setContent(content);
    }
}
