package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RAGSettingsConfigurable implements Configurable {

    private final RAGSettingsComponent ragSettingsComponent;

    public RAGSettingsConfigurable(Project project) {
        ragSettingsComponent = new RAGSettingsComponent(project);
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
        return ragSettingsComponent.createPanel();
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
        isModified |= ragSettingsComponent.getEnableIndexerCheckBox().isSelected() != stateService.getRagEnabled();
        isModified |= ragSettingsComponent.getPortIndexer().getNumber() != stateService.getIndexerPort();
        isModified |= ragSettingsComponent.getMaxResultsSpinner().getNumber() != stateService.getIndexerMaxResults();
        isModified |= ragSettingsComponent.getMinScoreField().getValue() != stateService.getIndexerMinScore();

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        boolean oldValue = stateService.getRagEnabled();
        boolean newValue = ragSettingsComponent.getEnableIndexerCheckBox().isSelected();

        stateService.setRagEnabled(ragSettingsComponent.getEnableIndexerCheckBox().isSelected());
        stateService.setIndexerPort(ragSettingsComponent.getPortIndexer().getNumber());
        stateService.setIndexerMinScore((Double) ragSettingsComponent.getMinScoreField().getValue());
        stateService.setIndexerMaxResults(ragSettingsComponent.getMaxResultsSpinner().getNumber());

        if (oldValue != newValue) {
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.RAG_STATE_TOPIC)
                    .onRAGStateChanged(newValue);
        }
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        ragSettingsComponent.getEnableIndexerCheckBox().setSelected(stateService.getRagEnabled());
        ragSettingsComponent.getPortIndexer().setNumber(stateService.getIndexerPort());
        ragSettingsComponent.getMinScoreField().setValue(stateService.getIndexerMinScore());
        ragSettingsComponent.getMaxResultsSpinner().setNumber(stateService.getIndexerMaxResults());
    }
}
