package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.SettingsComponent;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class PromptSettingsComponent implements SettingsComponent {

    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    @Getter
    private final JTextArea systemPromptField = new JTextArea(stateService.getSystemPrompt());
    @Getter
    private final JTextArea testPromptField = new JTextArea(stateService.getTestPrompt());
    @Getter
    private final JTextArea explainPromptField = new JTextArea(stateService.getExplainPrompt());
    @Getter
    private final JTextArea reviewPromptField = new JTextArea(stateService.getReviewPrompt());
    @Getter
    private final JTextArea customPromptField = new JTextArea(stateService.getCustomPrompt());

    public PromptSettingsComponent() {
        addListeners();
    }

    @Override
    public JPanel createPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);

        addSection(panel, gbc, "Prompts");

        addPromptArea(panel, gbc, "System prompt", systemPromptField);
        addPromptArea(panel, gbc, "/Test prompt", testPromptField);
        addPromptArea(panel, gbc, "/Explain prompt", explainPromptField);
        addPromptArea(panel, gbc, "/Review prompt", reviewPromptField);
        addPromptArea(panel, gbc, "/Custom prompt", customPromptField);

        // Add vertical glue to push everything to the top
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private void addSection(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, String title) {
        gbc.gridy++;
        panel.add(new JXTitledSeparator(title), gbc);
        gbc.gridy++;
    }

    private void addPromptArea(JPanel panel, GridBagConstraints gbc, String label, JTextArea textArea) {
        gbc.gridy++;
        panel.add(new JLabel(label), gbc);

        gbc.gridy++;
        textArea.setRows(5);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, gbc);
    }

    @Override
    public void addListeners() {
        // Add any listeners if needed in the future
    }
}
