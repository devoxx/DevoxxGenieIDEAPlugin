package com.devoxx.genie.ui.settings.llmconfig;

import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;

public class LLMConfigSettingsComponent extends AbstractSettingsComponent {

    private final DevoxxGenieSettingsService stateService = DevoxxGenieSettingsServiceProvider.getInstance();

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
    private final JCheckBox showExecutionTimeCheckBox = new JCheckBox("", stateService.getShowExecutionTime());

    public LLMConfigSettingsComponent() {
        addListeners();
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
        panel.add(new JLabel("Show details metrics (execution time, tokens, price)"), gbc);
        gbc.gridx = 1;
        panel.add(showExecutionTimeCheckBox, gbc);

        return panel;
    }
}
