package com.devoxx.genie.model.gemini;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface GeminiApi {

    @POST
    @Headers({"Content-Type: application/json"})
    Call<GeminiCompletionResponse> completion(@Url String url, @Body GeminiMessageRequest completionRequest);
}
