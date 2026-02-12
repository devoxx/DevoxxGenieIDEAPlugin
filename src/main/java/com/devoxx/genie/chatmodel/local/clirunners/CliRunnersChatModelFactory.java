package com.devoxx.genie.chatmodel.local.clirunners;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;

/**
 * ChatModelFactory for CLI Runners provider.
 * Returns enabled CLI tools as LanguageModel entries.
 * CLI Runners bypass Langchain4J entirely — createChatModel/createStreamingChatModel return null.
 */
public class CliRunnersChatModelFactory implements ChatModelFactory {

    @Override
    public ChatModel createChatModel(CustomChatModel customChatModel) {
        return null;
    }

    @Override
    public List<LanguageModel> getModels() {
        return DevoxxGenieStateService.getInstance().getCliTools().stream()
                .filter(CliToolConfig::isEnabled)
                .map(tool -> LanguageModel.builder()
                        .provider(ModelProvider.CLIRunners)
                        .modelName(tool.getName())
                        .displayName(tool.getName())
                        .apiKeyUsed(false)
                        .inputCost(0)
                        .outputCost(0)
                        .inputMaxTokens(0)
                        .outputMaxTokens(0)
                        .build())
                .toList();
    }
}
