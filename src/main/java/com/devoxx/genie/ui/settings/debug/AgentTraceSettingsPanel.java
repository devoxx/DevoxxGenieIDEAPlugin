package com.devoxx.genie.ui.settings.debug;

import com.devoxx.genie.service.trace.TruffleHogMode;
import com.devoxx.genie.service.trace.TruffleHogOnFindings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
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

        dependents = List.of(maxToolResultCharsSpinner, redactHomeCheckBox, customPatternsArea,
                truffleHogModeCombo, onFindingsCombo, truffleHogPathField, verifyCheckBox);
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

    /** Returns an error message for the first invalid custom pattern, or {@code null} when all compile. */
    public String validatePatterns() {
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
                || verifyCheckBox.isSelected() != Boolean.TRUE.equals(s.getAgentTraceTruffleHogVerify());
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
        updateEnabledState();
    }
}
