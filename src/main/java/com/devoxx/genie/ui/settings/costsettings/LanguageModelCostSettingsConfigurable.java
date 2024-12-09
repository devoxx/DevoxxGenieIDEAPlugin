package com.devoxx.genie.ui.settings.costsettings;

<<<<<<< HEAD
=======
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
>>>>>>> master
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
<<<<<<< HEAD

public class LanguageModelCostSettingsConfigurable implements Configurable {

=======
import java.util.List;

public class LanguageModelCostSettingsConfigurable implements Configurable {

    private LanguageModelCostSettingsComponent llmCostSettingsComponent = new LanguageModelCostSettingsComponent();
    private final LanguageModelCostSettingsComponent component = new LanguageModelCostSettingsComponent();
    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

>>>>>>> master
    private final MessageBus messageBus;

    public LanguageModelCostSettingsConfigurable(@NotNull Project project) {
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "LLM Costs";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
<<<<<<< HEAD
        return new LanguageModelCostSettingsComponent().createPanel();
=======
        llmCostSettingsComponent = new LanguageModelCostSettingsComponent();
        // llmCostSettingsComponent.reset(); // This will load the current (including default) values
        return llmCostSettingsComponent.createPanel();
>>>>>>> master
    }

    @Override
    public boolean isModified() {
<<<<<<< HEAD
=======
        List<LanguageModel> currentModels = stateService.getLanguageModels();
        // List<LanguageModel> modifiedModels = component.getModifiedModels();

//        if (currentModels.size() != modifiedModels.size()) {
//            return true;
//        }
//
//        for (int i = 0; i < currentModels.size(); i++) {
//            LanguageModel current = currentModels.get(i);
//            LanguageModel modified = modifiedModels.get(i);
//
//            if (!current.getProvider().equals(modified.getProvider()) ||
//                !current.getModelName().equals(modified.getModelName()) ||
//                current.getInputCost() != modified.getInputCost() ||
//                current.getOutputCost() != modified.getOutputCost() ||
//                current.getContextWindow() != modified.getContextWindow()) {
//                return true;
//            }
//        }

>>>>>>> master
        return false;
    }

    @Override
    public void apply() {
<<<<<<< HEAD
        // Notify listeners that settings have changed
        messageBus.syncPublisher(AppTopics.LLM_SETTINGS_CHANGED_TOPIC).llmSettingsChanged();
    }
=======
        // List<LanguageModel> modifiedModels = component.getModifiedModels();
        // stateService.setLanguageModels(modifiedModels);

        // Notify listeners that settings have changed
        messageBus.syncPublisher(AppTopics.LLM_SETTINGS_CHANGED_TOPIC).llmSettingsChanged();
    }

    @Override
    public void reset() {
        // llmCostSettingsComponent.reset();
    }
>>>>>>> master
}
