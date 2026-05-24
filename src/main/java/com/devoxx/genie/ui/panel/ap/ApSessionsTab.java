package com.devoxx.genie.ui.panel.ap;

import com.devoxx.genie.model.ap.ApAgent;
import com.devoxx.genie.model.ap.ApProject;
import com.devoxx.genie.model.ap.ApSession;
import com.devoxx.genie.service.ap.ApCliException;
import com.devoxx.genie.service.ap.ApCliService;
import com.devoxx.genie.service.ap.ApProjectMatcher;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists past sessions with optional agent/project filtering.
 *
 * <p>Continuation of a session is not exposed via the {@code ap} CLI yet, so the row action
 * is "Open in browser" which navigates to the Agentic Platform web UI for the selected
 * session (the {@code open_url} returned by {@code ap run}).</p>
 */
@Slf4j
public class ApSessionsTab extends JPanel {

    static final String SESSION_URL_TEMPLATE = "https://agentic-platform.docker.com/sessions/%s";
    private static final String ANY_AGENT = "<any agent>";
    private static final String ANY_PROJECT = "<any project>";

    private final Project project;

    private final SessionTableModel tableModel = new SessionTableModel();
    private final JBTable table = new JBTable(tableModel);
    private final JComboBox<String> agentFilter = new JComboBox<>();
    private final JComboBox<String> projectFilter = new JComboBox<>();
    private final JButton refreshButton = new JButton(DevoxxGenieIconsUtil.RefreshIcon);
    private final JButton openButton = new JButton("Open in browser", AllIcons.General.Web);
    private final JBLabel statusLabel = new JBLabel(" ");

    public ApSessionsTab(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;

        table.setRowHeight(22);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(280); // title
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // status
        table.getColumnModel().getColumn(2).setPreferredWidth(120); // agent
        table.getColumnModel().getColumn(3).setPreferredWidth(160); // project
        table.getTableHeader().setReorderingAllowed(false);

        agentFilter.setModel(new DefaultComboBoxModel<>(new String[]{ANY_AGENT}));
        projectFilter.setModel(new DefaultComboBoxModel<>(new String[]{ANY_PROJECT}));

        refreshButton.addActionListener(e -> refresh());
        openButton.addActionListener(e -> openSelectedInBrowser());
        agentFilter.addActionListener(e -> refresh());
        projectFilter.addActionListener(e -> refresh());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.add(new JBLabel("Agent:"));
        toolbar.add(agentFilter);
        toolbar.add(new JBLabel("Project:"));
        toolbar.add(projectFilter);
        toolbar.add(refreshButton);
        toolbar.add(openButton);
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        toolbar.add(statusLabel);

        add(toolbar, BorderLayout.NORTH);
        add(new JBScrollPane(table), BorderLayout.CENTER);

        loadFilterOptions();
        refresh();
    }

    /** Populates the agent/project filter dropdowns from the platform metadata. */
    public void loadFilterOptions() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<String> agentNames = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            agentNames.add(ANY_AGENT);
            projectNames.add(ANY_PROJECT);
            try {
                ApCliService svc = ApCliService.getInstance();
                svc.listAgents(50).stream()
                        .map(ApAgent::name)
                        .filter(n -> n != null && !n.isBlank())
                        .forEach(agentNames::add);
                svc.listProjects(50).stream()
                        .map(ApProject::name)
                        .filter(n -> n != null && !n.isBlank())
                        .forEach(projectNames::add);
            } catch (ApCliException e) {
                log.debug("Could not populate session filter dropdowns: {}", e.getMessage());
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                Object selAgent = agentFilter.getSelectedItem();
                Object selProject = projectFilter.getSelectedItem();
                agentFilter.setModel(new DefaultComboBoxModel<>(agentNames.toArray(new String[0])));
                projectFilter.setModel(new DefaultComboBoxModel<>(projectNames.toArray(new String[0])));
                if (selAgent != null) agentFilter.setSelectedItem(selAgent);

                // Default the project filter to the current IDE project on first load.
                // On subsequent reloads, respect whatever the user had picked.
                String preferred = (selProject == null || ANY_PROJECT.equals(selProject))
                        ? findCurrentProjectName(projectNames)
                        : selProject.toString();
                if (preferred != null) projectFilter.setSelectedItem(preferred);
            }, ModalityState.any());
        });
    }

    public void refresh() {
        String agent = filterValue(agentFilter, ANY_AGENT);
        String project = filterValue(projectFilter, ANY_PROJECT);
        statusLabel.setText("Loading…");
        setRefreshing(true);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<ApSession> sessions = ApCliService.getInstance().listSessions(agent, project, 50);
                ApplicationManager.getApplication().invokeLater(() -> {
                    tableModel.setSessions(sessions);
                    statusLabel.setText(sessions.size() + " session(s)");
                    setRefreshing(false);
                }, ModalityState.any());
            } catch (ApCliException e) {
                log.warn("Failed to list sessions: {}", e.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    setRefreshing(false);
                }, ModalityState.any());
            }
        });
    }

    /** Swaps the refresh button icon for a spinner (and disables the button) while a refresh is in flight. */
    private void setRefreshing(boolean refreshing) {
        refreshButton.setIcon(refreshing ? AnimatedIcon.Default.INSTANCE : DevoxxGenieIconsUtil.RefreshIcon);
        refreshButton.setEnabled(!refreshing);
    }

    private void openSelectedInBrowser() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        ApSession s = tableModel.getSessionAt(table.convertRowIndexToModel(row));
        if (s == null || s.id() == null) return;
        BrowserUtil.open(String.format(SESSION_URL_TEMPLATE, s.id()));
    }

    /** Returns the first available filter name that matches the IDE project, or {@code null}. */
    private @Nullable String findCurrentProjectName(@NotNull List<String> available) {
        for (String candidate : ApProjectMatcher.candidateNames(project)) {
            for (String name : available) {
                if (candidate.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private @Nullable String filterValue(@NotNull JComboBox<String> combo, @NotNull String anyValue) {
        Object sel = combo.getSelectedItem();
        if (sel == null || anyValue.equals(sel)) return null;
        return sel.toString();
    }

    // ===== Table model =====

    private static class SessionTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Title", "Status", "Agent", "Project"};
        private final List<ApSession> sessions = new ArrayList<>();

        void setSessions(@NotNull List<ApSession> next) {
            sessions.clear();
            sessions.addAll(next);
            fireTableDataChanged();
        }

        @Nullable ApSession getSessionAt(int modelRow) {
            return modelRow >= 0 && modelRow < sessions.size() ? sessions.get(modelRow) : null;
        }

        @Override public int getRowCount() { return sessions.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ApSession s = sessions.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> s.title() == null ? "" : s.title();
                case 1 -> s.status() == null ? "" : s.status();
                case 2 -> s.agent() == null ? "" : s.agent();
                case 3 -> s.project() == null ? "" : s.project();
                default -> "";
            };
        }
    }
}
