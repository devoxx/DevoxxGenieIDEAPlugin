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
 * loaded from {@code ~/.devoxxgenie/skills/} and {@code &lt;project&gt;/.devoxxgenie/skills/}.
 *
 * <p>The panel shows a table of detected skills, lets the user enable/disable each one, and
 * exposes shortcuts to open the source folders and reload from disk.</p>
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
        reset();
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
                        + "Drop a <code>SKILL.md</code> file (plus optional resources) into "
                        + "<code>~/.devoxxgenie/skills/&lt;name&gt;/</code> or "
                        + "<code>&lt;project&gt;/.devoxxgenie/skills/&lt;name&gt;/</code>. "
                        + "Project skills override user skills of the same name."
                        + "</body></html>");
        description.setEditable(false);
        description.setOpaque(false);
        gbc.gridy++;
        panel.add(description, gbc);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton openUserFolder = new JButton("Open user skills folder");
        openUserFolder.addActionListener(e -> openFolder(registry.userSkillsDir()));
        toolbar.add(openUserFolder);

        JButton openProjectFolder = new JButton("Open project skills folder");
        openProjectFolder.addActionListener(e -> {
            Path dir = registry.projectSkillsDir();
            if (dir != null) {
                openFolder(dir);
            }
        });
        openProjectFolder.setEnabled(registry.projectSkillsDir() != null);
        toolbar.add(openProjectFolder);

        JButton reload = new JButton("Reload");
        reload.addActionListener(e -> {
            registry.reload();
            tableModel.setEntries(registry.getAllSkills());
        });
        toolbar.add(reload);

        gbc.gridy++;
        panel.add(toolbar, gbc);

        // Table of detected skills.
        JBTable table = new JBTable(tableModel);
        table.setStriped(true);
        table.getColumnModel().getColumn(SkillsTableModel.COL_ENABLED).setMaxWidth(80);
        table.getColumnModel().getColumn(SkillsTableModel.COL_ENABLED).setMinWidth(60);
        table.getColumnModel().getColumn(SkillsTableModel.COL_SOURCE).setMaxWidth(80);
        table.getColumnModel().getColumn(SkillsTableModel.COL_SOURCE).setMinWidth(60);
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
    }

    public void reset() {
        workingDisabledNames.clear();
        Set<String> persisted = DevoxxGenieStateService.getInstance().getDisabledSkillNames();
        if (persisted != null) {
            workingDisabledNames.addAll(persisted);
        }
        tableModel.setEntries(registry.getAllSkills());
    }

    private void openFolder(Path dir) {
        try {
            registry.ensureDirectoriesExist();
            BrowserUtil.browse(dir.toUri());
        } catch (RuntimeException e) {
            log.warn("Could not open skills folder {}", dir, e);
        }
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
                case COL_SOURCE -> entry.source().name().toLowerCase();
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
