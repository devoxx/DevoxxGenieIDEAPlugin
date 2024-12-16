package com.devoxx.genie.chatmodel.lmstudio;

import com.devoxx.genie.chatmodel.LocalChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.service.lmstudio.LMStudioService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.LMStudioUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;

public class LMStudioChatModelFactory extends LocalChatModelFactory {

    public static final int DEFAULT_CONTEXT_LENGTH = 8000;

    public LMStudioChatModelFactory() {
        super(ModelProvider.LMStudio);
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LMStudioChatModel.builder()
                .baseUrl(getModelUrl())
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .topP(chatModel.getTopP())
                .maxTokens(chatModel.getMaxTokens())
                .maxRetries(chatModel.getMaxRetries())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return createLocalAiStreamingChatModel(chatModel);
    }

    @Override
    protected String getModelUrl() {
        return DevoxxGenieStateService.getInstance().getLmstudioModelUrl();
    }

    @Override
    protected LMStudioModelEntryDTO[] fetchModels() throws IOException {
        if (!LMStudioUtil.isLMStudioRunning()) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                    "LMStudio is not running. Please start it and try again.");
            throw new IOException("LMStudio is not running");
        }
        return LMStudioService.getInstance().getModels();
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        LMStudioModelEntryDTO lmStudioModel = (LMStudioModelEntryDTO) model;
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(lmStudioModel.getId())
                .displayName(lmStudioModel.getId())
                .inputCost(0)
                .outputCost(0)
                .contextWindow(DEFAULT_CONTEXT_LENGTH)
                .apiKeyUsed(false)
                .build();
    }
}
