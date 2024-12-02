package com.devoxx.genie.ui.settings.gitmerge;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import com.intellij.openapi.ui.ComboBox;
import javax.swing.*;
import java.awt.*;

public class GitMergeSettingsComponent extends AbstractSettingsComponent {

    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    @Getter
    private final ComboBox<GitDiffMode> gitDiffModeComboBox;

    @Getter
    private final JLabel previewImageLabel = new JLabel();

    @Getter
    private final JEditorPane descriptionLabel = new JEditorPane("text/html", "");

    public GitMergeSettingsComponent() {
        // Create combobox with Git Diff modes
        gitDiffModeComboBox = new ComboBox<>(GitDiffMode.values());

        // Set custom renderer to display the enum's display name
        gitDiffModeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GitDiffMode) {
                    setText(((GitDiffMode) value).getDisplayName());
                }
                return this;
            }
        });

        // Set initial selected mode from state service
        GitDiffMode currentMode = determineCurrentMode();
        gitDiffModeComboBox.setSelectedItem(currentMode);

        // Add listener to update preview when selection changes
        gitDiffModeComboBox.addActionListener(e -> {
            GitDiffMode selectedMode = (GitDiffMode) gitDiffModeComboBox.getSelectedItem();
            updatePreviewImage(selectedMode);
        });
    }

    private GitDiffMode determineCurrentMode() {
        if (stateService.getUseDiffMerge()) {
            return GitDiffMode.DIFF_MERGE;
        } else if (stateService.getUseSimpleDiff()) {
            return GitDiffMode.SIMPLE_DIFF;
        }
        return GitDiffMode.DISABLED;
    }

    private void updatePreviewImage(GitDiffMode mode) {
        float scaleFactor = JBUIScale.scale(1f);
        try {
            try (var imageStream = getClass().getResourceAsStream(mode.getIconPath())) {
                if (imageStream != null) {
                    byte[] imageBytes = imageStream.readAllBytes();
                    ImageIcon icon = new ImageIcon(imageBytes);
                    previewImageLabel.setIcon(icon);
                    descriptionLabel.setText(
                        mode.getDescription()
                            .formatted(scaleFactor == 1.0f ? "normal" : scaleFactor * 100 + "%"));
                } else {
                    throw new IllegalStateException("Image not found: " + mode.getIconPath());
                }
            }
        } catch (Exception e) {
            previewImageLabel.setIcon(null);
        }
    }

    @Override
    public JPanel createPanel() {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);

        // Add title
        panel.add(new JXTitledSeparator("Git Diff Mode"), gbc);

        gbc.gridy++;
        panel.add(new JLabel("Commit LLM suggestions using a Git Diff/Merge view"), gbc);

        // Add combobox
        gbc.gridy++;
        gbc.gridx = 1;
        panel.add(new JLabel("Select Git Diff Mode:"), gbc);
        gbc.gridy++;
        panel.add(gitDiffModeComboBox, gbc);

        // Add description
        gbc.gridy++;
        panel.add(descriptionLabel, gbc);

        // Add preview image
        gbc.gridy++;
        panel.add(previewImageLabel, gbc);

        return panel;
    }
}
