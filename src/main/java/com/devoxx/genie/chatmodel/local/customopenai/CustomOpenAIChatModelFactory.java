package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.net.http.HttpClient;

public class CustomOpenAIChatModelFactory implements ChatModelFactory {

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService stateInstance = DevoxxGenieStateService.getInstance();
        return OpenAiChatModel.builder()
                .baseUrl(stateInstance.getCustomOpenAIUrl())
                .apiKey(stateInstance.isCustomOpenAIApiKeyEnabled() ? stateInstance.getCustomOpenAIApiKey() : "na")
                .modelName(stateInstance.isCustomOpenAIModelNameEnabled() ?
                        (stateInstance.getCustomOpenAIModelName().isBlank() ? "default" : stateInstance.getCustomOpenAIModelName()) : "")
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(customChatModel.getTemperature())
                .maxTokens(customChatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(customChatModel.getTopP())
                .listeners(getListener())
                .httpClientBuilder(getHttpClientBuilder())
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService stateInstance = DevoxxGenieStateService.getInstance();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(stateInstance.getCustomOpenAIUrl())
                .apiKey(stateInstance.isCustomOpenAIApiKeyEnabled() ? stateInstance.getCustomOpenAIApiKey() : "na")
                .modelName(stateInstance.isCustomOpenAIModelNameEnabled() ?
                        (stateInstance.getCustomOpenAIModelName().isBlank() ? "default" : stateInstance.getCustomOpenAIModelName()) : "")
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .listeners(getListener())
                .httpClientBuilder(getHttpClientBuilder())
                .build();
    }

    private JdkHttpClientBuilder getHttpClientBuilder() {
        boolean forceHttp11 = DevoxxGenieStateService.getInstance().isCustomOpenAIForceHttp11();
        return forceHttp11
                ? JdkHttpClient.builder()
                        .httpClientBuilder(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1))
                : JdkHttpClient.builder();
    }

    /**
     * Get the model names from the custom local OpenAI compliant service.
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {
        return Collections.emptyList();
    }
}
