package com.devoxx.genie.ui.trace;

import com.devoxx.genie.service.trace.hf.HfDatasetTarget;
import com.devoxx.genie.service.trace.hf.HfUploadSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * First-use setup for uploading agent traces to Hugging Face (task-280): asks for a write
 * token (only when none is found) and the target dataset. A blank dataset means
 * {@code <your-hf-user>/devoxxgenie-traces}, resolved with {@code whoami} during the upload.
 */
final class HfUploadSetupDialog extends DialogWrapper {

    private final boolean needsToken;
    private final JBPasswordField tokenField = new JBPasswordField();
    private final JBTextField datasetField = new JBTextField();
    private final JBCheckBox privateCheckBox = new JBCheckBox("Create the dataset as private", true);

    HfUploadSetupDialog(@Nullable Project project, boolean needsToken, @NotNull HfUploadSettings settings) {
        super(project, true);
        this.needsToken = needsToken;
        setTitle("Upload Agent Traces to Hugging Face");
        setOKButtonText("Continue");
        datasetField.getEmptyText().setText("<your-hf-user>/" + HfUploadSettings.DEFAULT_DATASET_NAME);
        if (settings.target() != null) {
            datasetField.setText(settings.target().repoId());
        }
        privateCheckBox.setSelected(settings.isPrivate());
        tokenField.getEmptyText().setText("hf_…");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        FormBuilder form = FormBuilder.createFormBuilder();
        form.addComponent(help("Traces are uploaded to a Hugging Face dataset, where they open in the Hub's agent "
                + "trace viewer. They are redacted before upload, but can still contain code and prompts: keep the "
                + "dataset private unless you have reviewed them."));
        if (needsToken) {
            form.addLabeledComponent("Access token:", tokenField);
            form.addComponentToRightColumn(help("Create one with <b>write</b> access at huggingface.co/settings/tokens. "
                    + "It is stored in the IDE's credential store."));
        }
        form.addLabeledComponent("Dataset:", datasetField);
        form.addComponentToRightColumn(help("Leave empty to use <code>&lt;your-hf-user&gt;/"
                + HfUploadSettings.DEFAULT_DATASET_NAME + "</code>. It is created if it doesn't exist."));
        form.addComponent(privateCheckBox);
        JPanel panel = form.getPanel();
        panel.setPreferredSize(new java.awt.Dimension(520, panel.getPreferredSize().height));
        return panel;
    }

    private static JBLabel help(String html) {
        JBLabel label = new JBLabel("<html><body style='width:420px'>" + html + "</body></html>");
        label.setForeground(UIUtil.getContextHelpForeground());
        return label;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return needsToken ? tokenField : datasetField;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (needsToken && token() == null) {
            return new ValidationInfo("A Hugging Face token with write access is required", tokenField);
        }
        String dataset = datasetField.getText().trim();
        if (!dataset.isEmpty() && HfDatasetTarget.parse(dataset) == null) {
            return new ValidationInfo("Use the form user/name", datasetField);
        }
        return null;
    }

    /** The entered token, or {@code null} when none was asked for or entered. */
    @Nullable String token() {
        String t = new String(tokenField.getPassword()).trim();
        return t.isEmpty() ? null : t;
    }

    /** The chosen dataset, or {@code null} for the default {@code <user>/devoxxgenie-traces}. */
    @Nullable HfDatasetTarget target() {
        return HfDatasetTarget.parse(datasetField.getText());
    }

    boolean isPrivateDataset() {
        return privateCheckBox.isSelected();
    }
}
