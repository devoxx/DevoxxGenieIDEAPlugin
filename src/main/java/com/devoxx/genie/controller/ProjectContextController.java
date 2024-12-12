package com.devoxx.genie.controller;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;

public class ProjectContextController {

    private final Project project;
    private final ComboBox<ModelProvider> modelProviderComboBox;
    private final ComboBox<LanguageModel> modelNameComboBox;
    private final ActionButtonsPanel actionButtonsPanel;
    private boolean isProjectContextAdded = false;
    @Getter
    private String projectContext;
    private int tokenCount;

    public ProjectContextController(Project project,
                                    ComboBox<ModelProvider> modelProviderComboBox,
                                    ComboBox<LanguageModel> modelNameComboBox,
                                    ActionButtonsPanel actionButtonsPanel) {
        this.project = project;
        this.modelProviderComboBox = modelProviderComboBox;
        this.modelNameComboBox = modelNameComboBox;
        this.actionButtonsPanel = actionButtonsPanel;
    }

    public void resetProjectContext() {
        isProjectContextAdded = false;
        projectContext = null;
        tokenCount = 0;
        actionButtonsPanel.updateAddProjectButton(isProjectContextAdded);
        actionButtonsPanel.resetTokenUsageBar();
        NotificationUtil.sendNotification(project, "Project context removed successfully");
    }

    public void addProjectContext() {
        ModelProvider modelProvider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (modelProvider == null) {
            NotificationUtil.sendNotification(project, "Please select a provider first");
            return;
        }

        if (!isSupportedProvider(modelProvider)) {
            NotificationUtil.sendNotification(project,
                    "This feature only works for OpenAI, Anthropic, Gemini and Ollama providers because of the large token window context.");
            return;
        }

        actionButtonsPanel.setAddProjectButtonEnabled(false);
        actionButtonsPanel.setTokenUsageBarVisible(true);
        actionButtonsPanel.resetTokenUsageBar();

        int tokenLimit = getWindowContext();

        ProjectContentService.getInstance().getProjectContent(project, tokenLimit, false)
                .thenAccept(projectContent -> {
                    projectContext = "Project Context:\n" + projectContent.getContent();
                    isProjectContextAdded = true;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        tokenCount = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE).countTokens(projectContent.getContent());
                        actionButtonsPanel.updateAddProjectButton(isProjectContextAdded, tokenCount);
                        actionButtonsPanel.setAddProjectButtonEnabled(true);
                        actionButtonsPanel.updateTokenUsageBar(tokenCount, tokenLimit);
                    });
                })
                .exceptionally(ex -> {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        actionButtonsPanel.setAddProjectButtonEnabled(true);
                        actionButtonsPanel.setTokenUsageBarVisible(false);
                        NotificationUtil.sendNotification(project, "Error adding project content: " + ex.getMessage());
                    });
                    return null;
                });
    }

    public boolean isProjectContextAdded() {
        return isProjectContextAdded;
    }

    public boolean isProjectContextSupportedProvider() {
        ModelProvider selectedProvider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        return selectedProvider != null && isSupportedProvider(selectedProvider);
    }

    private boolean isSupportedProvider(@NotNull ModelProvider modelProvider) {
        return modelProvider.equals(Google) ||
                modelProvider.equals(Anthropic) ||
                modelProvider.equals(OpenAI) ||
                modelProvider.equals(Mistral) ||
                modelProvider.equals(DeepSeek) ||
                modelProvider.equals(OpenRouter) ||
                modelProvider.equals(DeepInfra) ||
                modelProvider.equals(Ollama);
    }

    private int getWindowContext() {
        LanguageModel languageModel = (LanguageModel) modelNameComboBox.getSelectedItem();
        int tokenLimit = 4096;
        if (languageModel != null) {
            tokenLimit = languageModel.getContextWindow();
        }
        return tokenLimit;
    }
}
