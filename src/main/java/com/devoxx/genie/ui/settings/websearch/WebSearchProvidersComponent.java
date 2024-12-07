package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

@Getter
public class WebSearchProvidersComponent extends AbstractSettingsComponent {

    private final JCheckBox enableWebSearchCheckbox =
            new JCheckBox("", stateService.getEnableWebSearch());

    private final JPasswordField tavilySearchApiKeyField =
            new JPasswordField(stateService.getTavilySearchKey());

    private final JPasswordField googleSearchApiKeyField =
            new JPasswordField(stateService.getGoogleSearchKey());

    private final JPasswordField googleCSIApiKeyField =
            new JPasswordField(stateService.getGoogleCSIKey());

    private final JBIntSpinner maxSearchResults =
            new JBIntSpinner(new UINumericRange(stateService.getMaxSearchResults(), 1, 10));

    public WebSearchProvidersComponent() {
        validateSearchElements();
        addListeners();
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
        infoLabel.setText("<html><body style='width: 100%;'>" +
                "Post your question prompt on the web using either<br>" +
                "Google search or Tavily search.</body></html>");
        infoLabel.setForeground(UIUtil.getContextHelpForeground());
        infoLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(infoLabel, gbc);

        gbc.gridy++;
        addSettingRow(panel, gbc, "Enable feature", enableWebSearchCheckbox);

        addSettingRow(panel, gbc, "Tavily Web Search API Key",
                createTextWithPasswordButton(tavilySearchApiKeyField, "https://app.tavily.com/home"));

        addSettingRow(panel, gbc, "Google Web Search API Key",
                createTextWithPasswordButton(googleSearchApiKeyField, "https://developers.google.com/custom-search/docs/paid_element#api_key"));

        addSettingRow(panel, gbc, "Google Custom Search Engine ID",
                createTextWithPasswordButton(googleCSIApiKeyField, "https://programmablesearchengine.google.com/controlpanel/create"));

        addSettingRow(panel, gbc, "Max search results", maxSearchResults);

        return panel;
    }

    @Override
    public void addListeners() {
        enableWebSearchCheckbox.addItemListener(
                event -> stateService.setEnableWebSearch(event.getStateChange() == ItemEvent.SELECTED));
    }

    private void validateSearchElements() {
        // TODO We need to check if either Google search URL or Tavily search URL is set to enable the search feature
    }
}
