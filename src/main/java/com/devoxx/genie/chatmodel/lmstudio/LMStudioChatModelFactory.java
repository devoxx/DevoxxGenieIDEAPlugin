package com.devoxx.genie.chatmodel.lmstudio;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class LMStudioChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getLmstudioModelUrl())
            .modelName("LMStudio")
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxTokens(chatModel.getMaxTokens())
            .maxRetries(chatModel.getMaxRetries())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return LocalAiStreamingChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getLmstudioModelUrl())
            .modelName("LMStudio")
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }
}
