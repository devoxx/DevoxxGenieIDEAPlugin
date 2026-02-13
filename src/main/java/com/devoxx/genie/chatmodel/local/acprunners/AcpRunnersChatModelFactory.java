package com.devoxx.genie.chatmodel.local.acprunners;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.spec.AcpToolConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;

/**
 * ChatModelFactory for ACP Runners provider.
 * Returns enabled ACP tools as LanguageModel entries.
 * ACP Runners bypass Langchain4J entirely — createChatModel/createStreamingChatModel return null.
 */
public class AcpRunnersChatModelFactory implements ChatModelFactory {

    @Override
    public ChatModel createChatModel(CustomChatModel customChatModel) {
        return null;
    }

    @Override
    public List<LanguageModel> getModels() {
        return DevoxxGenieStateService.getInstance().getAcpTools().stream()
                .filter(AcpToolConfig::isEnabled)
                .map(tool -> LanguageModel.builder()
                        .provider(ModelProvider.ACPRunners)
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
