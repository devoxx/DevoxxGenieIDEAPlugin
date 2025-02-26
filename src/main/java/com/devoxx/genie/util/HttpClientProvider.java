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
            Response response = null;
            IOException exception = null;

            int tryCount = 0;
            while (tryCount < maxRetries) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    }

                    response.close();

                    tryCount++;
                    // Exponential backoff
                    try {
                        Thread.sleep((long) Math.pow(2, tryCount) * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", e);
                    }
                } catch (IOException e) {
                    exception = e;
                    tryCount++;
                    if (tryCount >= maxRetries) {
                        throw exception;
                    }
                }
            }

            if (response != null) {
                return response;
            }

            throw new IOException("Request failed after " + maxRetries + " attempts");
        }
    }
}
