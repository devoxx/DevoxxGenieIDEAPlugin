package com.devoxx.genie.model.gemini;

import retrofit2.Call;
import retrofit2.http.*;

public interface GeminiApi {

    @POST
    @Headers({"Content-Type: application/json"})
    Call<GeminiCompletionResponse> completion(@Url String url, @Body GeminiMessageRequest completionRequest);
}
