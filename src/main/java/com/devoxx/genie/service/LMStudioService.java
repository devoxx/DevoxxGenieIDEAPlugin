package com.devoxx.genie.service;

import com.devoxx.genie.model.lmstudio.LMStudioModelDTO;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.util.LMStudioUtil;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LMStudioService {

    @NotNull
    public static LMStudioService getInstance() {
        return ApplicationManager.getApplication().getService(LMStudioService.class);
    }

    public LMStudioModelEntryDTO[] getModels() throws IOException {
        try (Response response = LMStudioUtil.executeRequest("models")) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            assert response.body() != null;

            LMStudioModelDTO lmStudioModelDTO = new Gson().fromJson(response.body().string(), LMStudioModelDTO.class);
            return lmStudioModelDTO != null &&
                   lmStudioModelDTO.getData() != null ? lmStudioModelDTO.getData() : new LMStudioModelEntryDTO[0];
        }
    }

    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
