package com.devoxx.genie.ui.settings.automation;

import com.devoxx.genie.model.automation.AgentType;
import com.devoxx.genie.model.automation.EventAgentMapping;
import com.devoxx.genie.model.automation.IdeEventType;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating or editing an event-agent mapping.
 */
public class EventAgentDialog extends DialogWrapper {

    private final ComboBox<IdeEventType> eventTypeCombo;
    private final ComboBox<AgentType> agentTypeCombo;
    private final JBTextField customNameField;
    private final JTextArea promptArea;
    private final JCheckBox autoRunCheckbox;
    private final JLabel customNameLabel;

    public EventAgentDialog(@Nullable EventAgentMapping existing) {
        super(true);

        eventTypeCombo = new ComboBox<>(IdeEventType.values());
        eventTypeCombo.setRenderer(new EventTypeRenderer());

        agentTypeCombo = new ComboBox<>(AgentType.values());
        agentTypeCombo.setRenderer(new AgentTypeRenderer());

        customNameField = new JBTextField();
        customNameLabel = new JLabel("Custom Agent Name:");

        promptArea = new JTextArea(6, 50);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);

        autoRunCheckbox = new JCheckBox("Auto-run (no confirmation dialog)");

        // Update prompt when agent type changes
        agentTypeCombo.addActionListener(e -> {
            AgentType selected = (AgentType) agentTypeCombo.getSelectedItem();
            if (selected != null && selected != AgentType.CUSTOM) {
                promptArea.setText(selected.getDefaultPrompt());
            }
            updateCustomFieldVisibility(selected);
        });

        if (existing != null) {
            try {
                eventTypeCombo.setSelectedItem(IdeEventType.valueOf(existing.getEventType()));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                AgentType agentType = AgentType.valueOf(existing.getAgentType());
                agentTypeCombo.setSelectedItem(agentType);
                updateCustomFieldVisibility(agentType);
            } catch (IllegalArgumentException ignored) {
            }
            customNameField.setText(existing.getCustomAgentName());
            promptArea.setText(existing.getPrompt());
            autoRunCheckbox.setSelected(existing.isAutoRun());
        } else {
            // Default: populate prompt from first agent type
            AgentType firstAgent = (AgentType) agentTypeCombo.getSelectedItem();
            if (firstAgent != null) {
                promptArea.setText(firstAgent.getDefaultPrompt());
                updateCustomFieldVisibility(firstAgent);
            }
        }

        init();
        setTitle(existing != null ? "Edit Event Automation" : "Add Event Automation");
    }

    private void updateCustomFieldVisibility(AgentType agentType) {
        boolean isCustom = agentType == AgentType.CUSTOM;
        customNameField.setVisible(isCustom);
        customNameLabel.setVisible(isCustom);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Event Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("IDE Event:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(eventTypeCombo, gbc);

        // Event description
        gbc.gridx = 1;
        gbc.gridy = 1;
        JLabel eventDescLabel = new JLabel();
        eventDescLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        eventDescLabel.setFont(eventDescLabel.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(eventDescLabel, gbc);

        eventTypeCombo.addActionListener(e -> {
            IdeEventType sel = (IdeEventType) eventTypeCombo.getSelectedItem();
            if (sel != null) {
                eventDescLabel.setText(sel.getDescription());
            }
        });
        // Trigger initial
        IdeEventType initialEvent = (IdeEventType) eventTypeCombo.getSelectedItem();
        if (initialEvent != null) {
            eventDescLabel.setText(initialEvent.getDescription());
        }

        // Agent Type
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Agent:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(agentTypeCombo, gbc);

        // Agent description
        gbc.gridx = 1;
        gbc.gridy = 3;
        JLabel agentDescLabel = new JLabel();
        agentDescLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        agentDescLabel.setFont(agentDescLabel.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(agentDescLabel, gbc);

        agentTypeCombo.addActionListener(e -> {
            AgentType sel = (AgentType) agentTypeCombo.getSelectedItem();
            if (sel != null) {
                agentDescLabel.setText(sel.getDescription());
            }
        });
        AgentType initialAgent = (AgentType) agentTypeCombo.getSelectedItem();
        if (initialAgent != null) {
            agentDescLabel.setText(initialAgent.getDescription());
        }

        // Custom name
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        panel.add(customNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(customNameField, gbc);

        // Prompt
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Agent Prompt:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JBScrollPane scrollPane = new JBScrollPane(promptArea);
        scrollPane.setPreferredSize(new Dimension(400, 120));
        panel.add(scrollPane, gbc);

        // Auto-run checkbox
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(autoRunCheckbox, gbc);

        panel.setPreferredSize(new Dimension(550, 350));
        return panel;
    }

    public @NotNull EventAgentMapping getMapping() {
        IdeEventType eventType = (IdeEventType) eventTypeCombo.getSelectedItem();
        AgentType agentType = (AgentType) agentTypeCombo.getSelectedItem();

        return EventAgentMapping.builder()
                .enabled(true)
                .eventType(eventType != null ? eventType.name() : IdeEventType.BEFORE_COMMIT.name())
                .agentType(agentType != null ? agentType.name() : AgentType.CODE_REVIEW.name())
                .customAgentName(customNameField.getText().trim())
                .prompt(promptArea.getText().trim())
                .autoRun(autoRunCheckbox.isSelected())
                .build();
    }

    /**
     * Renderer for IDE event types in combo box, grouped by category.
     */
    private static class EventTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof IdeEventType eventType) {
                setText("[" + eventType.getCategory().getDisplayName() + "] " + eventType.getDisplayName());
            }
            return this;
        }
    }

    /**
     * Renderer for agent types in combo box.
     */
    private static class AgentTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AgentType agentType) {
                setText(agentType.getDisplayName());
            }
            return this;
        }
    }
}
