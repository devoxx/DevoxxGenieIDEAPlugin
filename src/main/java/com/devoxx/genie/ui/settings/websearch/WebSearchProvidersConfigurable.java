package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.websearch.WebSearchCredentialField;
import com.devoxx.genie.service.websearch.WebSearchProviderRegistry;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

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
        initialIsWebSearchEnabled = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getIsWebSearchEnabled());
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

        isModified |= webSearchProvidersComponent.getSelectedProvider() != stateService.getSelectedWebSearchProvider();
        for (CredentialKey key : credentialKeys()) {
            isModified |= isFieldModified(webSearchProvidersComponent.getCredentialField(key), stateService.getCredential(key));
        }

        isModified |= !java.util.Objects.equals(
                webSearchProvidersComponent.getMaxSearchResults().getNumber(), stateService.getMaxSearchResults());

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
        settings.setSelectedWebSearchProvider(webSearchProvidersComponent.getSelectedProvider());
        // Keep every provider's keys (not only the selected one) so switching back needs no re-entry.
        for (CredentialKey key : credentialKeys()) {
            settings.setCredential(key, new String(webSearchProvidersComponent.getCredentialField(key).getPassword()));
        }
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

    /** Every credential of every registered provider, in registry order. */
    private static List<CredentialKey> credentialKeys() {
        return WebSearchProviderRegistry.all().stream()
                .flatMap(definition -> definition.fields().stream())
                .map(WebSearchCredentialField::key)
                .toList();
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        // Refresh snapshot before resetting the checkbox (the checkbox's ItemListener will fire
        // and update stateService, but the snapshot stays anchored to the persisted value).
        initialIsWebSearchEnabled = Boolean.TRUE.equals(settings.getIsWebSearchEnabled());

        webSearchProvidersComponent.getEnableWebSearchCheckbox().setSelected(Boolean.TRUE.equals(settings.getIsWebSearchEnabled()));

        webSearchProvidersComponent.setSelectedProvider(settings.getSelectedWebSearchProvider());
        for (CredentialKey key : credentialKeys()) {
            webSearchProvidersComponent.getCredentialField(key).setText(settings.getCredential(key));
        }
        webSearchProvidersComponent.getMaxSearchResults().setValue(settings.getMaxSearchResults());
    }
}
