package com.devoxx.genie.ui.settings.ap;

import com.devoxx.genie.model.ap.ApAuthMode;
import com.devoxx.genie.service.ap.ApCliService;
import com.devoxx.genie.ui.panel.ap.ApPreviewRibbon;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Settings panel for the Docker Agentic Platform ({@code ap}) CLI integration.
 *
 * <p>Captures the binary location, the auth mode (Docker Desktop login vs. manual tokens),
 * and the manual token pair when applicable. A "Test connection" button runs
 * {@code ap version} + {@code ap agent ls --limit 1} with the configured env and reports
 * the result inline.</p>
 */
public class ApSettingsComponent extends AbstractSettingsComponent {

    private final JBCheckBox enabledCheckBox =
            new JBCheckBox("Enable Docker Agentic Platform tab in the DevoxxGenie tool window");
    private final TextFieldWithBrowseButton binaryPathField = createBinaryBrowseField();
    private final JRadioButton cachedLoginRadio = new JRadioButton("Cached login (recommended — run `ap` once to authenticate)");
    private final JRadioButton dockerDesktopRadio = new JRadioButton("Docker Desktop login");
    private final JRadioButton manualTokensRadio = new JRadioButton("Manual tokens");
    private final JBPasswordField accessTokenField = new JBPasswordField();
    private final JBPasswordField refreshTokenField = new JBPasswordField();
    private final JBLabel statusLabel = new JBLabel(" ");
    private final Color defaultStatusColor = statusLabel.getForeground();
    private final JButton testButton = new JButton("Test connection");

    public ApSettingsComponent() {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        addSection(content, gbc, "Docker Agentic Platform CLI");
        addHelpText(content, gbc,
                "Configure the path to the `ap` CLI binary. List agents, projects, and past sessions, " +
                "or start a new run from inside DevoxxGenie. The binary is shipped by Docker as part of " +
                "the Agentic Platform; download or copy it to a location on disk.");

        addFullWidthRow(content, gbc, enabledCheckBox);
        addSettingRow(content, gbc, "ap CLI binary:", binaryPathField);

        addSection(content, gbc, "Authentication");
        addHelpText(content, gbc,
                "Cached login: the plugin sets no env vars and the `ap` binary uses whichever " +
                "credentials it finds on its own (the tokens cached by a prior TUI login, or " +
                "Docker Desktop). If `ap agent ls` works in your terminal, this mode will work too. " +
                "Docker Desktop: forces AP_AGENTIC_PLATFORM_AUTH_MODE=docker-desktop. " +
                "Manual tokens: provide AP_AGENTIC_PLATFORM_ACCESS_TOKEN / REFRESH_TOKEN explicitly.");

        ButtonGroup group = new ButtonGroup();
        group.add(cachedLoginRadio);
        group.add(dockerDesktopRadio);
        group.add(manualTokensRadio);
        addFullWidthRow(content, gbc, cachedLoginRadio);
        addFullWidthRow(content, gbc, dockerDesktopRadio);
        addFullWidthRow(content, gbc, manualTokensRadio);

        addSettingRow(content, gbc, "Access token:", accessTokenField);
        addSettingRow(content, gbc, "Refresh token:", refreshTokenField);

        JPanel testRow = new JPanel(new BorderLayout(8, 0));
        testRow.add(testButton, BorderLayout.WEST);
        testRow.add(statusLabel, BorderLayout.CENTER);
        addFullWidthRow(content, gbc, testRow);

        gbc.weighty = 1.0;
        gbc.gridy++;
        content.add(Box.createVerticalGlue(), gbc);

        panel.add(new ApPreviewRibbon(), BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        reset();
        addListeners();
    }

    @Override
    public void addListeners() {
        cachedLoginRadio.addActionListener(e -> updateTokenFieldsEnabled());
        dockerDesktopRadio.addActionListener(e -> updateTokenFieldsEnabled());
        manualTokensRadio.addActionListener(e -> updateTokenFieldsEnabled());
        testButton.addActionListener(e -> runTestConnection());
    }

    private void updateTokenFieldsEnabled() {
        boolean manual = manualTokensRadio.isSelected();
        accessTokenField.setEnabled(manual);
        refreshTokenField.setEnabled(manual);
    }

    private void runTestConnection() {
        // Apply current UI values so the test reflects what the user just typed,
        // even if they haven't clicked "Apply" yet.
        applyToState();
        statusLabel.setText("Testing…");
        statusLabel.setForeground(defaultStatusColor);
        testButton.setEnabled(false);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApCliService.TestResult result = ApCliService.getInstance().testConnection();
            // ModalityState.any() is required because the Settings dialog is modal —
            // the default NON_MODAL modality leaves the callback queued until the dialog closes.
            ApplicationManager.getApplication().invokeLater(() -> {
                statusLabel.setText(result.message());
                statusLabel.setForeground(result.ok() ? new Color(0x4CAF50) : new Color(0xE57373));
                testButton.setEnabled(true);
            }, ModalityState.any());
        });
    }

    // ===== State Management =====

    public boolean isModified() {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        return enabledCheckBox.isSelected() != Boolean.TRUE.equals(s.getApIntegrationEnabled())
                || !nullSafeEq(binaryPathField.getText(), s.getApCliPath())
                || !nullSafeEq(selectedAuthMode().name(), nullToDefault(s.getApAuthMode()))
                || !nullSafeEq(new String(accessTokenField.getPassword()), s.getApAccessToken())
                || !nullSafeEq(new String(refreshTokenField.getPassword()), s.getApRefreshToken());
    }

    public void apply() {
        applyToState();
    }

    public void reset() {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        enabledCheckBox.setSelected(Boolean.TRUE.equals(s.getApIntegrationEnabled()));
        binaryPathField.setText(nullToEmpty(s.getApCliPath()));
        ApAuthMode mode = ApAuthMode.fromName(s.getApAuthMode());
        cachedLoginRadio.setSelected(mode == ApAuthMode.CACHED_LOGIN);
        dockerDesktopRadio.setSelected(mode == ApAuthMode.DOCKER_DESKTOP);
        manualTokensRadio.setSelected(mode == ApAuthMode.MANUAL_TOKENS);
        accessTokenField.setText(nullToEmpty(s.getApAccessToken()));
        refreshTokenField.setText(nullToEmpty(s.getApRefreshToken()));
        updateTokenFieldsEnabled();
        statusLabel.setText(" ");
    }

    private void applyToState() {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        s.setApIntegrationEnabled(enabledCheckBox.isSelected());
        s.setApCliPath(binaryPathField.getText().trim());
        s.setApAuthMode(selectedAuthMode().name());
        s.setApAccessToken(new String(accessTokenField.getPassword()));
        s.setApRefreshToken(new String(refreshTokenField.getPassword()));
    }

    @Override
    protected String getHelpUrl() {
        return "https://docs.docker.com/desktop/features/gordon/agentic-platform/";
    }

    // ===== Helpers =====

    private @NotNull ApAuthMode selectedAuthMode() {
        if (manualTokensRadio.isSelected()) return ApAuthMode.MANUAL_TOKENS;
        if (dockerDesktopRadio.isSelected()) return ApAuthMode.DOCKER_DESKTOP;
        return ApAuthMode.CACHED_LOGIN;
    }

    private static @NotNull TextFieldWithBrowseButton createBinaryBrowseField() {
        TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true, false, false, false, false, false)
                .withTitle("Select ap CLI binary")
                .withDescription("Path to the Docker Agentic Platform `ap` executable");
        field.addBrowseFolderListener(null, descriptor);
        return field;
    }

    private void addHelpText(@NotNull JPanel parent, @NotNull GridBagConstraints gbc, @NotNull String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));
        area.setFont(area.getFont().deriveFont(Font.ITALIC));
        addFullWidthRow(parent, gbc, area);
    }

    private void addFullWidthRow(@NotNull JPanel parent, @NotNull GridBagConstraints gbc, @NotNull JComponent comp) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        parent.add(comp, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;
    }

    private static boolean nullSafeEq(String a, String b) {
        return java.util.Objects.equals(nullToEmpty(a), nullToEmpty(b));
    }

    private static @NotNull String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static @NotNull String nullToDefault(String s) {
        return (s == null || s.isBlank()) ? ApAuthMode.CACHED_LOGIN.name() : s;
    }
}
