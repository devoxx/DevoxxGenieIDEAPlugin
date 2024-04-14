package com.devoxx.genie.service;

import com.devoxx.genie.model.ollama.OllamaModelDTO;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;

public class OllamaService {

    public OllamaModelEntryDTO[] getModels() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .url("http://localhost:11434/api/tags")
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
