package com.devoxx.genie.ui.settings.llmconfig;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;

@Getter
public class LLMConfigSettingsComponent extends AbstractSettingsComponent {

    private final JBIntSpinner chatMemorySizeField = new JBIntSpinner(new UINumericRange(stateService.getChatMemorySize(), 1, 500));
    private final JSpinner temperatureField = new JSpinner(new SpinnerNumberModel(stateService.getTemperature().doubleValue(), 0.0d, 2.0d, 0.1d));
    private final JSpinner topPField = new JSpinner(new SpinnerNumberModel(stateService.getTopP().doubleValue(), 0.0d, 1.0d, 0.1d));
    private final JBIntSpinner maxOutputTokensField = new JBIntSpinner(new UINumericRange(stateService.getMaxOutputTokens(), 1, 1_000_000));
    private final JBIntSpinner timeoutField = new JBIntSpinner(new UINumericRange(stateService.getTimeout(), 1, Integer.MAX_VALUE));
    private final JBIntSpinner retryField = new JBIntSpinner(new UINumericRange(stateService.getMaxRetries(), 1, 5));

    private final JCheckBox useFileInEditorCheckBox = new JCheckBox("", stateService.getUseFileInEditor());

    public LLMConfigSettingsComponent() {
        addListeners();
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/configuration/settings";
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

        panel.add(new JXTitledSeparator("LLM Chat Settings"), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Chat Memory Size"), gbc);
        gbc.gridx = 1;
        panel.add(chatMemorySizeField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        var temperatureLabel = new JLabel("Temperature");
        temperatureLabel.setToolTipText("""
                <html>
                <body>
                Low temperature = a more deterministic result<br>
                High temperature = a more diverse or creative result<br><br>
                Advised temperature values per use case:<br>
                <ul>
                  <li>Coding/Math = 0</li>
                  <li>Data Cleaning/Data Analysis = 0.7</li>
                  <li>General Conversation = 1.0</li>
                  <li>Translation = 1.1</li>
                  <li>Creative Writing/Poetry = 1.25</li>
                </ul>
                </body>
                </html>
                """);
        panel.add(temperatureLabel, gbc);
        gbc.gridx = 1;
        panel.add(temperatureField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        var topPLabel = new JLabel("Top-P");
        topPLabel.setToolTipText("""
                <html>
                <body>
                A sampling technique with temperature, called nucleus sampling, where you can control how deterministic the model is.
                If you are looking for exact and factual answers keep this low. If you are looking for more diverse responses, increase to a higher value.
                If you use Top P it means that only the tokens comprising the top_p probability mass are considered for responses,
                so a low top_p value selects the most confident responses. This means that a high top_p value will enable the model to look at more possible words,
                including less likely ones, leading to more diverse outputs.<br>
                The general recommendation is to alter temperature or Top P but not both.
                </body>
                </html>
                """);
        panel.add(topPLabel, gbc);
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
        panel.add(new JLabel("Include currently open file in context"), gbc);
        gbc.gridx = 1;
        panel.add(useFileInEditorCheckBox, gbc);

        return panel;
    }
}
