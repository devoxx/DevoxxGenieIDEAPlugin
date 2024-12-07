package com.devoxx.genie.ui.settings.semanticsearch;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SemanticSearchSettingsConfigurable implements Configurable {

    private final SemanticSearchSettingsComponent semanticSearchSettingsComponent;

    public SemanticSearchSettingsConfigurable(Project project) {
        semanticSearchSettingsComponent = new SemanticSearchSettingsComponent(project);
    }

    /**
     * Get the display name
     *
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "Semantic Search";
    }

    /**
     * Get the Prompt Settings component
     *
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return semanticSearchSettingsComponent.createPanel();
    }

    /**
     * Check if the settings have been modified
     *
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        boolean isModified = false;
        isModified |= semanticSearchSettingsComponent.getEnableIndexerCheckBox().isSelected() != stateService.getSemanticSearchEnabled();
        isModified |= semanticSearchSettingsComponent.getPortIndexer().getNumber() != stateService.getIndexerPort();
        isModified |= semanticSearchSettingsComponent.getMaxResults().getNumber() != stateService.getIndexerMaxResults();
        isModified |= semanticSearchSettingsComponent.getMinScoreField().getValue() != stateService.getIndexerMinScore();

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        boolean oldValue = stateService.getSemanticSearchEnabled();
        boolean newValue = semanticSearchSettingsComponent.getEnableIndexerCheckBox().isSelected();

        stateService.setSemanticSearchEnabled(semanticSearchSettingsComponent.getEnableIndexerCheckBox().isSelected());
        stateService.setIndexerPort(semanticSearchSettingsComponent.getPortIndexer().getNumber());
        stateService.setIndexerMinScore((Double) semanticSearchSettingsComponent.getMinScoreField().getValue());
        stateService.setIndexerMaxResults(semanticSearchSettingsComponent.getMaxResults().getNumber());

        if (oldValue != newValue) {
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.SEMANTIC_SEARCH_STATE_TOPIC)
                    .onSemanticSearchStateChanged(newValue);
        }
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        semanticSearchSettingsComponent.getEnableIndexerCheckBox().setSelected(stateService.getSemanticSearchEnabled());
        semanticSearchSettingsComponent.getPortIndexer().setNumber(stateService.getIndexerPort());
        semanticSearchSettingsComponent.getMinScoreField().setValue(stateService.getIndexerMinScore());
        semanticSearchSettingsComponent.getMaxResults().setNumber(stateService.getIndexerMaxResults());
    }
}
