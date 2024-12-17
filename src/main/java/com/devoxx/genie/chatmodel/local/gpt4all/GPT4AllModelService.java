package com.devoxx.genie.chatmodel.local.gpt4all;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class GPT4AllModelService implements LocalLLMProvider {

    @NotNull
    public static GPT4AllModelService getInstance() {
        return ApplicationManager.getApplication().getService(GPT4AllModelService.class);
    }

    @Override
    public List<Model> getModels() throws IOException {
        return LocalLLMProviderUtil
                .getModels("gpt4allModelUrl", "models", ResponseDTO.class)
                .getData();
    }
}
