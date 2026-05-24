package com.devoxx.genie.chatmodel.local.jan;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JanChatModelFactory extends LocalChatModelFactory {

    public JanChatModelFactory() {
        super(ModelProvider.Jan);
    }

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return createOpenAiChatModel(customChatModel);
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return createOpenAiStreamingChatModel(customChatModel);
    }

    @Override
    protected String getModelUrl() {
        return DevoxxGenieStateService.getInstance().getJanModelUrl();
    }

    /**
     * Jan v0.8.0's bundled llama.cpp server hangs on multi-line JSON request bodies, which is
     * exactly what langchain4j's OpenAI client sends (pretty-printed). Decorate the HTTP client
     * so the body is compacted to a single line before it reaches the server (issue #1051).
     */
    @Override
    protected dev.langchain4j.http.client.HttpClientBuilder resolveHttpClientBuilder() {
        return new CompactJsonHttpClientBuilder(super.resolveHttpClientBuilder());
    }

    @Override
    protected Data[] fetchModels() throws IOException {
        return JanModelService.getInstance().getModels().toArray(new Data[0]);
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        Data janModel = (Data) model;
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(janModel.getId())
                .displayName(resolveDisplayName(janModel))
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(resolveContextLength(janModel))
                .apiKeyUsed(false)
                .build();
    }

    /**
     * Jan's OpenAI-compatible {@code /models} endpoint returns an {@code id} but no
     * {@code name}, so fall back to the id to keep a usable, non-null display name.
     */
    private static String resolveDisplayName(@NotNull Data janModel) {
        String name = janModel.getName();
        return (name == null || name.isBlank()) ? janModel.getId() : name;
    }

    /**
     * Context length may be reported at the top level ({@code ctx_len}) or nested under
     * {@code settings}, and may be absent entirely. Default to 8k when unknown.
     */
    private static int resolveContextLength(@NotNull Data janModel) {
        if (janModel.getSettings() != null && janModel.getSettings().getCtxLen() != null) {
            return janModel.getSettings().getCtxLen();
        }
        if (janModel.getCtxLen() != null) {
            return janModel.getCtxLen().intValue();
        }
        return 8_000;
    }
}
