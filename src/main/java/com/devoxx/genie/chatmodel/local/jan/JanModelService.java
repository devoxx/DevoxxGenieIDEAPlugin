package com.devoxx.genie.chatmodel.local.jan;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.model.jan.ResponseDTO;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class JanModelService implements LocalLLMProvider {

    @NotNull
    public static JanModelService getInstance() {
        return ApplicationManager.getApplication().getService(JanModelService.class);
    }

    @Override
    public List<Data> getModels() throws IOException {
        return LocalLLMProviderUtil
                .getModels("janModelUrl", "models", ResponseDTO.class)
                .getData();
    }
}
