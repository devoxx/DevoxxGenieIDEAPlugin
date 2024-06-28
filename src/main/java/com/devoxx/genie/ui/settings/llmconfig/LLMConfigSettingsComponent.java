package com.devoxx.genie.ui.settings.llmconfig;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.SettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class LLMConfigSettingsComponent implements SettingsComponent {

    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    @Getter
    private final JBIntSpinner chatMemorySizeField = new JBIntSpinner(new UINumericRange(stateService.getChatMemorySize(), 1, 100));
    @Getter
    private final JSpinner temperatureField = new JSpinner(new SpinnerNumberModel(stateService.getTemperature().doubleValue(), 0.0d, 1.0d, 0.1d));
    @Getter
    private final JSpinner topPField = new JSpinner(new SpinnerNumberModel(stateService.getTopP().doubleValue(), 0.0d, 1.0d, 0.1d));
    @Getter
    private final JBIntSpinner maxOutputTokensField = new JBIntSpinner(new UINumericRange(stateService.getMaxOutputTokens(), 1, 1_000_000));
    @Getter
    private final JBIntSpinner timeoutField = new JBIntSpinner(new UINumericRange(stateService.getTimeout(), 1, 600));
    @Getter
    private final JBIntSpinner retryField = new JBIntSpinner(new UINumericRange(stateService.getMaxRetries(), 1, 5));
    @Getter
    private final JCheckBox astMode = new JCheckBox("", stateService.getAstMode());
    @Getter
    private final JCheckBox astParentClassCheckBox = new JCheckBox("", stateService.getAstParentClass());
    @Getter
    private final JCheckBox astReferenceClassesCheckBox = new JCheckBox("", stateService.getAstClassReference());
    @Getter
    private final JCheckBox astReferenceFieldCheckBox = new JCheckBox("", stateService.getAstFieldReference());

    public LLMConfigSettingsComponent() {
        addListeners();
    }

    @Override
    public JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        panel.add(new JXTitledSeparator("Local Large Language Models"), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Chat Memory Size"), gbc);
        gbc.gridx = 1;
        panel.add(chatMemorySizeField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Temperature"), gbc);
        gbc.gridx = 1;
        panel.add(temperatureField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Top-P"), gbc);
        gbc.gridx = 1;
        panel.add(topPField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Maximum Output Tokens"), gbc);
        gbc.gridx = 1;
        panel.add(maxOutputTokensField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Timeout (in secs)"), gbc);
        gbc.gridx = 1;
        panel.add(timeoutField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Maximum Attempts"), gbc);
        gbc.gridx = 1;
        panel.add(retryField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JXTitledSeparator("Abstract Syntax Tree Config"), gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Automatically incorporate code into the prompt window context based on the selected AST options."), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Enable AST Mode (Beta)"), gbc);
        gbc.gridx = 1;
        panel.add(astMode, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Include Project Parent Class(es)"), gbc);
        gbc.gridx = 1;
        panel.add(astParentClassCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Include Class References"), gbc);
        gbc.gridx = 1;
        panel.add(astReferenceClassesCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Include Field References"), gbc);
        gbc.gridx = 1;
        panel.add(astReferenceFieldCheckBox, gbc);

        return panel;
    }

    @Override
    public void addListeners() {
        astMode.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            astParentClassCheckBox.setEnabled(selected);
            astReferenceClassesCheckBox.setEnabled(selected);
            astReferenceFieldCheckBox.setEnabled(selected);
        });
    }
}
