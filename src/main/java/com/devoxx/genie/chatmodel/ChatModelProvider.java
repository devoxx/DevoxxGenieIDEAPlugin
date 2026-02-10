package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.devoxx.genie.chatmodel.ChatModelFactory.TEST_MODEL;

@Setter
public class ChatModelProvider {

    private static final ModelProvider DEFAULT_PROVIDER = ModelProvider.OpenAI;

    public ChatModel getChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        CustomChatModel customChatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createChatModel(customChatModel);
    }

    public StreamingChatModel getStreamingChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        CustomChatModel customChatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createStreamingChatModel(customChatModel);
    }

    private @NotNull ChatModelFactory getFactory(@NotNull ChatMessageContext chatMessageContext) {
        ModelProvider provider = Optional.ofNullable(chatMessageContext.getLanguageModel())
            .map(LanguageModel::getProvider)
            .orElse(DEFAULT_PROVIDER);

        return ChatModelFactoryProvider.getFactoryByProvider(provider.name())
            .orElseThrow(() -> new IllegalArgumentException("No factory for provider: " + provider));
    }

    public @NotNull CustomChatModel initChatModel(@NotNull ChatMessageContext chatMessageContext) {
        CustomChatModel customChatModel = new CustomChatModel();
        DevoxxGenieSettingsService stateService = DevoxxGenieStateService.getInstance();
        setMaxOutputTokens(stateService, customChatModel);

        customChatModel.setTemperature(stateService.getTemperature());
        customChatModel.setMaxRetries(stateService.getMaxRetries());
        customChatModel.setTopP(stateService.getTopP());
        customChatModel.setTimeout(stateService.getTimeout());
        customChatModel.setProject(chatMessageContext.getProject());

        LanguageModel languageModel = chatMessageContext.getLanguageModel();
        customChatModel.setModelName(languageModel.getModelName() == null ? TEST_MODEL : languageModel.getModelName());

        // Set context window if available (for providers like Ollama that support it)
        if (languageModel.getInputMaxTokens() > 0) {
            customChatModel.setContextWindow(languageModel.getInputMaxTokens());
        }

        setLocalBaseUrl(languageModel, customChatModel, stateService);

        return customChatModel;
    }

    private void setLocalBaseUrl(@NotNull LanguageModel languageModel,
                                 CustomChatModel customChatModel,
                                 DevoxxGenieSettingsService stateService) {
        // Set base URL for local providers
        switch (languageModel.getProvider()) {
            case LMStudio:
                customChatModel.setBaseUrl(stateService.getLmstudioModelUrl());
                break;
            case Ollama:
                customChatModel.setBaseUrl(stateService.getOllamaModelUrl());
                break;
            case GPT4All:
                customChatModel.setBaseUrl(stateService.getGpt4allModelUrl());
                break;
            case LLaMA:
                customChatModel.setBaseUrl(stateService.getLlamaCPPUrl());
                break;
            case CustomOpenAI:
                customChatModel.setBaseUrl(stateService.getCustomOpenAIUrl());
                break;
            // Add other local providers as needed
        }
    }

    private static void setMaxOutputTokens(@NotNull DevoxxGenieSettingsService settingsState,
                                           @NotNull CustomChatModel customChatModel) {
        Integer maxOutputTokens = settingsState.getMaxOutputTokens();
        customChatModel.setMaxTokens(maxOutputTokens != null ? maxOutputTokens : Constant.MAX_OUTPUT_TOKENS);
    }
}
