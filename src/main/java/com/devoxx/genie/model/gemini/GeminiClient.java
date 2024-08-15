package com.devoxx.genie.model.gemini;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.time.Duration;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

public class GeminiClient {

    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private final GeminiApi geminiApi;

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;

    @Builder
    public GeminiClient(String baseUrl,
                        String apiKey,
                        String modelName,
                        Duration timeout) {

        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .callTimeout(timeout)
            .connectTimeout(timeout)
            .readTimeout(timeout)
            .writeTimeout(timeout)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(GSON))
            .build();

        geminiApi = retrofit.create(GeminiApi.class);
    }

    public GeminiCompletionResponse completion(GeminiMessageRequest request) {
        String url = String.format("%s/v1/models/%s:generateContent?key=%s", baseUrl, modelName, apiKey);

        try {
            retrofit2.Response<GeminiCompletionResponse> retrofitResponse = geminiApi.completion(url, request).execute();
            if (retrofitResponse.isSuccessful()) {
                return retrofitResponse.body();
            } else {
                throw toException(retrofitResponse);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull RuntimeException toException(retrofit2.@NotNull Response<?> response) throws IOException {
        int code = response.code();
        String body = response.errorBody().string();

        String errorMessage = String.format("status code: %s; body: %s", code, body);
        return new RuntimeException(errorMessage);
    }
}
