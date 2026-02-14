package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class WebSearchProvidersConfigurable implements Configurable {

    private final Project project;
    private final WebSearchProvidersComponent webSearchProvidersComponent;

    public WebSearchProvidersConfigurable(Project project) {
        this.project = project;
        webSearchProvidersComponent = new WebSearchProvidersComponent();
    }

    /**
     * Get the display name
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "Web Search";
    }

    /**
     * Get the Prompt Settings component
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return AbstractSettingsComponent.wrapWithHelpButton(
            webSearchProvidersComponent.createPanel(),
            "https://genie.devoxx.com/docs/features/web-search");
    }

    /**
     * Check if the settings have been modified
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= !stateService.getIsWebSearchEnabled().equals(webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected());

        webSearchProvidersComponent.getEnableWebSearchCheckbox().addItemListener(event -> {
            String text = webSearchProvidersComponent.getEnableWebSearchCheckbox().getText();
            stateService.setIsWebSearchEnabled(text.equalsIgnoreCase("true"));
        });

        isModified |= webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected() != stateService.getIsWebSearchEnabled();
        isModified |= stateService.isTavilySearchEnabled() != webSearchProvidersComponent.getTavilySearchEnabledCheckBox().isSelected();
        isModified |= isFieldModified(webSearchProvidersComponent.getTavilySearchApiKeyField(), stateService.getTavilySearchKey());

        isModified |= stateService.isGoogleSearchEnabled() != webSearchProvidersComponent.getGoogleSearchEnabledCheckBox().isSelected();
        isModified |= isFieldModified(webSearchProvidersComponent.getGoogleSearchApiKeyField(), stateService.getGoogleSearchKey());
        isModified |= isFieldModified(webSearchProvidersComponent.getGoogleCSIApiKeyField(), stateService.getGoogleCSIKey());

        isModified |= webSearchProvidersComponent.getMaxSearchResults().getNumber() != stateService.getMaxSearchResults();

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        settings.setIsWebSearchEnabled(webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected());
        settings.setTavilySearchEnabled(webSearchProvidersComponent.getTavilySearchEnabledCheckBox().isSelected());
        settings.setTavilySearchKey(new String(webSearchProvidersComponent.getTavilySearchApiKeyField().getPassword()));
        settings.setGoogleSearchEnabled(webSearchProvidersComponent.getGoogleSearchEnabledCheckBox().isSelected());
        settings.setGoogleSearchKey(new String(webSearchProvidersComponent.getGoogleSearchApiKeyField().getPassword()));
        settings.setGoogleCSIKey(new String(webSearchProvidersComponent.getGoogleCSIApiKeyField().getPassword()));
        settings.setMaxSearchResults(webSearchProvidersComponent.getMaxSearchResults().getNumber());
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        webSearchProvidersComponent.getEnableWebSearchCheckbox().setSelected(settings.getIsWebSearchEnabled());

        webSearchProvidersComponent.getTavilySearchEnabledCheckBox().setSelected(settings.isTavilySearchEnabled());
        webSearchProvidersComponent.getTavilySearchApiKeyField().setText(settings.getTavilySearchKey());

        webSearchProvidersComponent.getGoogleSearchEnabledCheckBox().setSelected(settings.isGoogleSearchEnabled());
        webSearchProvidersComponent.getGoogleSearchApiKeyField().setText(settings.getGoogleSearchKey());

        webSearchProvidersComponent.getGoogleCSIApiKeyField().setText(settings.getGoogleCSIKey());
        webSearchProvidersComponent.getMaxSearchResults().setValue(settings.getMaxSearchResults());
    }
}
