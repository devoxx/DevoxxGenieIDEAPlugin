package com.devoxx.genie.ui.settings.spec;

import com.devoxx.genie.service.spec.BacklogConfigService;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Settings panel for Spec Driven Development features.
 */
public class SpecSettingsComponent extends AbstractSettingsComponent {

    private final Project project;

    private final JBCheckBox enableSpecBrowserCheckbox =
            new JBCheckBox("Enable Spec Browser", Boolean.TRUE.equals(stateService.getSpecBrowserEnabled()));

    private final JBTextField specDirectoryField = new JBTextField(
            stateService.getSpecDirectory() != null ? stateService.getSpecDirectory() : "backlog");

    private final JButton initBacklogButton = new JButton("Init Backlog");
    private final JBLabel statusLabel = new JBLabel();

    public SpecSettingsComponent(@NotNull Project project) {
        this.project = project;
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy = 0;

        // --- Spec Browser ---
        addSection(contentPanel, gbc, "Spec Browser");

        addFullWidthRow(contentPanel, gbc, enableSpecBrowserCheckbox);
        addHelpText(contentPanel, gbc,
                "Enable the Spec Browser tool window to browse and manage task spec files. " +
                "Compatible with Backlog.md format (markdown files with YAML frontmatter). " +
                "When enabled, 17 embedded backlog tools (task, document, milestone management) " +
                "are automatically available in agent mode. " +
                "Selecting a task and clicking 'Implement with Agent' will inject the full spec " +
                "as structured context into the LLM prompt.");

        JPanel dirRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        dirRow.add(new JBLabel("Spec directory:"));
        specDirectoryField.setColumns(20);
        dirRow.add(specDirectoryField);
        addFullWidthRow(contentPanel, gbc, dirRow);
        addHelpText(contentPanel, gbc,
                "Directory relative to the project root containing task spec files. " +
                "Default: \"backlog\" (compatible with Backlog.md).");

        // --- Getting Started ---
        addSection(contentPanel, gbc, "Getting Started");

        JPanel initRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        initBacklogButton.addActionListener(e -> performInit());
        initRow.add(initBacklogButton);
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        initRow.add(statusLabel);
        addFullWidthRow(contentPanel, gbc, initRow);
        updateInitButtonState();

        addHelpText(contentPanel, gbc,
                "Creates the backlog directory with the standard Backlog.md structure: " +
                "tasks/, docs/, completed/, and archive/ subdirectories, along with a config.yml " +
                "with default settings. " +
                "You can then create task specs as markdown files with YAML frontmatter, " +
                "or use the embedded backlog tools in agent mode to manage tasks programmatically. " +
                "The IDE provides a dedicated Spec Browser tool window where you can view the progress " +
                "of all tasks, inspect their details, and launch implementation directly with the AI agent.");

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
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
        gbc.insets = new Insets(0, 25, 8, 5);
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
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy++;
    }

    private void addCodeBlock(JPanel panel, GridBagConstraints gbc, String code) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.insets = new Insets(4, 25, 8, 5);
        JTextArea codeArea = new JTextArea(code);
        codeArea.setLineWrap(false);
        codeArea.setEditable(false);
        codeArea.setFocusable(true);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Label.font").getSize()));
        codeArea.setBackground(UIManager.getColor("EditorPane.background"));
        codeArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        panel.add(codeArea, gbc);
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy++;
    }

    private void updateInitButtonState() {
        boolean initialized = BacklogConfigService.getInstance(project).isBacklogInitialized();
        initBacklogButton.setEnabled(!initialized);
        statusLabel.setText(initialized ? "Backlog already initialized" : "");
    }

    private void performInit() {
        initBacklogButton.setEnabled(false);
        statusLabel.setText("Initializing...");

        final Exception[] error = {null};
        boolean cancelled = false;

        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();

                try {
                    // Step 1: Create directory structure
                    indicator.setText("Creating backlog directory structure...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.2);

                    BacklogConfigService.getInstance(project).initBacklog(project.getName());

                    indicator.checkCanceled();

                    // Step 2: Refresh VFS
                    indicator.setText("Refreshing project files...");
                    indicator.setFraction(0.6);

                    String basePath = project.getBasePath();
                    if (basePath != null) {
                        String specDirName = stateService.getSpecDirectory();
                        if (specDirName == null || specDirName.isEmpty()) {
                            specDirName = "backlog";
                        }
                        VirtualFile vf = LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(basePath + "/" + specDirName);
                        if (vf != null) {
                            vf.refresh(false, true);
                        }
                    }

                    indicator.checkCanceled();

                    // Step 3: Refresh spec service cache
                    indicator.setText("Updating spec browser...");
                    indicator.setFraction(0.9);

                    SpecService.getInstance(project).refresh();

                    indicator.setFraction(1.0);
                } catch (ProcessCanceledException e) {
                    throw e;
                } catch (Exception ex) {
                    error[0] = ex;
                }
            }, "Initializing Backlog", true, project);
        } catch (ProcessCanceledException e) {
            cancelled = true;
        }

        // Back on EDT — update UI
        if (cancelled) {
            statusLabel.setText("Cancelled");
            initBacklogButton.setEnabled(true);
        } else if (error[0] != null) {
            statusLabel.setText("Error: " + error[0].getMessage());
            initBacklogButton.setEnabled(true);
            NotificationUtil.sendNotification(project,
                    "Failed to initialize backlog: " + error[0].getMessage());
        } else {
            updateInitButtonState();
            NotificationUtil.sendNotification(project, "Backlog initialized successfully");
        }
    }

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return enableSpecBrowserCheckbox.isSelected() != Boolean.TRUE.equals(state.getSpecBrowserEnabled())
                || !Objects.equals(specDirectoryField.getText().trim(), state.getSpecDirectory());
    }

    public void apply() {
        stateService.setSpecBrowserEnabled(enableSpecBrowserCheckbox.isSelected());
        stateService.setSpecDirectory(specDirectoryField.getText().trim());
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        enableSpecBrowserCheckbox.setSelected(Boolean.TRUE.equals(state.getSpecBrowserEnabled()));
        specDirectoryField.setText(state.getSpecDirectory() != null ? state.getSpecDirectory() : "backlog");
    }

    @Override
    public void addListeners() {
        // No dynamic listeners needed
    }
}
