package com.devoxx.genie.ui.settings.security;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

/**
 * Settings panel for Security Scanning features (gitleaks, opengrep, trivy).
 */
@Slf4j
public class SecurityScanSettingsComponent extends AbstractSettingsComponent {

    private final JBCheckBox enableSecurityScanCheckbox =
            new JBCheckBox("Enable Security Scanning", Boolean.TRUE.equals(stateService.getSecurityScanEnabled()));

    private final JBCheckBox createSpecTasksCheckbox =
            new JBCheckBox("Create Spec Tasks from findings",
                    !Boolean.FALSE.equals(stateService.getSecurityScanCreateSpecTasks()));

    private final TextFieldWithBrowseButton gitleaksPathField = createBrowseField(
            "Select Gitleaks Binary",
            "Select the gitleaks executable",
            stateService.getGitleaksPath());

    private final TextFieldWithBrowseButton opengrepPathField = createBrowseField(
            "Select OpenGrep Binary",
            "Select the opengrep executable",
            stateService.getOpengrepPath());

    private final TextFieldWithBrowseButton trivyPathField = createBrowseField(
            "Select Trivy Binary",
            "Select the trivy executable",
            stateService.getTrivyPath());

    private final JBLabel gitleaksStatusLabel = new JBLabel();
    private final JBLabel opengrepStatusLabel = new JBLabel();
    private final JBLabel trivyStatusLabel = new JBLabel();

    private final JBCheckBox gitleaksScanToolCheckbox =
            new JBCheckBox("run_gitleaks_scan  — detect hardcoded secrets",
                    Boolean.TRUE.equals(stateService.getGitleaksScanToolEnabled()));

    private final JBCheckBox opengrepScanToolCheckbox =
            new JBCheckBox("run_opengrep_scan  — SAST code security analysis",
                    Boolean.TRUE.equals(stateService.getOpengrepScanToolEnabled()));

    private final JBCheckBox trivyScanToolCheckbox =
            new JBCheckBox("run_trivy_scan  — SCA dependency vulnerability scan",
                    Boolean.TRUE.equals(stateService.getTrivyScanToolEnabled()));

    public SecurityScanSettingsComponent() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy = 0;

        // --- Security Scanning ---
        addSection(contentPanel, gbc, "Security Scanning");

        addFullWidthRow(contentPanel, gbc, enableSecurityScanCheckbox);
        addHelpText(contentPanel, gbc,
                "Enable security scanning tools as LLM agent tools. When the agent runs a scan, " +
                "findings are automatically reported to the agent. " +
                "Each scanner must be installed on your system (see below).");

        addFullWidthRow(contentPanel, gbc, createSpecTasksCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, each security finding is automatically created as a task in the " +
                "Spec Browser (Backlog.md) with severity-based priority. " +
                "Duplicate findings are skipped if a task with the same title already exists.");

        // --- Gitleaks ---
        addSeparator(contentPanel, gbc);
        addSection(contentPanel, gbc, "Gitleaks — Secret Detection");
        addHelpText(contentPanel, gbc,
                "Detects hardcoded secrets, API keys, passwords and tokens in source code. " +
                "Install via: brew install gitleaks  |  or download from GitHub.");
        addFullWidthRow(contentPanel, gbc, buildDownloadRow(
                "https://github.com/gitleaks/gitleaks/releases",
                "https://github.com/gitleaks/gitleaks#readme"));
        addFullWidthRow(contentPanel, gbc,
                buildScannerRow("Gitleaks path:", gitleaksPathField, gitleaksStatusLabel, "gitleaks", "version"));

        // --- OpenGrep ---
        addSeparator(contentPanel, gbc);
        addSection(contentPanel, gbc, "OpenGrep — SAST Analysis");
        addHelpText(contentPanel, gbc,
                "Static Application Security Testing (SAST): detects security issues in source code " +
                "using a large library of rules. Install via: brew install opengrep  |  or download from GitHub.");
        addFullWidthRow(contentPanel, gbc, buildDownloadRow(
                "https://github.com/opengrep/opengrep/releases",
                "https://github.com/opengrep/opengrep#readme"));
        addFullWidthRow(contentPanel, gbc,
                buildScannerRow("OpenGrep path:", opengrepPathField, opengrepStatusLabel, "opengrep", "--version"));

        // --- Trivy ---
        addSeparator(contentPanel, gbc);
        addSection(contentPanel, gbc, "Trivy — Dependency Vulnerability Scan (SCA)");
        addHelpText(contentPanel, gbc,
                "Scans project dependencies for known CVEs. Must be installed manually — " +
                "specify the binary path below or ensure trivy is on your system PATH.");
        addFullWidthRow(contentPanel, gbc, buildDownloadRow(
                "https://github.com/aquasecurity/trivy/releases",
                "https://aquasecurity.github.io/trivy/"));
        addHelpText(contentPanel, gbc,
                "Install via: brew install trivy   (macOS/Linux)  |  " +
                "choco install trivy   (Windows)  |  or download from the releases page above.");
        addFullWidthRow(contentPanel, gbc,
                buildScannerRow("Trivy path:", trivyPathField, trivyStatusLabel, "trivy", "--version"));
        addHelpText(contentPanel, gbc,
                "Leave empty to use trivy from the system PATH.");

        // --- Agent Tools ---
        addSeparator(contentPanel, gbc);
        addSection(contentPanel, gbc, "Security Agent Tools");
        addHelpText(contentPanel, gbc,
                "Select which security scanners the LLM agent can invoke individually. " +
                "Requires 'Enable Security Scanning' to be checked above.");
        addFullWidthRow(contentPanel, gbc, gitleaksScanToolCheckbox);
        addFullWidthRow(contentPanel, gbc, opengrepScanToolCheckbox);
        addFullWidthRow(contentPanel, gbc, trivyScanToolCheckbox);

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
    }

    /** Creates a TextFieldWithBrowseButton configured to select an executable file. */
    private static TextFieldWithBrowseButton createBrowseField(String title, String description, String initialValue) {
        TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
        field.setText(initialValue != null ? initialValue : "");
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true, false, false, false, false, false)
                .withTitle(title)
                .withDescription(description);
        field.addBrowseFolderListener(null, descriptor);
        return field;
    }

    /** Builds a scanner row: label + TextFieldWithBrowseButton + Test button + status label. */
    private JPanel buildScannerRow(String label, TextFieldWithBrowseButton pathField,
                                   JBLabel statusLabel, String fallbackBinary,
                                   String versionArg) {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        row.add(new JBLabel(label), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        row.add(pathField, c);

        JButton testBtn = new JButton("Test");
        testBtn.addActionListener(e -> testBinary(pathField, statusLabel, fallbackBinary, versionArg));
        statusLabel.setFont(statusLabel.getFont().deriveFont((float) statusLabel.getFont().getSize() - 1));

        c.gridx = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.insets = new Insets(0, 6, 0, 4);
        row.add(testBtn, c);

        c.gridx = 3;
        c.insets = new Insets(0, 0, 0, 0);
        row.add(statusLabel, c);

        return row;
    }

    /** Builds a row with Download + Documentation hyperlinks. */
    private JPanel buildDownloadRow(String downloadUrl, String docsUrl) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

        HyperlinkLabel download = new HyperlinkLabel("Download");
        download.addHyperlinkListener(e -> BrowserUtil.browse(downloadUrl));

        HyperlinkLabel docs = new HyperlinkLabel("Documentation");
        docs.addHyperlinkListener(e -> BrowserUtil.browse(docsUrl));

        row.add(download);
        row.add(new JLabel("|"));
        row.add(docs);
        return row;
    }

    private void testBinary(TextFieldWithBrowseButton pathField, JBLabel statusLabel,
                            String fallbackBinary, String versionArg) {
        statusLabel.setText("Testing…");
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String binaryPath = pathField.getText().trim();
            if (binaryPath.isEmpty()) {
                binaryPath = fallbackBinary; // try system PATH
            }
            String result = runVersion(binaryPath, versionArg);
            final boolean ok = !result.startsWith("✗");
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(result);
                statusLabel.setForeground(ok
                        ? new Color(0, 150, 0)
                        : UIManager.getColor("Label.errorForeground") != null
                                ? UIManager.getColor("Label.errorForeground")
                                : Color.RED);
            });
        });
    }

    private String runVersion(String binary, String versionArg) {
        try {
            ProcessBuilder pb = new ProcessBuilder(List.of(binary, versionArg));
            pb.redirectErrorStream(true);
            // Augment PATH so co-located dependencies (e.g. pyopengrep) can be found.
            // IntelliJ launched from Dock/Finder gets a minimal PATH that often misses
            // /usr/local/bin, /opt/homebrew/bin, ~/.local/bin, etc.
            java.io.File binaryFile = new java.io.File(binary);
            java.util.Map<String, String> env = pb.environment();
            String home = System.getProperty("user.home", "");
            String existingPath = env.getOrDefault("PATH", "");
            String augmented = (binaryFile.getParent() != null ? binaryFile.getParent() + java.io.File.pathSeparator : "")
                    + home + "/.local/bin" + java.io.File.pathSeparator
                    + "/usr/local/bin" + java.io.File.pathSeparator
                    + "/opt/homebrew/bin" + java.io.File.pathSeparator
                    + "/opt/homebrew/sbin" + java.io.File.pathSeparator
                    + existingPath;
            env.put("PATH", augmented);
            if (binaryFile.getParentFile() != null && binaryFile.getParentFile().isDirectory()) {
                pb.directory(binaryFile.getParentFile());
            }
            Process process = pb.start();
            String firstLine;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                firstLine = reader.readLine();
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return "✓ " + (firstLine != null ? firstLine.trim() : "OK");
            }
            return "✗ " + (firstLine != null ? firstLine.trim() : "Exit code " + exitCode);
        } catch (Exception e) {
            log.debug("Binary test failed for {}: {}", binary, e.getMessage());
            return "✗ Not found — install or set path above";
        }
    }

    private void addSeparator(JPanel p, GridBagConstraints gbc) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 0, 8, 0);
        p.add(new JSeparator(), gbc);
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy++;
    }

    private void addFullWidthRow(JPanel p, GridBagConstraints gbc, JComponent component) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        p.add(component, gbc);
        gbc.gridy++;
    }

    private void addHelpText(JPanel p, GridBagConstraints gbc, String text) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 25, 8, 5);
        JTextArea helpArea = new JTextArea(text);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setEditable(false);
        helpArea.setFocusable(false);
        helpArea.setOpaque(false);
        helpArea.setBorder(null);
        helpArea.setFont(UIManager.getFont("Label.font").deriveFont((float) UIManager.getFont("Label.font").getSize() - 1));
        helpArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        p.add(helpArea, gbc);
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy++;
    }

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return enableSecurityScanCheckbox.isSelected() != Boolean.TRUE.equals(state.getSecurityScanEnabled())
                || createSpecTasksCheckbox.isSelected() != !Boolean.FALSE.equals(state.getSecurityScanCreateSpecTasks())
                || !Objects.equals(gitleaksPathField.getText().trim(), state.getGitleaksPath() != null ? state.getGitleaksPath() : "")
                || !Objects.equals(opengrepPathField.getText().trim(), state.getOpengrepPath() != null ? state.getOpengrepPath() : "")
                || !Objects.equals(trivyPathField.getText().trim(), state.getTrivyPath() != null ? state.getTrivyPath() : "")
                || gitleaksScanToolCheckbox.isSelected() != Boolean.TRUE.equals(state.getGitleaksScanToolEnabled())
                || opengrepScanToolCheckbox.isSelected() != Boolean.TRUE.equals(state.getOpengrepScanToolEnabled())
                || trivyScanToolCheckbox.isSelected() != Boolean.TRUE.equals(state.getTrivyScanToolEnabled());
    }

    public void apply() {
        stateService.setSecurityScanEnabled(enableSecurityScanCheckbox.isSelected());
        stateService.setSecurityScanCreateSpecTasks(createSpecTasksCheckbox.isSelected());
        stateService.setGitleaksPath(gitleaksPathField.getText().trim());
        stateService.setOpengrepPath(opengrepPathField.getText().trim());
        stateService.setTrivyPath(trivyPathField.getText().trim());
        stateService.setGitleaksScanToolEnabled(gitleaksScanToolCheckbox.isSelected());
        stateService.setOpengrepScanToolEnabled(opengrepScanToolCheckbox.isSelected());
        stateService.setTrivyScanToolEnabled(trivyScanToolCheckbox.isSelected());
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        enableSecurityScanCheckbox.setSelected(Boolean.TRUE.equals(state.getSecurityScanEnabled()));
        createSpecTasksCheckbox.setSelected(!Boolean.FALSE.equals(state.getSecurityScanCreateSpecTasks()));
        gitleaksPathField.setText(state.getGitleaksPath() != null ? state.getGitleaksPath() : "");
        opengrepPathField.setText(state.getOpengrepPath() != null ? state.getOpengrepPath() : "");
        trivyPathField.setText(state.getTrivyPath() != null ? state.getTrivyPath() : "");
        gitleaksScanToolCheckbox.setSelected(Boolean.TRUE.equals(state.getGitleaksScanToolEnabled()));
        opengrepScanToolCheckbox.setSelected(Boolean.TRUE.equals(state.getOpengrepScanToolEnabled()));
        trivyScanToolCheckbox.setSelected(Boolean.TRUE.equals(state.getTrivyScanToolEnabled()));
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/security-scanning";
    }
}
