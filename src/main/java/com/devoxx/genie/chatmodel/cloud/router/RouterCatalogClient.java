package com.devoxx.genie.chatmodel.cloud.router;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.Duration;

/** Fetches one router catalog endpoint. Exception messages must never carry the key or the response body. */
interface RouterCatalogClient {

    String fetch(String url, String apiKey) throws IOException;

    /** A single fast-fail attempt, so an unreachable router degrades quickly instead of tying up the caller. */
    final class OkHttp implements RouterCatalogClient {

        private static final OkHttpClient HTTP = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .writeTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(false)
                .build();

        @Override
        public String fetch(String url, String apiKey) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .build();
            try (Response response = HTTP.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code());
                }
                ResponseBody body = response.body();
                return body == null ? "" : body.string();
            }
        }
    }
}
