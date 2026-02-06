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
    private final JButton createDevoxxGenieMdButton = new JButton("Create DEVOXXGENIE.md");

    private final Project project;

    public PromptSettingsComponent(Project project) {
        this.project = project;
        createDevoxxGenieMdButton.addActionListener(e -> createDevoxxGenieMdFile());
        addListeners();
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

        addSection(panel, gbc, "Prompts");
        addPromptArea(panel, gbc, systemPromptField);

        addSection(panel, gbc, "DEVOXXGENIE.md Generation");

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createDevoxxGenieMdCheckbox, gbc);

        JPanel projectTreePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        projectTreePanel.add(includeProjectTreeCheckbox);
        projectTreePanel.add(new JLabel("Tree depth:"));
        projectTreePanel.add(projectTreeDepthSpinner);

        gbc.gridy++;
        panel.add(projectTreePanel, gbc);

        gbc.gridy++;
        panel.add(useDevoxxGenieMdInPromptCheckbox, gbc);

        gbc.gridy++;
        JEditorPane explanationPane = new JEditorPane(
                "text/html",
                "<html><body style='margin: 5px'>When enabled, the content of DEVOXXGENIE.md will be included in the prompt sent to the AI, "
                        + "providing it with context about your project structure and important files.</body></html>"
        );
        explanationPane.setEditable(false);
        explanationPane.setBackground(null);
        explanationPane.setBorder(null);
        panel.add(explanationPane, gbc);

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(createDevoxxGenieMdButton, gbc);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

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

        addSection(panel, gbc, "Configure keyboard submit shortcut");

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;

        if (SystemInfo.isWindows) {
            panel.add(createShortcutPanel("Windows", stateService.getSubmitShortcutWindows(), true), gbc);
        } else if (SystemInfo.isMac) {
            panel.add(createShortcutPanel("Mac", stateService.getSubmitShortcutMac(), true), gbc);
        } else {
            panel.add(createShortcutPanel("Linux", stateService.getSubmitShortcutLinux(), true), gbc);
        }

        addSection(panel, gbc, "Configure keyboard newline shortcut");

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        if (SystemInfo.isWindows) {
            panel.add(createNewlineShortcutPanel("Windows", stateService.getNewlineShortcutWindows()), gbc);
        } else if (SystemInfo.isMac) {
            panel.add(createNewlineShortcutPanel("Mac", stateService.getNewlineShortcutMac()), gbc);
        } else {
            panel.add(createNewlineShortcutPanel("Linux", stateService.getNewlineShortcutLinux()), gbc);
        }

        JEditorPane addFilesInfoPane = new JEditorPane(
                "text/html",
                "<html><body style='margin: 5px'>You can also trigger the add files popup dialog using @ in the input field.</body></html>"
        );
        addFilesInfoPane.setEditable(false);
        addFilesInfoPane.setBackground(null);
        addFilesInfoPane.setBorder(null);

        gbc.gridy++;
        panel.add(addFilesInfoPane, gbc);

        return panel;
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
