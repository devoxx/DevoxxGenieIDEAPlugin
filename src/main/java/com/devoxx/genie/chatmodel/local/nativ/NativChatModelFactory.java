package com.devoxx.genie.chatmodel.local.nativ;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.nativ.NativModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Nativ (<a href="https://blaizzy.github.io/nativ/">blaizzy.github.io/nativ</a>) runs MLX models
 * locally on Apple Silicon and serves them over an OpenAI-compatible API, so it plugs straight
 * into the shared OpenAI chat/streaming clients.
 */
public class NativChatModelFactory extends LocalChatModelFactory {

    /**
     * Nativ's {@code /v1/models} response carries no context-length field (see
     * {@link NativModelEntryDTO}), so the window has to be assumed. 8k is the conservative floor
     * that virtually every MLX chat model meets; users running larger-context models raise it via
     * the "Nativ Fallback Context" setting.
     */
    public static final int DEFAULT_CONTEXT_LENGTH = 8000;

    public NativChatModelFactory() {
        super(ModelProvider.Nativ);
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
        return DevoxxGenieStateService.getInstance().getNativModelUrl();
    }

    @Override
    protected NativModelEntryDTO[] fetchModels() throws IOException {
        return NativModelService.getInstance().getModels().toArray(new NativModelEntryDTO[0]);
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        NativModelEntryDTO nativModel = (NativModelEntryDTO) model;
        Integer configuredFallback = DevoxxGenieStateService.getInstance().getNativFallbackContextLength();
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(nativModel.getId())
                .displayName(nativModel.resolveDisplayName())
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(configuredFallback != null ? configuredFallback : DEFAULT_CONTEXT_LENGTH)
                .apiKeyUsed(false)
                .build();
    }
}
