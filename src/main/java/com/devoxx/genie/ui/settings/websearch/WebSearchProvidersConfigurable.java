package com.devoxx.genie.ui.settings.websearch;

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

    // Snapshot of isWebSearchEnabled taken when the settings dialog opens (reset()).
    // The ItemListener in WebSearchProvidersComponent mutates stateService immediately on every
    // checkbox change, so stateService can no longer be used as the "before" value in isModified().
    // Comparing against this snapshot ensures isModified() returns true when the user actually
    // toggled the checkbox, so IntelliJ calls apply() and the WEB_SEARCH_STATE_TOPIC is published.
    private boolean initialIsWebSearchEnabled;

    public WebSearchProvidersConfigurable(Project project) {
        this.project = project;
        webSearchProvidersComponent = new WebSearchProvidersComponent();
        initialIsWebSearchEnabled = DevoxxGenieStateService.getInstance().getIsWebSearchEnabled();
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
        return webSearchProvidersComponent.createPanelWithHelp();
    }

    /**
     * Check if the settings have been modified
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // Use initialIsWebSearchEnabled (not stateService) for the enable checkbox: the ItemListener
        // in WebSearchProvidersComponent mutates stateService live, so stateService always equals
        // the checkbox and would make this check always return false.
        boolean isModified = webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected() != initialIsWebSearchEnabled;

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

        boolean isWebSearchEnabled = webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected();

        settings.setIsWebSearchEnabled(isWebSearchEnabled);
        settings.setTavilySearchEnabled(webSearchProvidersComponent.getTavilySearchEnabledCheckBox().isSelected());
        settings.setTavilySearchKey(new String(webSearchProvidersComponent.getTavilySearchApiKeyField().getPassword()));
        settings.setGoogleSearchEnabled(webSearchProvidersComponent.getGoogleSearchEnabledCheckBox().isSelected());
        settings.setGoogleSearchKey(new String(webSearchProvidersComponent.getGoogleSearchApiKeyField().getPassword()));
        settings.setGoogleCSIKey(new String(webSearchProvidersComponent.getGoogleCSIApiKeyField().getPassword()));
        settings.setMaxSearchResults(webSearchProvidersComponent.getMaxSearchResults().getNumber());

        // Re-arm the feature-enablement analytics snapshot (task-209).
        com.devoxx.genie.service.analytics.DevoxxGenieSettingsChangedTopic.notifySettingsChanged();

        // Always publish so the Web switch in SearchOptionsPanel reflects the current state.
        // The ItemListener in WebSearchProvidersComponent already mutates stateService directly,
        // so comparing oldValue/newValue here would always see them as equal.
        project.getMessageBus()
                .syncPublisher(AppTopics.WEB_SEARCH_STATE_TOPIC)
                .onWebSearchStateChange(isWebSearchEnabled);
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        // Refresh snapshot before resetting the checkbox (the checkbox's ItemListener will fire
        // and update stateService, but the snapshot stays anchored to the persisted value).
        initialIsWebSearchEnabled = settings.getIsWebSearchEnabled();

        webSearchProvidersComponent.getEnableWebSearchCheckbox().setSelected(settings.getIsWebSearchEnabled());

        webSearchProvidersComponent.getTavilySearchEnabledCheckBox().setSelected(settings.isTavilySearchEnabled());
        webSearchProvidersComponent.getTavilySearchApiKeyField().setText(settings.getTavilySearchKey());

        webSearchProvidersComponent.getGoogleSearchEnabledCheckBox().setSelected(settings.isGoogleSearchEnabled());
        webSearchProvidersComponent.getGoogleSearchApiKeyField().setText(settings.getGoogleSearchKey());

        webSearchProvidersComponent.getGoogleCSIApiKeyField().setText(settings.getGoogleCSIKey());
        webSearchProvidersComponent.getMaxSearchResults().setValue(settings.getMaxSearchResults());
    }
}
