package com.devoxx.genie.util;

import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class LMStudioUtil {
    private static final Logger LOG = Logger.getInstance(LMStudioUtil.class);

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(5))
        .writeTimeout(Duration.ofSeconds(5))
        .build();

    public static boolean isLMStudioRunning() {
        try (Response response = executeRequest("models")) {
            return response.isSuccessful();
        } catch (IOException e) {
            LOG.warn("Failed to connect to LMStudio: " + e.getMessage());
            return false;
        }
    }

    public static @NotNull Response executeRequest(String endpoint) throws IOException {
        String baseUrl = ensureEndsWithSlash(DevoxxGenieSettingsServiceProvider.getInstance().getLmstudioModelUrl());
        Request request = new Request.Builder()
            .url(baseUrl + endpoint)
            .build();
        return client.newCall(request).execute();
    }
}
