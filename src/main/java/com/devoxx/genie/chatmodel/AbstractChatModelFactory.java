package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.LanguageModel;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractChatModelFactory implements ChatModelFactory {

    protected final List<LanguageModel> LANGUAGE_MODELS = new ArrayList<>();

    @Override
    public List<LanguageModel> getModelNames() {
        return LANGUAGE_MODELS;
    }

    @Override
    public Double getCostPer1MTokens(String modelName) {
        return LANGUAGE_MODELS.stream()
            .filter(model -> model.getName().equals(modelName))
            .findFirst()
            .map(LanguageModel::getCostPer1MTokensInput)
            .orElse(0.0);
    }
}
