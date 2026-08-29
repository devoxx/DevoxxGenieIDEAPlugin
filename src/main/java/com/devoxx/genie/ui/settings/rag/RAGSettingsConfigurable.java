package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RAGSettingsConfigurable implements Configurable {

    private final RAGSettingsComponent ragSettingsComponent;
    private final Project project;

    public RAGSettingsConfigurable(Project project) {
        this.project = project;
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
        return ragSettingsComponent.createPanelWithHelp();
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
        isModified |= ragSettingsComponent.getQueryExpansionCheckBox().isSelected()
                != Boolean.TRUE.equals(stateService.getRagQueryExpansionEnabled());
        int storedN = stateService.getRagQueryExpansionN() == null ? 3 : stateService.getRagQueryExpansionN();
        isModified |= ragSettingsComponent.getQueryExpansionVariantsSpinner().getNumber() != storedN;
        isModified |= !ragSettingsComponent.getRagExcludedDirsPanel().getData()
                .equals(stateService.getRagExcludedDirectories());

        // Reranker (task-214)
        isModified |= ragSettingsComponent.getRerankCheckBox().isSelected()
                != Boolean.TRUE.equals(stateService.getRerankResults());
        String storedModel = stateService.getRerankerModelName();
        isModified |= !ragSettingsComponent.getRerankerModelField().getText().equals(storedModel);
        int storedShortlist = stateService.getRerankerShortlistSize() == null ? 30 : stateService.getRerankerShortlistSize();
        isModified |= ragSettingsComponent.getRerankerShortlistSpinner().getNumber() != storedShortlist;
        int storedTimeout = stateService.getRerankerTimeoutMs() == null ? 2000 : stateService.getRerankerTimeoutMs();
        isModified |= ragSettingsComponent.getRerankerTimeoutSpinner().getNumber() != storedTimeout;

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
        stateService.setRagQueryExpansionEnabled(ragSettingsComponent.getQueryExpansionCheckBox().isSelected());
        stateService.setRagQueryExpansionN(ragSettingsComponent.getQueryExpansionVariantsSpinner().getNumber());
        stateService.setRagExcludedDirectories(
                new java.util.ArrayList<>(ragSettingsComponent.getRagExcludedDirsPanel().getData()));

        // Reranker (task-214)
        stateService.setRerankResults(ragSettingsComponent.getRerankCheckBox().isSelected());
        stateService.setRerankerModelName(ragSettingsComponent.getRerankerModelField().getText().trim());
        stateService.setRerankerShortlistSize(ragSettingsComponent.getRerankerShortlistSpinner().getNumber());
        stateService.setRerankerTimeoutMs(ragSettingsComponent.getRerankerTimeoutSpinner().getNumber());

        // Re-arm the feature-enablement analytics snapshot (task-209).
        com.devoxx.genie.service.analytics.DevoxxGenieSettingsChangedTopic.notifySettingsChanged();

        if (oldValue != newValue) {
            project.getMessageBus()
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
        ragSettingsComponent.getQueryExpansionCheckBox().setSelected(
                Boolean.TRUE.equals(stateService.getRagQueryExpansionEnabled()));
        ragSettingsComponent.getQueryExpansionVariantsSpinner().setNumber(
                stateService.getRagQueryExpansionN() == null ? 3 : stateService.getRagQueryExpansionN());
        ragSettingsComponent.getRagExcludedDirsPanel().setData(
                new java.util.ArrayList<>(stateService.getRagExcludedDirectories()));

        // Reranker (task-214)
        ragSettingsComponent.getRerankCheckBox().setSelected(
                Boolean.TRUE.equals(stateService.getRerankResults()));
        ragSettingsComponent.getRerankerModelField().setText(stateService.getRerankerModelName());
        ragSettingsComponent.getRerankerShortlistSpinner().setNumber(
                stateService.getRerankerShortlistSize() == null ? 30 : stateService.getRerankerShortlistSize());
        ragSettingsComponent.getRerankerTimeoutSpinner().setNumber(
                stateService.getRerankerTimeoutMs() == null ? 2000 : stateService.getRerankerTimeoutMs());
    }
}
