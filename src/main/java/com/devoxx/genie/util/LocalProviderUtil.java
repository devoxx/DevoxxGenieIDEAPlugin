package com.devoxx.genie.util;

import com.intellij.openapi.diagnostic.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class LocalProviderUtil {
    private static final Logger LOG = Logger.getInstance(LocalProviderUtil.class);

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(5))
        .writeTimeout(Duration.ofSeconds(5))
        .build();

    public static boolean isProviderRunning(String baseUrl) {
        try (Response response = executeRequest(baseUrl, "models")) {
            return response.isSuccessful();
        } catch (IOException e) {
            LOG.warn("Failed to connect to LMStudio: " + e.getMessage());
            return false;
        }
    }

    public static @NotNull Response executeRequest(String baseUrl, String endpoint) throws IOException {
        String url = ensureEndsWithSlash(baseUrl);
        Request request = new Request.Builder()
            .url(url + endpoint)
            .build();
        return client.newCall(request).execute();
    }
}
