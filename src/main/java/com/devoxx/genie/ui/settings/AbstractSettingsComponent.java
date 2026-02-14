package com.devoxx.genie.ui.settings;

import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;

public class AbstractSettingsComponent implements SettingsComponent {

    protected final JPanel panel = new JPanel(new BorderLayout());

    protected static final String LINK_EMOJI = "\uD83D\uDD17";
    protected static final String PASSWORD_EMOJI = "\uD83D\uDD11";

    protected final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    @Override
    public JPanel createPanel() {
        return panel;
    }

    @Override
    public void addListeners() {
    }

    protected void addSection(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, String title) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JXTitledSeparator(title), gbc);
        gbc.gridy++;
    }

    protected void addSettingRow(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, String label, JComponent component) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
        gbc.gridy++;
    }

    protected void addSettingRow(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, String label) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        panel.add(new JBLabel(label), gbc);
        gbc.gridy++;
    }

    protected void addProviderSettingRow(JPanel panel, GridBagConstraints gbc, String label, JCheckBox checkbox) {
        JPanel providerPanel = new JPanel(new BorderLayout(5, 0));
        providerPanel.add(checkbox, BorderLayout.WEST);

        addSettingRow(panel, gbc, label, providerPanel);
    }
    
    protected void addProviderSettingRow(JPanel panel, GridBagConstraints gbc, String label, JCheckBox checkbox, JComponent urlComponent) {
        JPanel providerPanel = new JPanel(new BorderLayout(5, 0));
        providerPanel.add(checkbox, BorderLayout.WEST);
        providerPanel.add(urlComponent, BorderLayout.CENTER);

        addSettingRow(panel, gbc, label, providerPanel);
    }

    protected @NotNull JComponent createTextWithPasswordButton(JComponent jComponent, String url) {
        return createTextWithLinkButton(jComponent, url);
    }

    protected @NotNull JComponent createTextWithLinkButton(JComponent jComponent,
                                                         String url) {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(jComponent, BorderLayout.CENTER);
        JButton btnApiKey = createActionButton(
                AbstractSettingsComponent.PASSWORD_EMOJI,
                null, "Get your API Key from " + " " + url,
                e -> {
            try {
                BrowserUtil.open(url);
            } catch (Exception ex) {
                Project project = ProjectManager.getInstance().getOpenProjects()[0];
                NotificationUtil.sendNotification(project, "Error: Unable to open the link");
            }
        });

        jPanel.add(btnApiKey, BorderLayout.WEST);
        return jPanel;
    }

    /**
     * Wraps a settings content panel with a help button at the top-right corner
     * that links to the relevant documentation page.
     *
     * @param contentPanel the original settings panel
     * @param docUrl       the full URL to the documentation page
     * @return a wrapper panel containing the help button and the content panel
     */
    public static @NotNull JPanel wrapWithHelpButton(@NotNull JPanel contentPanel, @NotNull String docUrl) {
        JPanel wrapper = new JPanel(new BorderLayout());

        JButton helpButton = new JButton("Help", AllIcons.Actions.Help);
        helpButton.setToolTipText("Open documentation");
        helpButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        helpButton.addActionListener(e -> BrowserUtil.open(docUrl));

        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        helpPanel.setBorder(JBUI.Borders.emptyBottom(4));
        helpPanel.add(helpButton);

        wrapper.add(helpPanel, BorderLayout.NORTH);
        wrapper.add(contentPanel, BorderLayout.CENTER);

        return wrapper;
    }
}
