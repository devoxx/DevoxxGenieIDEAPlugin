package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.service.analyzer.DevoxxGenieGenerator;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class PromptSettingsComponent extends AbstractSettingsComponent {

    @Getter
    @Setter
    private String submitShortcutWindows;

    @Getter
    @Setter
    private String submitShortcutMac;

    @Getter
    @Setter
    private String submitShortcutLinux;

    @Getter
    @Setter
    private String newlineShortcutWindows;

    @Getter
    @Setter
    private String newlineShortcutMac;

    @Getter
    @Setter
    private String newlineShortcutLinux;

    @Getter
    private final JTextArea systemPromptField = new JTextArea(stateService.getSystemPrompt());

    @Getter
    private final JCheckBox createDevoxxGenieMdCheckbox = new JCheckBox("Generate DEVOXXGENIE.md file", stateService.getCreateDevoxxGenieMd());

    @Getter
    private final JCheckBox includeProjectTreeCheckbox = new JCheckBox("Include project tree", stateService.getIncludeProjectTree());

    @Getter
    private final JSpinner projectTreeDepthSpinner = new JSpinner(new SpinnerNumberModel(stateService.getProjectTreeDepth().intValue(), 1, 10, 1));

    @Getter
    private final JCheckBox useDevoxxGenieMdInPromptCheckbox = new JCheckBox("Use DEVOXXGENIE.md in prompt", stateService.getUseDevoxxGenieMdInPrompt());

    @Getter
    private final JCheckBox useClaudeOrAgentsMdInPromptCheckbox = new JCheckBox("Use CLAUDE.md or AGENTS.md in prompt", stateService.getUseClaudeOrAgentsMdInPrompt());

    @Getter
    private final JButton createDevoxxGenieMdButton = new JButton("Create DEVOXXGENIE.md");

    private final Project project;

    public PromptSettingsComponent(Project project) {
        this.project = project;
        createDevoxxGenieMdButton.addActionListener(e -> createDevoxxGenieMdFile());
        addListeners();
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/configuration/prompts";
    }

    @Override
    public JPanel createPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(4, 5);
        gbc.gridy = 0;

        // --- System Prompt ---
        addSection(contentPanel, gbc, "Prompts");
        addPromptArea(contentPanel, gbc, systemPromptField);

        // --- DEVOXXGENIE.md Generation ---
        addSection(contentPanel, gbc, "DEVOXXGENIE.md Generation");

        addFullWidthRow(contentPanel, gbc, createDevoxxGenieMdCheckbox);

        JPanel projectTreePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        projectTreePanel.add(includeProjectTreeCheckbox);
        projectTreePanel.add(new JLabel("Tree depth:"));
        projectTreePanel.add(projectTreeDepthSpinner);
        addFullWidthRow(contentPanel, gbc, projectTreePanel);

        addFullWidthRow(contentPanel, gbc, useDevoxxGenieMdInPromptCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the content of DEVOXXGENIE.md will be included in the prompt sent to the AI, " +
                "providing it with context about your project structure and important files.");

        addFullWidthRow(contentPanel, gbc, createDevoxxGenieMdButton);

        // --- CLAUDE.md / AGENTS.md Inclusion ---
        addSection(contentPanel, gbc, "CLAUDE.md / AGENTS.md Inclusion");

        addFullWidthRow(contentPanel, gbc, useClaudeOrAgentsMdInPromptCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the plugin will check for CLAUDE.md or AGENTS.md files in your project root. " +
                "If both files exist, CLAUDE.md takes priority and AGENTS.md is skipped. " +
                "The content will be included in the prompt to provide AI-specific context and instructions.");

        createDevoxxGenieMdCheckbox.addChangeListener(e -> {
            boolean enabled = createDevoxxGenieMdCheckbox.isSelected();
            includeProjectTreeCheckbox.setEnabled(enabled);
            projectTreeDepthSpinner.setEnabled(enabled && includeProjectTreeCheckbox.isSelected());
            useDevoxxGenieMdInPromptCheckbox.setEnabled(enabled);
            createDevoxxGenieMdButton.setEnabled(enabled);
        });

        includeProjectTreeCheckbox.addChangeListener(e ->
                projectTreeDepthSpinner.setEnabled(createDevoxxGenieMdCheckbox.isSelected() && includeProjectTreeCheckbox.isSelected())
        );

        includeProjectTreeCheckbox.setEnabled(createDevoxxGenieMdCheckbox.isSelected());
        projectTreeDepthSpinner.setEnabled(createDevoxxGenieMdCheckbox.isSelected() && includeProjectTreeCheckbox.isSelected());
        useDevoxxGenieMdInPromptCheckbox.setEnabled(createDevoxxGenieMdCheckbox.isSelected());
        createDevoxxGenieMdButton.setEnabled(createDevoxxGenieMdCheckbox.isSelected());

        // --- Keyboard Shortcuts ---
        addSection(contentPanel, gbc, "Configure keyboard submit shortcut");

        if (SystemInfo.isWindows) {
            addFullWidthRow(contentPanel, gbc, createShortcutPanel("Windows", stateService.getSubmitShortcutWindows(), true));
        } else if (SystemInfo.isMac) {
            addFullWidthRow(contentPanel, gbc, createShortcutPanel("Mac", stateService.getSubmitShortcutMac(), true));
        } else {
            addFullWidthRow(contentPanel, gbc, createShortcutPanel("Linux", stateService.getSubmitShortcutLinux(), true));
        }

        addSection(contentPanel, gbc, "Configure keyboard newline shortcut");

        if (SystemInfo.isWindows) {
            addFullWidthRow(contentPanel, gbc, createNewlineShortcutPanel("Windows", stateService.getNewlineShortcutWindows()));
        } else if (SystemInfo.isMac) {
            addFullWidthRow(contentPanel, gbc, createNewlineShortcutPanel("Mac", stateService.getNewlineShortcutMac()));
        } else {
            addFullWidthRow(contentPanel, gbc, createNewlineShortcutPanel("Linux", stateService.getNewlineShortcutLinux()));
        }

        addHelpText(contentPanel, gbc,
                "You can also trigger the add files popup dialog using @ in the input field.");

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
        return panel;
    }

    private void addFullWidthRow(JPanel panel, GridBagConstraints gbc, JComponent component) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        panel.add(component, gbc);
        gbc.gridy++;
    }

    private void addHelpText(JPanel panel, GridBagConstraints gbc, String text) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.insets = JBUI.insets(0, 25, 8, 5);
        JTextArea helpArea = new JTextArea(text);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setEditable(false);
        helpArea.setFocusable(false);
        helpArea.setOpaque(false);
        helpArea.setBorder(null);
        helpArea.setFont(UIManager.getFont("Label.font").deriveFont((float) UIManager.getFont("Label.font").getSize() - 1));
        helpArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(helpArea, gbc);
        gbc.insets = JBUI.insets(4, 5);
        gbc.gridy++;
    }

    private @NotNull JPanel createShortcutPanel(String os, String initialShortcut, boolean isSubmitShortcut) {
        KeyboardShortcutPanel shortcutPanel = new KeyboardShortcutPanel(project, os, initialShortcut, shortcut -> {
            if (isSubmitShortcut) {
                if ("Mac".equalsIgnoreCase(os)) {
                    setSubmitShortcutMac(shortcut);
                } else if ("Windows".equalsIgnoreCase(os)) {
                    setSubmitShortcutWindows(shortcut);
                } else {
                    setSubmitShortcutLinux(shortcut);
                }
                notifyShortcutChanged(shortcut);
            } else {
                if ("Mac".equalsIgnoreCase(os)) {
                    setNewlineShortcutMac(shortcut);
                } else if ("Windows".equalsIgnoreCase(os)) {
                    setNewlineShortcutWindows(shortcut);
                } else {
                    setNewlineShortcutLinux(shortcut);
                }
                notifyNewlineShortcutChanged(shortcut);
            }
        });

        if (isSubmitShortcut) {
            if ("Mac".equalsIgnoreCase(os)) {
                submitShortcutMac = shortcutPanel.getCurrentShortcut();
            } else if ("Windows".equalsIgnoreCase(os)) {
                submitShortcutWindows = shortcutPanel.getCurrentShortcut();
            } else {
                submitShortcutLinux = shortcutPanel.getCurrentShortcut();
            }
        } else {
            if ("Mac".equalsIgnoreCase(os)) {
                newlineShortcutMac = shortcutPanel.getCurrentShortcut();
            } else if ("Windows".equalsIgnoreCase(os)) {
                newlineShortcutWindows = shortcutPanel.getCurrentShortcut();
            } else {
                newlineShortcutLinux = shortcutPanel.getCurrentShortcut();
            }
        }

        return shortcutPanel;
    }

    private @NotNull JPanel createNewlineShortcutPanel(String os, String initialShortcut) {
        return createShortcutPanel(os, initialShortcut, false);
    }

    private void addPromptArea(@NotNull JPanel panel,
                               @NotNull GridBagConstraints gbc,
                               @NotNull JTextArea textArea) {
        gbc.gridy++;
        panel.add(new JLabel("System instruction"), gbc);

        gbc.gridy++;
        textArea.setRows(5);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(-1, 100));
        panel.add(scrollPane, gbc);
    }

    private void notifyShortcutChanged(String shortcut) {
        project.getMessageBus()
                .syncPublisher(AppTopics.SHORTCUT_CHANGED_TOPIC)
                .onShortcutChanged(shortcut);
    }

    private void notifyNewlineShortcutChanged(String shortcut) {
        project.getMessageBus()
                .syncPublisher(AppTopics.NEWLINE_SHORTCUT_CHANGED_TOPIC)
                .onNewlineShortcutChanged(shortcut);
    }

    private void createDevoxxGenieMdFile() {
        createDevoxxGenieMdButton.setEnabled(false);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating DEVOXXGENIE.md", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Generating DEVOXXGENIE.md file...");

                    boolean includeTree = includeProjectTreeCheckbox.isSelected();
                    int treeDepth = (Integer) projectTreeDepthSpinner.getValue();

                    DevoxxGenieGenerator devoxxGenieGenerator =
                            new DevoxxGenieGenerator(project, includeTree, treeDepth, indicator);
                    devoxxGenieGenerator.generate();
                } finally {
                    ApplicationManager.getApplication().invokeLater(() ->
                            createDevoxxGenieMdButton.setEnabled(createDevoxxGenieMdCheckbox.isSelected())
                    );
                }
            }
        });
    }
}
