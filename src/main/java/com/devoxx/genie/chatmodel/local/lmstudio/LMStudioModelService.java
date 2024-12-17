package com.devoxx.genie.chatmodel.local.lmstudio;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LMStudioModelService implements LocalLLMProvider {

    @NotNull
    public static LMStudioModelService getInstance() {
        return ApplicationManager.getApplication().getService(LMStudioModelService.class);
    }

    @Override
    public LMStudioModelEntryDTO[] getModels() throws IOException {
        return LocalLLMProviderUtil
                .getModels("lmStudioModelUrl", "models", LMStudioModelEntryDTO[].class);
    }
}
