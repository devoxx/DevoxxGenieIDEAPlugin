package com.devoxx.genie.util;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class HttpClientProvider {

    private static final OkHttpClient sharedClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(30))
            .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .addInterceptor(new RetryInterceptor(3))
            .build();

    public static OkHttpClient getClient() {
        return sharedClient;
    }

    public record RetryInterceptor(int maxRetries) implements Interceptor {

        @Override
        public @NotNull Response intercept(@NotNull Chain chain) throws IOException {
            Request request = chain.request();
            for (int attempt = 1; ; attempt++) {
                boolean lastAttempt = attempt >= maxRetries;
                try {
                    Response response = chain.proceed(request);
                    // Return the last attempt's response open, so the caller sees the real HTTP status and body.
                    // Closing it first made callers that read the body (Retrofit) fail with "IllegalStateException: closed".
                    if (response.isSuccessful() || lastAttempt) {
                        return response;
                    }
                    response.close();
                } catch (IOException e) {
                    if (lastAttempt) {
                        throw e;
                    }
                }
                // Exponential backoff, outside the try so an interruption stops the retries instead of being
                // caught as one more failed attempt.
                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }
        }
    }
}
