package com.devoxx.genie.ui.settings.gitdiff;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import javax.swing.*;
import java.awt.*;

@Getter
public class GitDiffSettingsComponent extends AbstractSettingsComponent {

    private final JBCheckBox enableGitDiffCheckBox =
            new JBCheckBox("Enable feature", DevoxxGenieStateService.getInstance().getGitDiffEnabled());

    private final JLabel previewImageLabel = new JLabel();

    private final JEditorPane descriptionLabel = new JEditorPane("text/html", "");

    public GitDiffSettingsComponent() {
        updatePreviewImage(GitDiffMode.SIMPLE_DIFF);
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
        gbc.gridy++;
        panel.add(new JXTitledSeparator("LLM Git Diff Viewer"), gbc);

        gbc.gridy++;
        JPanel enablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        enablePanel.add(enableGitDiffCheckBox);
        panel.add(enablePanel, gbc);

        // Add description
        gbc.gridy++;
        JBLabel infoLabel = new JBLabel();
        infoLabel.setText("<html><body style='width: 100%;'>" +
                "The Git Diff feature allows you to review and commit LLM-suggested code changes<br>" +
                "using the IDEA Git's diff viewer.</body></html>");
        infoLabel.setForeground(UIUtil.getContextHelpForeground());
        infoLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(infoLabel, gbc);

        // Add description
        gbc.gridy++;
        panel.add(descriptionLabel, gbc);

        // Add preview image
        gbc.gridy++;
        panel.add(previewImageLabel, gbc);

        return panel;
    }
}
