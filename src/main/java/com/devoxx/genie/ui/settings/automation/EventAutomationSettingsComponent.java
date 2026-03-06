package com.devoxx.genie.ui.settings.automation;

import com.devoxx.genie.model.automation.*;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings component for Event Automation — maps IDE events to agent triggers.
 */
public class EventAutomationSettingsComponent extends AbstractSettingsComponent {

    private final EventAutomationTableModel tableModel;
    private final JTable automationTable;
    private final JCheckBox enableAutomationCheckbox;
    private boolean isModified = false;

    public EventAutomationSettingsComponent() {
        tableModel = new EventAutomationTableModel();
        automationTable = new JBTable(tableModel);

        enableAutomationCheckbox = new JCheckBox("Enable Event Automations");
        enableAutomationCheckbox.addActionListener(e -> isModified = true);

        setupTable();

        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(automationTable)
                .setAddAction(button -> addMapping())
                .setEditAction(button -> editMapping())
                .setRemoveAction(button -> removeMapping());

        JPanel decoratedTablePanel = toolbarDecorator.createPanel();

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("<html>" +
                "<b>Event Automations</b> — Automatically trigger AI agents in response to IDE events.<br>" +
                "Configure which agent runs when specific events occur (e.g., run Code Review on commit, " +
                "Debug Agent on test failure).<br>" +
                "Default automations are provided but disabled — enable the ones you want.</html>"),
                BorderLayout.CENTER);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

        // Checkbox panel
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkboxPanel.add(enableAutomationCheckbox);

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoPanel, BorderLayout.NORTH);
        topPanel.add(checkboxPanel, BorderLayout.SOUTH);

        // Load defaults button
        JButton loadDefaultsButton = new JButton("Load Default Automations");
        loadDefaultsButton.setToolTipText("Add the default set of event-agent mappings (won't duplicate existing)");
        loadDefaultsButton.addActionListener(e -> loadDefaults());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(loadDefaultsButton);

        // Build main panel
        panel.setLayout(new BorderLayout());
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(decoratedTablePanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        ApplicationManager.getApplication().invokeLater(() -> {
            loadCurrentSettings();
            enableAutomationCheckbox.setSelected(stateService.getEventAutomationEnabled());
            isModified = false;
        });
    }

    private void setupTable() {
        automationTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // Enabled
        automationTable.getColumnModel().getColumn(0).setMaxWidth(60);
        automationTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // Category
        automationTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Event
        automationTable.getColumnModel().getColumn(3).setPreferredWidth(180);  // Agent
        automationTable.getColumnModel().getColumn(4).setPreferredWidth(70);   // Auto-run
        automationTable.getColumnModel().getColumn(4).setMaxWidth(80);

        // Center align category and auto-run columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        automationTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Listen for enabled checkbox changes
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                isModified = true;
            }
        });
    }

    private void loadCurrentSettings() {
        List<EventAgentMapping> mappings = stateService.getEventAutomationSettings().getMappings();
        if (mappings.isEmpty()) {
            // First time: load defaults
            tableModel.setMappings(EventAutomationSettings.getDefaultMappings());
        } else {
            tableModel.setMappings(new ArrayList<>(mappings));
        }
    }

    private void loadDefaults() {
        List<EventAgentMapping> defaults = EventAutomationSettings.getDefaultMappings();
        List<EventAgentMapping> current = tableModel.getMappings();

        int added = 0;
        for (EventAgentMapping def : defaults) {
            boolean exists = current.stream().anyMatch(m ->
                    m.getEventType().equals(def.getEventType()) && m.getAgentType().equals(def.getAgentType()));
            if (!exists) {
                tableModel.addMapping(def);
                added++;
            }
        }

        if (added > 0) {
            isModified = true;
            Messages.showInfoMessage("Added " + added + " default automation(s).", "Defaults Loaded");
        } else {
            Messages.showInfoMessage("All default automations already exist.", "No Changes");
        }
    }

    private void addMapping() {
        EventAgentDialog dialog = new EventAgentDialog(null);
        if (dialog.showAndGet()) {
            tableModel.addMapping(dialog.getMapping());
            isModified = true;
        }
    }

    private void editMapping() {
        int selectedRow = automationTable.getSelectedRow();
        if (selectedRow >= 0) {
            EventAgentMapping existing = tableModel.getMappingAt(selectedRow);
            EventAgentDialog dialog = new EventAgentDialog(existing);
            if (dialog.showAndGet()) {
                tableModel.updateMapping(selectedRow, dialog.getMapping());
                isModified = true;
            }
        }
    }

    private void removeMapping() {
        int selectedRow = automationTable.getSelectedRow();
        if (selectedRow >= 0) {
            int result = Messages.showYesNoDialog(
                    "Are you sure you want to remove this event automation?",
                    "Confirm Removal",
                    Messages.getQuestionIcon()
            );
            if (result == Messages.YES) {
                tableModel.removeMapping(selectedRow);
                isModified = true;
            }
        }
    }

    public void apply() {
        if (isModified || enableAutomationCheckbox.isSelected() != stateService.getEventAutomationEnabled()) {
            stateService.setEventAutomationEnabled(enableAutomationCheckbox.isSelected());

            EventAutomationSettings settings = stateService.getEventAutomationSettings();
            settings.getMappings().clear();
            settings.getMappings().addAll(tableModel.getMappings());

            isModified = false;
        }
    }

    public boolean isModified() {
        return isModified ||
                enableAutomationCheckbox.isSelected() != stateService.getEventAutomationEnabled();
    }

    public void reset() {
        loadCurrentSettings();
        enableAutomationCheckbox.setSelected(stateService.getEventAutomationEnabled());
        isModified = false;
    }

    /**
     * Table model for event-agent automation mappings.
     */
    private static class EventAutomationTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Enabled", "Category", "IDE Event", "Agent", "Auto-run"};

        @Getter
        private List<EventAgentMapping> mappings = new ArrayList<>();

        public void setMappings(List<EventAgentMapping> mappings) {
            this.mappings = new ArrayList<>(mappings);
            fireTableDataChanged();
        }

        public @Nullable EventAgentMapping getMappingAt(int row) {
            if (row >= 0 && row < mappings.size()) {
                return mappings.get(row);
            }
            return null;
        }

        public void addMapping(EventAgentMapping mapping) {
            mappings.add(mapping);
            fireTableRowsInserted(mappings.size() - 1, mappings.size() - 1);
        }

        public void updateMapping(int row, EventAgentMapping mapping) {
            if (row >= 0 && row < mappings.size()) {
                // Preserve the enabled state from the table
                mapping.setEnabled(mappings.get(row).isEnabled());
                mappings.set(row, mapping);
                fireTableRowsUpdated(row, row);
            }
        }

        public void removeMapping(int row) {
            if (row >= 0 && row < mappings.size()) {
                mappings.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        @Override
        public int getRowCount() {
            return mappings.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
            EventAgentMapping mapping = mappings.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> mapping.isEnabled();
                case 1 -> getCategoryDisplay(mapping);
                case 2 -> getEventDisplay(mapping);
                case 3 -> getAgentDisplay(mapping);
                case 4 -> mapping.isAutoRun() ? "Yes" : "No";
                default -> null;
            };
        }

        private @NotNull String getCategoryDisplay(EventAgentMapping mapping) {
            try {
                return IdeEventType.valueOf(mapping.getEventType()).getCategory().getDisplayName();
            } catch (IllegalArgumentException e) {
                return "Unknown";
            }
        }

        private @NotNull String getEventDisplay(EventAgentMapping mapping) {
            try {
                return IdeEventType.valueOf(mapping.getEventType()).getDisplayName();
            } catch (IllegalArgumentException e) {
                return mapping.getEventType();
            }
        }

        private @NotNull String getAgentDisplay(EventAgentMapping mapping) {
            try {
                AgentType agentType = AgentType.valueOf(mapping.getAgentType());
                if (agentType == AgentType.CUSTOM && !mapping.getCustomAgentName().isEmpty()) {
                    return "Custom: " + mapping.getCustomAgentName();
                }
                return agentType.getDisplayName();
            } catch (IllegalArgumentException e) {
                return mapping.getAgentType();
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0; // Only enabled checkbox is editable inline
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean enabled) {
                mappings.get(rowIndex).setEnabled(enabled);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}
