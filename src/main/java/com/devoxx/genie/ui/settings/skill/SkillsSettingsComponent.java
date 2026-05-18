package com.devoxx.genie.ui.settings.skill;

import com.devoxx.genie.service.skill.SkillRegistry;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings panel for langchain4j {@link com.devoxx.genie.service.skill.SkillRegistry skills}
 * loaded from six locations: {@code ~/.agents/skills/}, {@code ~/.claude/skills/},
 * {@code ~/.devoxxgenie/skills/} and the matching {@code &lt;project&gt;/.<tool>/skills/}
 * counterparts.
 *
 * <p>The panel shows a table of detected skills (with their source), lets the user
 * enable/disable each one, and exposes shortcuts to open the source folders and reload
 * from disk.</p>
 */
@Slf4j
public class SkillsSettingsComponent extends AbstractSettingsComponent {

    private static final String DOCS_URL = "https://genie.devoxx.com/docs/features/skills";

    private final Project project;
    private final SkillRegistry registry;
    private final SkillsTableModel tableModel = new SkillsTableModel();

    /**
     * Working copy of the disabled-names set, populated from settings in {@link #reset()} and
     * mutated as the user toggles checkboxes. Persisted only on {@link #apply()}.
     */
    private final Set<String> workingDisabledNames = new HashSet<>();

    public SkillsSettingsComponent(Project project) {
        this.project = project;
        this.registry = SkillRegistry.getInstance(project);
        // Trigger reset() synchronously to seed the working set; the table is then refreshed
        // asynchronously from the pooled-thread skills scan to avoid blocking the EDT when
        // the settings panel is opened.
        resetWorkingSet();
        registry.reloadAsync(this::refreshTable);
    }

    @Override
    protected String getHelpUrl() {
        return DOCS_URL;
    }

    @Override
    public JPanel createPanel() {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(5);

        addSection(panel, gbc, "Skills");

        // Banner shown when Agent mode is off.
        JBLabel agentModeBanner = new JBLabel(
                "Skills are only active when Agent mode is enabled (Settings \u2192 Agent).");
        agentModeBanner.setForeground(JBColor.namedColor("Label.warningForeground", JBColor.ORANGE));
        agentModeBanner.setVisible(!Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled()));
        gbc.gridy++;
        panel.add(agentModeBanner, gbc);

        JEditorPane description = new JEditorPane("text/html",
                "<html><body>"
                        + "Drop a <code>SKILL.md</code> file (plus optional resources) into one of "
                        + "the supported skill directories:"
                        + "<ul>"
                        + "<li><code>~/.agents/skills/&lt;name&gt;/</code> &mdash; user, shared with .agents tools</li>"
                        + "<li><code>~/.claude/skills/&lt;name&gt;/</code> &mdash; user, shared with Claude Code</li>"
                        + "<li><code>~/.devoxxgenie/skills/&lt;name&gt;/</code> &mdash; user, DevoxxGenie</li>"
                        + "<li><code>&lt;project&gt;/.agents/skills/&lt;name&gt;/</code> &mdash; project, .agents tools</li>"
                        + "<li><code>&lt;project&gt;/.claude/skills/&lt;name&gt;/</code> &mdash; project, Claude Code</li>"
                        + "<li><code>&lt;project&gt;/.devoxxgenie/skills/&lt;name&gt;/</code> &mdash; project, DevoxxGenie (highest priority)</li>"
                        + "</ul>"
                        + "On name collision the higher-priority source wins."
                        + "</body></html>");
        description.setEditable(false);
        description.setOpaque(false);
        gbc.gridy++;
        panel.add(description, gbc);

        // Toolbar 1: open-folder buttons (one per supported skill directory). The buttons
        // wrap onto multiple rows when the panel is narrow.
        JPanel folderToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folderToolbar.add(makeOpenFolderButton("Open user .devoxxgenie skills folder",
                SkillRegistry.Source.USER_DEVOXXGENIE));
        folderToolbar.add(makeOpenFolderButton("Open user .claude skills folder",
                SkillRegistry.Source.USER_CLAUDE));
        folderToolbar.add(makeOpenFolderButton("Open user .agents skills folder",
                SkillRegistry.Source.USER_AGENTS));
        folderToolbar.add(makeOpenFolderButton("Open project .devoxxgenie skills folder",
                SkillRegistry.Source.PROJECT_DEVOXXGENIE));
        folderToolbar.add(makeOpenFolderButton("Open project .claude skills folder",
                SkillRegistry.Source.PROJECT_CLAUDE));
        folderToolbar.add(makeOpenFolderButton("Open project .agents skills folder",
                SkillRegistry.Source.PROJECT_AGENTS));
        gbc.gridy++;
        panel.add(folderToolbar, gbc);

        // Toolbar 2: reload button.
        JPanel reloadToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reload = new JButton("Reload");
        reload.addActionListener(e -> {
            reload.setEnabled(false);
            // Off-load disk I/O to a pooled thread; the EDT callback re-enables the button
            // and refreshes the table from the freshly populated cache.
            registry.reloadAsync(() -> {
                refreshTable();
                reload.setEnabled(true);
            });
        });
        reloadToolbar.add(reload);

        gbc.gridy++;
        panel.add(reloadToolbar, gbc);

        // Table of detected skills.
        JBTable table = new JBTable(tableModel);
        table.setStriped(true);
        table.getColumnModel().getColumn(SkillsTableModel.COL_ENABLED).setMaxWidth(80);
        table.getColumnModel().getColumn(SkillsTableModel.COL_ENABLED).setMinWidth(60);
        table.getColumnModel().getColumn(SkillsTableModel.COL_SOURCE).setMaxWidth(180);
        table.getColumnModel().getColumn(SkillsTableModel.COL_SOURCE).setMinWidth(120);
        table.getColumnModel().getColumn(SkillsTableModel.COL_NAME).setPreferredWidth(160);
        table.getColumnModel().getColumn(SkillsTableModel.COL_DESCRIPTION).setPreferredWidth(420);

        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(-1, 320));
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);

        return panel;
    }

    public boolean isModified() {
        Set<String> persisted = DevoxxGenieStateService.getInstance().getDisabledSkillNames();
        if (persisted == null) {
            return !workingDisabledNames.isEmpty();
        }
        return !persisted.equals(workingDisabledNames);
    }

    public void apply() {
        DevoxxGenieStateService.getInstance().setDisabledSkillNames(new HashSet<>(workingDisabledNames));
        // Invalidate the derived caches so the next agent prompt picks up the new filter
        // without forcing a disk re-scan.
        registry.invalidateDerivedCaches();
    }

    public void reset() {
        resetWorkingSet();
        // No disk re-scan on reset — the cache already reflects what's on disk; the table just
        // needs to redraw against the working set.
        refreshTable();
    }

    private void resetWorkingSet() {
        workingDisabledNames.clear();
        Set<String> persisted = DevoxxGenieStateService.getInstance().getDisabledSkillNames();
        if (persisted != null) {
            workingDisabledNames.addAll(persisted);
        }
    }

    private void refreshTable() {
        tableModel.setEntries(registry.peekAllSkills());
    }

    private void openFolder(Path dir) {
        try {
            BrowserUtil.browse(dir.toUri());
        } catch (RuntimeException e) {
            log.warn("Could not open skills folder {}", dir, e);
        }
    }

    /**
     * Builds a button that opens the directory backing the given {@link SkillRegistry.Source}.
     * The directory is created on demand if it doesn't yet exist. Buttons that map to
     * project-scoped sources are disabled when the project has no on-disk basePath.
     */
    private JButton makeOpenFolderButton(@org.jetbrains.annotations.NotNull String label,
                                         @org.jetbrains.annotations.NotNull SkillRegistry.Source source) {
        JButton button = new JButton(label);
        Path dir = registry.directoryFor(source);
        button.setEnabled(dir != null);
        button.addActionListener(e -> {
            Path target = registry.ensureDirectoryExists(source);
            if (target != null) {
                openFolder(target);
            }
        });
        return button;
    }

    /**
     * Simple table model exposing one row per detected skill.
     */
    private final class SkillsTableModel extends AbstractTableModel {
        static final int COL_ENABLED = 0;
        static final int COL_SOURCE = 1;
        static final int COL_NAME = 2;
        static final int COL_DESCRIPTION = 3;

        private final String[] columns = {"Enabled", "Source", "Name", "Description"};
        private List<SkillRegistry.SkillEntry> entries = List.of();

        void setEntries(List<SkillRegistry.SkillEntry> newEntries) {
            this.entries = newEntries;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == COL_ENABLED ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COL_ENABLED;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SkillRegistry.SkillEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case COL_ENABLED -> !workingDisabledNames.contains(entry.name());
                case COL_SOURCE -> entry.source().label();
                case COL_NAME -> entry.name();
                case COL_DESCRIPTION -> entry.description();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != COL_ENABLED || !(aValue instanceof Boolean enabled)) {
                return;
            }
            SkillRegistry.SkillEntry entry = entries.get(rowIndex);
            if (enabled) {
                workingDisabledNames.remove(entry.name());
            } else {
                workingDisabledNames.add(entry.name());
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
