package com.devoxx.genie.ui.settings;

<<<<<<< HEAD
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.components.JBLabel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

=======
>>>>>>> master
import javax.swing.*;
import java.awt.*;

public class AbstractSettingsComponent implements SettingsComponent {

    protected final JPanel panel = new JPanel(new BorderLayout());

<<<<<<< HEAD
    protected static final String LINK_EMOJI = "\uD83D\uDD17";
    protected static final String PASSWORD_EMOJI = "\uD83D\uDD11";

    protected final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

=======
>>>>>>> master
    @Override
    public JPanel createPanel() {
        return panel;
    }

    @Override
    public void addListeners() {
    }
<<<<<<< HEAD

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

    protected @NotNull JComponent createTextWithPasswordButton(JComponent jComponent, String url) {
        return createTextWithLinkButton(jComponent, url);
    }

    protected @NotNull JComponent createTextWithLinkButton(JComponent jComponent,
                                                         String url) {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(jComponent, BorderLayout.CENTER);

        JButton btnApiKey = new JButton(AbstractSettingsComponent.PASSWORD_EMOJI);
        btnApiKey.setToolTipText("Get your API Key from " + " " + url);
        btnApiKey.addActionListener(e -> {
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
=======
>>>>>>> master
}
