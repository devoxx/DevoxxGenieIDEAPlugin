package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractChatModelFactory implements ChatModelFactory {

    protected final List<LanguageModel> LANGUAGE_MODELS = new ArrayList<>();
    protected final ModelProvider provider;

    protected AbstractChatModelFactory(ModelProvider provider) {
        this.provider = provider;
    }

    @Override
    public List<LanguageModel> getModelNames() {
        return LANGUAGE_MODELS;
    }

    public void updateModelCosts() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        for (LanguageModel model : LANGUAGE_MODELS) {
            model.setCostPer1MTokensInput(settings.getModelInputCost(provider, model.getName()));
            model.setCostPer1MTokensOutput(settings.getModelOutputCost(provider, model.getName()));
            model.setMaxTokens(settings.getModelWindowContext(provider, model.getName()));
        }
    }
}
