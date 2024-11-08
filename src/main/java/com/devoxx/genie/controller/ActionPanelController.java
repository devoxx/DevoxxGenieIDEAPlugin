package com.devoxx.genie.controller;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.*;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class ActionPanelController {
    private final Project project;
    private final ChatPromptExecutor chatPromptExecutor;
    private final EditorFileButtonManager editorFileButtonManager;
    private final PromptInputArea promptInputArea;
    private final PromptOutputPanel promptOutputPanel;
    private final ComboBox<ModelProvider> modelProviderComboBox;
    private final ComboBox<LanguageModel> modelNameComboBox;
    private final ChatModelProvider chatModelProvider = new ChatModelProvider();
    private final ActionButtonsPanel actionButtonsPanel;
    private boolean isPromptRunning = false;

    private ChatMessageContext currentChatMessageContext;

    public ActionPanelController(Project project,
                                 PromptInputArea promptInputArea,
                                 PromptOutputPanel promptOutputPanel,
                                 ComboBox<ModelProvider> modelProviderComboBox,
                                 ComboBox<LanguageModel> modelNameComboBox,
                                 ActionButtonsPanel actionButtonsPanel) {

        this.project = project;
        this.promptInputArea = promptInputArea;
        this.promptOutputPanel = promptOutputPanel;
        this.chatPromptExecutor = new ChatPromptExecutor(promptInputArea);
        this.editorFileButtonManager = new EditorFileButtonManager(project, null);
        this.modelProviderComboBox = modelProviderComboBox;
        this.modelNameComboBox = modelNameComboBox;
        this.actionButtonsPanel = actionButtonsPanel;
    }

    public boolean isPromptRunning() {
        return isPromptRunning;
    }

    public boolean executePrompt(String actionCommand,
                                 boolean isProjectContextAdded,
                                 String projectContext) {
        if (isPromptRunning) {
            stopPromptExecution();
            return true;
        }

        if (!validateAndPreparePrompt(actionCommand, isProjectContextAdded, projectContext)) {
            return false;
        }

        isPromptRunning = true;

        AtomicBoolean response = new AtomicBoolean(true);
        chatPromptExecutor.updatePromptWithCommandIfPresent(currentChatMessageContext, promptOutputPanel)
            .ifPresentOrElse(
                this::executePromptWithContext,
                () -> response.set(false)
            );

        return response.get();
    }

    private void executePromptWithContext(String command) {
        chatPromptExecutor.executePrompt(currentChatMessageContext, promptOutputPanel, () -> {
            isPromptRunning = false;
            actionButtonsPanel.enableButtons();
            ApplicationManager.getApplication().invokeLater(() -> {
                promptInputArea.clear();
                promptInputArea.requestInputFocus();
            });
        });
    }

    public void stopPromptExecution() {
        chatPromptExecutor.stopPromptExecution(project);
        isPromptRunning = false;
        actionButtonsPanel.enableButtons();
    }

    /**
     * Validate and prepare the prompt.
     *
     * @param actionCommand the action event command
     * @return true if the prompt is valid
     */
    private boolean validateAndPreparePrompt(String actionCommand,
                                             boolean isProjectContextAdded,
                                             String projectContext) {
        String userPromptText = getUserPromptText();
        if (userPromptText == null) {
            return false;
        }

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        LanguageModel selectedLanguageModel = (LanguageModel) modelNameComboBox.getSelectedItem();

        // If selectedLanguageModel is null, create a default one
        if (selectedLanguageModel == null) {
            selectedLanguageModel = createDefaultLanguageModel(stateService);
        }

        currentChatMessageContext = ChatMessageContextUtil.createContext(
            project,
            userPromptText,
            selectedLanguageModel,
            chatModelProvider,
            stateService,
            actionCommand,
            editorFileButtonManager,
            projectContext,
            isProjectContextAdded
        );

        return true;
    }

    /**
     * get the user prompt text.
     */
    private @Nullable String getUserPromptText() {
        String userPromptText = promptInputArea.getText();
        if (userPromptText.isEmpty()) {
            NotificationUtil.sendNotification(project, "Please enter a prompt.");
            return null;
        }
        return userPromptText;
    }

    /**
     * Create a default language model.
     *
     * @param stateService the state service
     * @return the default language model
     */
    private LanguageModel createDefaultLanguageModel(@NotNull DevoxxGenieSettingsService stateService) {
        ModelProvider selectedProvider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (selectedProvider != null &&
            (selectedProvider.equals(ModelProvider.LMStudio) ||
                selectedProvider.equals(ModelProvider.GPT4All) ||
                selectedProvider.equals(ModelProvider.Jlama) ||
                selectedProvider.equals(ModelProvider.LLaMA))) {
            return LanguageModel.builder()
                .provider(selectedProvider)
                .apiKeyUsed(false)
                .inputCost(0)
                .outputCost(0)
                .contextWindow(4096)
                .build();
        } else {
            String modelName = stateService.getSelectedLanguageModel(project.getLocationHash());
            return LanguageModel.builder()
                .provider(selectedProvider != null ? selectedProvider : ModelProvider.OpenAI)
                .modelName(modelName)
                .apiKeyUsed(false)
                .inputCost(0)
                .outputCost(0)
                .contextWindow(128_000)
                .build();
        }
    }
}
