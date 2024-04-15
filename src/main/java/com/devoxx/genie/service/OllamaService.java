package com.devoxx.genie.service;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.ollama.OllamaModelDTO;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import okhttp3.*;

import java.io.IOException;
import java.util.Objects;

import static com.devoxx.genie.ui.Settings.OLLAMA_MODEL_URL;

public class OllamaService {

    public OllamaModelEntryDTO[] getModels() throws IOException {
        OkHttpClient client = new OkHttpClient();

        String baseUrl = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(OLLAMA_MODEL_URL),
            Constant.OLLAMA_MODEL_URL);

        // Ensure the base URL ends with exactly one slash
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        Request request = new Request.Builder()
            .url(baseUrl+"api/tags")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Assuming your JSON root object contains a List<String> of models
            OllamaModelDTO ollamaModelDTO = new Gson().fromJson(response.body().string(), OllamaModelDTO.class);
            if (ollamaModelDTO != null && ollamaModelDTO.getModels() != null) {
                return ollamaModelDTO.getModels();
            }
        }
        return new OllamaModelEntryDTO[0];
    }
}
