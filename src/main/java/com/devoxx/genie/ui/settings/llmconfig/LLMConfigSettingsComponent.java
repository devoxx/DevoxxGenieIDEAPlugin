package com.devoxx.genie.ui.settings.llmconfig;

import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.FormBuilder;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.event.ItemEvent;

public class LLMConfigSettingsComponent {

    public LLMConfigStateService llmConfigStateService = LLMConfigStateService.getInstance();

    @Getter
    private final JPanel panel;

    @Getter
    private final JBIntSpinner chatMemorySizeField = new JBIntSpinner(new UINumericRange(llmConfigStateService.getChatMemorySize(), 1, 100));
    @Getter
    private final JSpinner temperatureField = new JSpinner(new SpinnerNumberModel(llmConfigStateService.getTemperature().doubleValue(), 0.0d, 1.0d, 0.1d));
    @Getter
    private final JSpinner topPField = new JSpinner(new SpinnerNumberModel(llmConfigStateService.getTopP().doubleValue(), 0.0d, 1.0d, 0.1d));
    @Getter
    private final JBIntSpinner maxOutputTokensField = new JBIntSpinner(new UINumericRange(llmConfigStateService.getMaxOutputTokens(), 1, 1_000_000));
    @Getter
    private final JBIntSpinner timeoutField = new JBIntSpinner(new UINumericRange(llmConfigStateService.getTimeout(), 1, 60));
    @Getter
    private final JBIntSpinner retryField = new JBIntSpinner(new UINumericRange(llmConfigStateService.getMaxRetries(), 1, 5));
    @Getter
    private final JCheckBox astMode = new JCheckBox("", llmConfigStateService.getAstMode());
    @Getter
    private final JCheckBox astParentClassCheckBox = new JCheckBox("", llmConfigStateService.getAstParentClass());
    @Getter
    private final JCheckBox astReferenceClassesCheckBox = new JCheckBox("", llmConfigStateService.getAstClassReference());
    @Getter
    private final JCheckBox astReferenceFieldCheckBox = new JCheckBox("", llmConfigStateService.getAstFieldReference());


    public LLMConfigSettingsComponent() {

        panel = FormBuilder.createFormBuilder()
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

        astMode.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            astParentClassCheckBox.setEnabled(selected);
            astReferenceClassesCheckBox.setEnabled(selected);
            astReferenceFieldCheckBox.setEnabled(selected);
        });
    }
}
