package com.devoxx.genie.ui.settings.llmconfig;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.SettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.FormBuilder;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
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
    private final JBIntSpinner timeoutField = new JBIntSpinner(new UINumericRange(stateService.getTimeout(), 1, 60));
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
        return FormBuilder.createFormBuilder()
            .addComponent(new JXTitledSeparator("Local Large Language Models"))
            .addVerticalGap(5)
            .addComponent(new JLabel("Chat Memory Size"))
            .addComponent(chatMemorySizeField)
            .addVerticalGap(5)
            .addComponent(new JLabel("Temperature"))
            .addComponent(temperatureField)
            .addVerticalGap(5)
            .addComponent(new JLabel("Top-P"))
            .addComponent(topPField)
            .addVerticalGap(5)
            .addComponent(new JLabel("Maximum Output Tokens"))
            .addComponent(maxOutputTokensField)
            .addVerticalGap(5)
            .addComponent(new JLabel("Timeout (in secs)"))
            .addComponent(timeoutField)
            .addVerticalGap(5)
            .addComponent(new JLabel("Maximum Retries"))
            .addComponent(retryField)
            .addVerticalGap(5)
            .addComponent(new JXTitledSeparator("Abstract Syntax Tree Config"))
            .addVerticalGap(5)
            .addComponent(new JLabel("Enable AST Mode (Beta)"))
            .addComponent(astMode)
            .addVerticalGap(5)
            .addComponent(new JLabel("Automatically incorporate code into the prompt window context based on the selected AST options."))
            .addVerticalGap(5)
            .addComponent(new JLabel("Include Project Parent Class(es)"))
            .addComponent(astParentClassCheckBox)
            .addVerticalGap(5)
            .addComponent(new JLabel("Include Class References"))
            .addComponent(astReferenceClassesCheckBox)
            .addVerticalGap(5)
            .addComponent(new JLabel("Include Field References"))
            .addComponent(astReferenceFieldCheckBox)
            .getPanel();
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
