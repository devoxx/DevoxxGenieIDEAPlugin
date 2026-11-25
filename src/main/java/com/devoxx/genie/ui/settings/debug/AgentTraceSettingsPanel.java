package com.devoxx.genie.ui.settings.debug;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.devoxx.genie.service.trace.TruffleHogMode;
import com.devoxx.genie.service.trace.hf.HfDatasetTarget;
import com.devoxx.genie.service.trace.hf.HfTokenResolver;
import com.devoxx.genie.service.trace.hf.HfUploadSettings;
import com.devoxx.genie.service.trace.TruffleHogOnFindings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Settings for agent trace recording (task-279): the opt-in switch, redaction options and the
 * TruffleHog secret scan. Every option below the switch is disabled while recording is off.
 */
public class AgentTraceSettingsPanel {

    private final JPanel panel = new JPanel();
    private final JBCheckBox enabledCheckBox = new JBCheckBox("Record agent traces");
    private final JBIntSpinner maxToolResultCharsSpinner = new JBIntSpinner(50_000, 0, 10_000_000, 1_000);
    private final JBCheckBox redactHomeCheckBox = new JBCheckBox("Replace home directory with ~ and OS username with <user>");
    private final JBTextArea customPatternsArea = new JBTextArea(3, 40);
    private final JComboBox<TruffleHogMode> truffleHogModeCombo = new JComboBox<>(TruffleHogMode.values());
    private final JComboBox<TruffleHogOnFindings> onFindingsCombo = new JComboBox<>(TruffleHogOnFindings.values());
    private final JBTextField truffleHogPathField = new JBTextField();
    private final JBCheckBox verifyCheckBox = new JBCheckBox("Let TruffleHog verify secrets (sends candidate secrets to the issuing service)");
    // Hugging Face upload (task-280)
    private final JBTextField hfTargetField = new JBTextField();
    private final JBCheckBox hfPrivateCheckBox = new JBCheckBox("Create the dataset as private");
    private final JBTextField hfPathField = new JBTextField();
    private final JBCheckBox hfIncludeProjectCheckBox = new JBCheckBox("Include the project name in the trace header");
    private final JBCheckBox hfConfirmCheckBox = new JBCheckBox("Ask for confirmation before each upload");
    private final JBPasswordField hfTokenField = new JBPasswordField();
    private final JButton hfRemoveTokenButton = new JButton("Remove saved token");
    private final JBLabel hfTokenStatus = new JBLabel();
    /** Set when "Remove saved token" was clicked; applied on Apply. */
    private boolean hfRemoveTokenPending;
    private final List<JComponent> dependents;

    public AgentTraceSettingsPanel() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        truffleHogModeCombo.setRenderer(enumRenderer());
        onFindingsCombo.setRenderer(enumRenderer());
        truffleHogPathField.getEmptyText().setText("trufflehog (looked up on PATH)");
        customPatternsArea.getEmptyText().setText("One regular expression per line");

        add(enabledCheckBox);
        add(Box.createVerticalStrut(6));
        add(help("Off by default. When enabled, every agent turn is saved to the conversation history " +
                "database: your prompt, each assistant step with its reasoning and tool calls, and every " +
                "tool result. <b>Traces can contain prompts, file contents, command output and secrets.</b> " +
                "Likely secrets are redacted before anything is written, and TruffleHog (when installed) " +
                "scans each turn, but redaction is best-effort. Traces stay on this machine."));
        add(Box.createVerticalStrut(10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = JBUI.insets(2, 0, 2, 8);
        int row = 0;
        row = formRow(form, c, row, "Max tool result characters (0 = no limit):", maxToolResultCharsSpinner);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(redactHomeCheckBox, c);
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JBLabel("Custom redaction patterns:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(new JBScrollPane(customPatternsArea), c);
        c.fill = GridBagConstraints.NONE; c.weightx = 0; c.anchor = GridBagConstraints.WEST;
        row++;
        row = formRow(form, c, row, "TruffleHog scan:", truffleHogModeCombo);
        row = formRow(form, c, row, "When TruffleHog finds a secret:", onFindingsCombo);
        c.fill = GridBagConstraints.HORIZONTAL;
        row = formRow(form, c, row, "TruffleHog binary:", truffleHogPathField);
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(verifyCheckBox, c);
        add(form);
        add(Box.createVerticalStrut(6));
        add(help("TruffleHog: <b>Auto</b> scans when the binary is found; <b>Required</b> keeps a turn only " +
                "after a successful scan; <b>Off</b> relies on regex redaction only. Install it from " +
                "github.com/trufflesecurity/trufflehog (e.g. <code>brew install trufflehog</code>)."));

        add(Box.createVerticalStrut(16));
        JBLabel hfHeader = new JBLabel("Hugging Face upload");
        hfHeader.setFont(hfHeader.getFont().deriveFont(Font.BOLD));
        add(hfHeader);
        add(Box.createVerticalStrut(6));
        add(help("The chat window shows an <b>Upload to Hugging Face</b> button while trace recording is on. "
                + "It converts the conversation to the Hub's Session Trace format and uploads it to the "
                + "dataset below, where it opens in the agent trace viewer. Uploading again overwrites the "
                + "conversation's file."));
        add(Box.createVerticalStrut(6));
        hfTargetField.getEmptyText().setText("user/devoxxgenie-traces (asked on first upload)");
        hfPathField.getEmptyText().setText(HfUploadSettings.DEFAULT_PATH_IN_REPO);
        hfTokenField.getEmptyText().setText("hf_… (leave empty to keep the current token)");
        JPanel hfForm = new JPanel(new GridBagLayout());
        hfForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints h = new GridBagConstraints();
        h.anchor = GridBagConstraints.WEST;
        h.insets = JBUI.insets(2, 0, 2, 8);
        h.fill = GridBagConstraints.HORIZONTAL;
        int hRow = 0;
        hRow = formRow(hfForm, h, hRow, "Dataset:", hfTargetField);
        hRow = formRow(hfForm, h, hRow, "Folder in dataset:", hfPathField);
        hRow = formRow(hfForm, h, hRow, "Access token (write):", hfTokenField);
        h.fill = GridBagConstraints.NONE;
        JPanel tokenRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tokenRow.add(hfTokenStatus);
        tokenRow.add(Box.createHorizontalStrut(8));
        tokenRow.add(hfRemoveTokenButton);
        h.gridx = 1; h.gridy = hRow++;
        hfForm.add(tokenRow, h);
        for (JBCheckBox box : List.of(hfPrivateCheckBox, hfIncludeProjectCheckBox, hfConfirmCheckBox)) {
            h.gridx = 0; h.gridy = hRow++; h.gridwidth = 2;
            hfForm.add(box, h);
        }
        add(hfForm);
        hfRemoveTokenButton.addActionListener(e -> {
            hfRemoveTokenPending = true;
            hfTokenField.setText("");
            updateTokenStatus();
        });

        dependents = List.of(maxToolResultCharsSpinner, redactHomeCheckBox, customPatternsArea,
                truffleHogModeCombo, onFindingsCombo, truffleHogPathField, verifyCheckBox,
                hfTargetField, hfPathField, hfTokenField, hfRemoveTokenButton, hfPrivateCheckBox,
                hfIncludeProjectCheckBox, hfConfirmCheckBox);
        enabledCheckBox.addActionListener(e -> updateEnabledState());
        truffleHogModeCombo.addActionListener(e -> updateEnabledState());
        reset();
    }

    private static int formRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        form.add(new JBLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
        return row + 1;
    }

    private static @NotNull ListCellRenderer<Object> enumRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String text = value instanceof Enum<?> e ? displayName(e) : String.valueOf(value);
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        };
    }

    private static String displayName(Enum<?> e) {
        if (e == TruffleHogMode.AUTO) return "Auto (when installed)";
        if (e == TruffleHogMode.REQUIRED) return "Required";
        if (e == TruffleHogMode.OFF) return "Off";
        if (e == TruffleHogOnFindings.REDACT) return "Redact the secret";
        if (e == TruffleHogOnFindings.SKIP) return "Drop the turn's trace";
        return e.name();
    }

    private void add(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(component);
    }

    private void add(Component component) {
        panel.add(component);
    }

    private static JBLabel help(String html) {
        JBLabel label = new JBLabel("<html><body style='width:480px'>" + html + "</body></html>");
        label.setForeground(UIUtil.getContextHelpForeground());
        return label;
    }

    private void updateEnabledState() {
        boolean on = enabledCheckBox.isSelected();
        dependents.forEach(d -> d.setEnabled(on));
        boolean scanning = on && truffleHogModeCombo.getSelectedItem() != TruffleHogMode.OFF;
        onFindingsCombo.setEnabled(scanning);
        truffleHogPathField.setEnabled(scanning);
        verifyCheckBox.setEnabled(scanning);
    }

    public @NotNull JPanel getPanel() {
        return panel;
    }

    /** Non-blank lines of the patterns field. */
    List<String> customPatterns() {
        return Arrays.stream(customPatternsArea.getText().split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void updateTokenStatus() {
        boolean stored = !hfRemoveTokenPending && !storedToken().isEmpty();
        if (stored) {
            hfTokenStatus.setText("A token is saved in the IDE credential store.");
        } else {
            HfTokenResolver.ResolvedToken other = hfRemoveTokenPending ? null : HfTokenResolver.system().resolve();
            hfTokenStatus.setText(other == null
                    ? (hfRemoveTokenPending ? "The saved token will be removed on Apply." : "No token saved; you'll be asked on first upload.")
                    : other.source() == HfTokenResolver.Source.ENVIRONMENT ? "Using the HF_TOKEN environment variable."
                    : "Using the token from 'hf auth login'.");
        }
        hfRemoveTokenButton.setVisible(!storedToken().isEmpty() && !hfRemoveTokenPending);
    }

    private static String storedToken() {
        try {
            return CredentialService.getInstance().getCredential(CredentialKey.HUGGING_FACE_TOKEN);
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Returns an error message for the first invalid custom pattern, or {@code null} when all compile. */
    public String validatePatterns() {
        String target = hfTargetField.getText().trim();
        if (!target.isEmpty() && HfDatasetTarget.parse(target) == null) {
            return "Invalid Hugging Face dataset \"" + target + "\": use the form user/name (storage buckets aren't supported yet).";
        }
        for (String p : customPatterns()) {
            try {
                Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                return "Invalid redaction pattern \"" + p + "\": " + e.getDescription();
            }
        }
        return null;
    }

    public boolean isModified() {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        return enabledCheckBox.isSelected() != Boolean.TRUE.equals(s.getAgentTraceRecordingEnabled())
                || maxToolResultCharsSpinner.getNumber() != Objects.requireNonNullElse(s.getAgentTraceMaxToolResultChars(), 50_000)
                || redactHomeCheckBox.isSelected() != !Boolean.FALSE.equals(s.getAgentTraceRedactHomeDirectory())
                || !customPatterns().equals(Objects.requireNonNullElse(s.getAgentTraceCustomRedactionPatterns(), List.of()))
                || truffleHogModeCombo.getSelectedItem() != TruffleHogMode.fromSetting(s.getAgentTraceTruffleHogMode())
                || onFindingsCombo.getSelectedItem() != TruffleHogOnFindings.fromSetting(s.getAgentTraceTruffleHogOnFindings())
                || !truffleHogPathField.getText().trim().equals(Objects.requireNonNullElse(s.getAgentTraceTruffleHogPath(), "").trim())
                || verifyCheckBox.isSelected() != Boolean.TRUE.equals(s.getAgentTraceTruffleHogVerify())
                || !hfTargetField.getText().trim().equals(Objects.requireNonNullElse(s.getAgentTraceHfTarget(), "").trim())
                || !hfPathField.getText().trim().equals(Objects.requireNonNullElse(s.getAgentTraceHfPathInRepo(), HfUploadSettings.DEFAULT_PATH_IN_REPO))
                || hfPrivateCheckBox.isSelected() != !Boolean.FALSE.equals(s.getAgentTraceHfPrivate())
                || hfIncludeProjectCheckBox.isSelected() != !Boolean.FALSE.equals(s.getAgentTraceHfIncludeProjectName())
                || hfConfirmCheckBox.isSelected() != !Boolean.FALSE.equals(s.getAgentTraceHfConfirmBeforeUpload())
                || hfTokenField.getPassword().length > 0
                || hfRemoveTokenPending;
    }

    public void apply() {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        s.setAgentTraceRecordingEnabled(enabledCheckBox.isSelected());
        s.setAgentTraceMaxToolResultChars(maxToolResultCharsSpinner.getNumber());
        s.setAgentTraceRedactHomeDirectory(redactHomeCheckBox.isSelected());
        s.setAgentTraceCustomRedactionPatterns(new java.util.ArrayList<>(customPatterns()));
        TruffleHogMode mode = (TruffleHogMode) truffleHogModeCombo.getSelectedItem();
        s.setAgentTraceTruffleHogMode((mode == null ? TruffleHogMode.AUTO : mode).settingValue());
        TruffleHogOnFindings onFindings = (TruffleHogOnFindings) onFindingsCombo.getSelectedItem();
        s.setAgentTraceTruffleHogOnFindings((onFindings == null ? TruffleHogOnFindings.REDACT : onFindings).settingValue());
        s.setAgentTraceTruffleHogPath(truffleHogPathField.getText().trim());
        s.setAgentTraceTruffleHogVerify(verifyCheckBox.isSelected());
        HfDatasetTarget target = HfDatasetTarget.parse(hfTargetField.getText());
        s.setAgentTraceHfTarget(target == null ? "" : target.repoId());
        s.setAgentTraceHfPathInRepo(hfPathField.getText().trim());
        s.setAgentTraceHfPrivate(hfPrivateCheckBox.isSelected());
        s.setAgentTraceHfIncludeProjectName(hfIncludeProjectCheckBox.isSelected());
        s.setAgentTraceHfConfirmBeforeUpload(hfConfirmCheckBox.isSelected());
        String token = new String(hfTokenField.getPassword()).trim();
        if (!token.isEmpty()) {
            CredentialService.getInstance().setCredential(CredentialKey.HUGGING_FACE_TOKEN, token);
        } else if (hfRemoveTokenPending) {
            CredentialService.getInstance().removeCredential(CredentialKey.HUGGING_FACE_TOKEN);
        }
        hfTokenField.setText("");
        hfRemoveTokenPending = false;
        updateTokenStatus();
    }

    public void reset() {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        enabledCheckBox.setSelected(Boolean.TRUE.equals(s.getAgentTraceRecordingEnabled()));
        maxToolResultCharsSpinner.setNumber(Objects.requireNonNullElse(s.getAgentTraceMaxToolResultChars(), 50_000));
        redactHomeCheckBox.setSelected(!Boolean.FALSE.equals(s.getAgentTraceRedactHomeDirectory()));
        customPatternsArea.setText(String.join("\n", Objects.requireNonNullElse(s.getAgentTraceCustomRedactionPatterns(), List.of())));
        truffleHogModeCombo.setSelectedItem(TruffleHogMode.fromSetting(s.getAgentTraceTruffleHogMode()));
        onFindingsCombo.setSelectedItem(TruffleHogOnFindings.fromSetting(s.getAgentTraceTruffleHogOnFindings()));
        truffleHogPathField.setText(Objects.requireNonNullElse(s.getAgentTraceTruffleHogPath(), ""));
        verifyCheckBox.setSelected(Boolean.TRUE.equals(s.getAgentTraceTruffleHogVerify()));
        hfTargetField.setText(Objects.requireNonNullElse(s.getAgentTraceHfTarget(), ""));
        hfPathField.setText(Objects.requireNonNullElse(s.getAgentTraceHfPathInRepo(), HfUploadSettings.DEFAULT_PATH_IN_REPO));
        hfPrivateCheckBox.setSelected(!Boolean.FALSE.equals(s.getAgentTraceHfPrivate()));
        hfIncludeProjectCheckBox.setSelected(!Boolean.FALSE.equals(s.getAgentTraceHfIncludeProjectName()));
        hfConfirmCheckBox.setSelected(!Boolean.FALSE.equals(s.getAgentTraceHfConfirmBeforeUpload()));
        hfTokenField.setText("");
        hfRemoveTokenPending = false;
        updateTokenStatus();
        updateEnabledState();
    }
}
