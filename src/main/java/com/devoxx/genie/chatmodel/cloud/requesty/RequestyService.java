package com.devoxx.genie.chatmodel.cloud.requesty;

import com.devoxx.genie.model.requesty.Data;
import com.devoxx.genie.model.requesty.ResponseDTO;
import com.devoxx.genie.service.exception.UnsuccessfulRequestException;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches the Requesty model catalog.
 * <p>
 * {@code /v1/models/managed} is public and returns the curated list of models,
 * {@code /v1/models} needs an API key and returns the full catalog. Both are merged,
 * the managed entries come first and duplicates are removed by id.
 */
public class RequestyService {

    public static final String BASE_URL = "https://router.requesty.ai/v1";
    private static final String MANAGED_MODELS_URL = BASE_URL + "/models/managed";
    private static final String ALL_MODELS_URL = BASE_URL + "/models";

    private final OkHttpClient client = HttpClientProvider.getClient();

    @NotNull
    public static RequestyService getInstance() {
        return ApplicationManager.getApplication().getService(RequestyService.class);
    }

    public List<Data> getModels(@Nullable String apiKey) throws IOException {
        Map<String, Data> merged = new LinkedHashMap<>();
        IOException firstError = null;

        try {
            for (Data data : fetch(MANAGED_MODELS_URL, null)) {
                merged.putIfAbsent(data.getId(), data);
            }
        } catch (IOException e) {
            firstError = e;
        }

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                for (Data data : fetch(ALL_MODELS_URL, apiKey)) {
                    merged.putIfAbsent(data.getId(), data);
                }
            } catch (IOException e) {
                if (merged.isEmpty()) {
                    throw e;
                }
            }
        }

        if (merged.isEmpty() && firstError != null) {
            throw firstError;
        }

        return new ArrayList<>(merged.values());
    }

    private List<Data> fetch(@NotNull String url, @Nullable String apiKey) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new UnsuccessfulRequestException("Response is empty");
            }

            Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

            ResponseDTO responseDTO = gson.fromJson(response.body().string(), ResponseDTO.class);
            return responseDTO != null && responseDTO.getData() != null ? responseDTO.getData() : List.of();
        }
    }
}
