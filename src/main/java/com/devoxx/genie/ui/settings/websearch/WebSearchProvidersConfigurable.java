package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class WebSearchProvidersConfigurable implements Configurable {

    private final WebSearchProvidersComponent webSearchProvidersComponent;

    public WebSearchProvidersConfigurable() {
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
        return webSearchProvidersComponent.createPanel();
    }

    /**
     * Check if the settings have been modified
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= !stateService.getEnableWebSearch().equals(webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected());
        webSearchProvidersComponent.getEnableWebSearchCheckbox().addItemListener(event -> {
            String text = webSearchProvidersComponent.getEnableWebSearchCheckbox().getText();
            stateService.setEnableWebSearch(text.equals("true"));
        });

        boolean oldValue = stateService.getGitDiffEnabled();
        boolean newValue = webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected();

        isModified |= webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected() != stateService.getEnableWebSearch();
        isModified |= isFieldModified(webSearchProvidersComponent.getTavilySearchApiKeyField(), stateService.getTavilySearchKey());
        isModified |= isFieldModified(webSearchProvidersComponent.getGoogleSearchApiKeyField(), stateService.getGoogleSearchKey());
        isModified |= isFieldModified(webSearchProvidersComponent.getGoogleCSIApiKeyField(), stateService.getGoogleCSIKey());
        isModified |= webSearchProvidersComponent.getMaxSearchResults().getNumber() != stateService.getMaxSearchResults();

        if (oldValue != newValue) {
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.WEB_SEARCH_STATE_TOPIC)
                    .onWebSearchStateChange(newValue);
        }

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        settings.setEnableWebSearch(webSearchProvidersComponent.getEnableWebSearchCheckbox().isSelected());
        settings.setTavilySearchKey(new String(webSearchProvidersComponent.getTavilySearchApiKeyField().getPassword()));
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

        webSearchProvidersComponent.getEnableWebSearchCheckbox().setSelected(settings.getEnableWebSearch());
        webSearchProvidersComponent.getTavilySearchApiKeyField().setText(settings.getTavilySearchKey());
        webSearchProvidersComponent.getGoogleSearchApiKeyField().setText(settings.getGoogleSearchKey());
        webSearchProvidersComponent.getGoogleCSIApiKeyField().setText(settings.getGoogleCSIKey());
        webSearchProvidersComponent.getMaxSearchResults().setValue(settings.getMaxSearchResults());
    }
}
