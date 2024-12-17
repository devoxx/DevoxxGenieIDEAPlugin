package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.local.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.local.jan.JanChatModelFactory;
import com.devoxx.genie.chatmodel.local.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.local.ollama.OllamaChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.devoxx.genie.chatmodel.ChatModelFactory.TEST_MODEL;

@Setter
public class ChatModelProvider {

    private static final ModelProvider DEFAULT_PROVIDER = ModelProvider.OpenAI;

    public ChatLanguageModel getChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createChatModel(chatModel);
    }

    public StreamingChatLanguageModel getStreamingChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createStreamingChatModel(chatModel);
    }

    private @NotNull ChatModelFactory getFactory(@NotNull ChatMessageContext chatMessageContext) {
        ModelProvider provider = Optional.ofNullable(chatMessageContext.getLanguageModel())
            .map(LanguageModel::getProvider)
            .orElse(DEFAULT_PROVIDER);

        return ChatModelFactoryProvider.getFactoryByProvider(provider.name())
            .orElseThrow(() -> new IllegalArgumentException("No factory for provider: " + provider));
    }

    public @NotNull ChatModel initChatModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = new ChatModel();
        DevoxxGenieSettingsService stateService = DevoxxGenieStateService.getInstance();
        setMaxOutputTokens(stateService, chatModel);

        chatModel.setTemperature(stateService.getTemperature());
        chatModel.setMaxRetries(stateService.getMaxRetries());
        chatModel.setTopP(stateService.getTopP());
        chatModel.setTimeout(stateService.getTimeout());

        LanguageModel languageModel = chatMessageContext.getLanguageModel();
        chatModel.setModelName(languageModel.getModelName() == null ? TEST_MODEL : languageModel.getModelName());

        setLocalBaseUrl(languageModel, chatModel, stateService);

        return chatModel;
    }

    private void setLocalBaseUrl(@NotNull LanguageModel languageModel,
                                 ChatModel chatModel,
                                 DevoxxGenieSettingsService stateService) {
        // Set base URL for local providers
        switch (languageModel.getProvider()) {
            case LMStudio:
                chatModel.setBaseUrl(stateService.getLmstudioModelUrl());
                break;
            case Ollama:
                chatModel.setBaseUrl(stateService.getOllamaModelUrl());
                break;
            case GPT4All:
                chatModel.setBaseUrl(stateService.getGpt4allModelUrl());
                break;
            case LLaMA:
                chatModel.setBaseUrl(stateService.getLlamaCPPUrl());
                break;
            case CustomOpenAI:
                chatModel.setBaseUrl(stateService.getCustomOpenAIUrl());
                break;
            // Add other local providers as needed
        }
    }

    private static void setMaxOutputTokens(@NotNull DevoxxGenieSettingsService settingsState,
                                           @NotNull ChatModel chatModel) {
        Integer maxOutputTokens = settingsState.getMaxOutputTokens();
        chatModel.setMaxTokens(maxOutputTokens != null ? maxOutputTokens : Constant.MAX_OUTPUT_TOKENS);
    }
}
