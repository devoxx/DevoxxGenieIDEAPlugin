package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

@Getter
public class WebSearchProvidersComponent extends AbstractSettingsComponent {

    @Getter
    private final JCheckBox enableWebSearchCheckbox =
            new JCheckBox("", stateService.getIsWebSearchEnabled());

    @Getter
    private final JCheckBox tavilySearchEnabledCheckBox = new JCheckBox("", stateService.isTavilySearchEnabled());

    private final JPasswordField tavilySearchApiKeyField =
            new JPasswordField(stateService.getTavilySearchKey());

    @Getter
    private final JCheckBox googleSearchEnabledCheckBox = new JCheckBox("", stateService.isGoogleSearchEnabled());
    private final JPasswordField googleSearchApiKeyField =
            new JPasswordField(stateService.getGoogleSearchKey());

    private final JPasswordField googleCSIApiKeyField =
            new JPasswordField(stateService.getGoogleCSIKey());

    private final JBIntSpinner maxSearchResults =
            new JBIntSpinner(new UINumericRange(stateService.getMaxSearchResults(), 1, 10));

    public WebSearchProvidersComponent() {
        addListeners();
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/web-search";
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

        addSection(panel, gbc, "Web Search Providers");

        // Add description
        gbc.gridy++;
        JBLabel infoLabel = new JBLabel();
        infoLabel.setText(
                "<html><body style='width: 100%;'>" +
                        "Post your prompt on the web using either Google search or Tavily search." +
                        "</body></html>");

        infoLabel.setForeground(UIUtil.getContextHelpForeground());
        infoLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(infoLabel, gbc);

        gbc.gridy++;
        addSettingRow(panel, gbc, "Enable feature", enableWebSearchCheckbox);

        addProviderSettingRow(panel, gbc, "Tavily Web Search API Key", tavilySearchEnabledCheckBox,
                createTextWithPasswordButton(tavilySearchApiKeyField, "https://app.tavily.com/home"));

        addProviderSettingRow(panel, gbc, "Google Web Search API Key", googleSearchEnabledCheckBox,
                createTextWithPasswordButton(googleSearchApiKeyField, "https://developers.google.com/custom-search/docs/paid_element#api_key"));

        addSettingRow(panel, gbc, "Google Custom Search Engine ID",
                createTextWithPasswordButton(googleCSIApiKeyField, "https://programmablesearchengine.google.com/controlpanel/create"));

        addSettingRow(panel, gbc, "Max search results", maxSearchResults);

        return panel;
    }

    @Override
    public void addListeners() {
        enableWebSearchCheckbox.addItemListener(e -> {
            stateService.setIsWebSearchEnabled(e.getStateChange() == ItemEvent.SELECTED);
            // Disable both providers when the feature is disabled
            if (!enableWebSearchCheckbox.isSelected()) {
                tavilySearchEnabledCheckBox.setSelected(false);
                googleSearchEnabledCheckBox.setSelected(false);
                updateUrlFieldState(tavilySearchEnabledCheckBox, tavilySearchApiKeyField);
                updateUrlFieldState(googleSearchEnabledCheckBox, googleSearchApiKeyField);
            }
        });

        tavilySearchEnabledCheckBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Disable Google Search if Tavily is selected
                googleSearchEnabledCheckBox.setSelected(false);
                stateService.setGoogleSearchEnabled(false);
                updateUrlFieldState(googleSearchEnabledCheckBox, googleSearchApiKeyField);
            }
            stateService.setTavilySearchEnabled(tavilySearchEnabledCheckBox.isSelected());
            updateUrlFieldState(tavilySearchEnabledCheckBox, tavilySearchApiKeyField);
        });

        googleSearchEnabledCheckBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Disable Tavily if Google Search is selected
                tavilySearchEnabledCheckBox.setSelected(false);
                stateService.setTavilySearchEnabled(false);
                updateUrlFieldState(tavilySearchEnabledCheckBox, tavilySearchApiKeyField);
            }
            stateService.setGoogleSearchEnabled(googleSearchEnabledCheckBox.isSelected());
            updateUrlFieldState(googleSearchEnabledCheckBox, googleSearchApiKeyField);
        });
    }

    private void updateUrlFieldState(@NotNull JCheckBox checkbox,
                                     @NotNull JComponent urlComponent) {
        urlComponent.setEnabled(checkbox.isSelected());
    }
}
