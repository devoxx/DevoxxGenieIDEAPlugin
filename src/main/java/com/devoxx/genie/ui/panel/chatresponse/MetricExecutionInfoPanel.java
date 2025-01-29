package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.ui.JBColor;
import com.knuddels.jtokkit.api.Encoding;
import dev.langchain4j.model.output.TokenUsage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class MetricExecutionInfoPanel extends JPanel {

    public MetricExecutionInfoPanel(@NotNull ChatMessageContext chatMessageContext) {
        setLayout(new FlowLayout(FlowLayout.LEFT));

        String metricInfoLabel = String.format("ϟ %.2fs", chatMessageContext.getExecutionTimeMs() / 1000.0);
        TokenUsage tokenUsage = chatMessageContext.getTokenUsage();

        if (tokenUsage != null) {
            String cost = "";
            if (DefaultLLMSettingsUtil.isApiKeyBasedProvider(chatMessageContext.getLanguageModel().getProvider())) {
                cost = String.format("- %.5f $", chatMessageContext.getCost());
            }

            tokenUsage = calcOllamaInputTokenCount(chatMessageContext, tokenUsage);

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            String formattedInputTokens = numberFormat.format(tokenUsage.inputTokenCount());
            String formattedOutputTokens = numberFormat.format(tokenUsage.outputTokenCount());

            metricInfoLabel += String.format(" - Tokens ↑ %s ↓️ %s %s", formattedInputTokens, formattedOutputTokens, cost);
        }


        JLabel tokenLabel = new JLabel(metricInfoLabel);
        tokenLabel.setForeground(JBColor.GRAY);
        tokenLabel.setFont(tokenLabel.getFont().deriveFont(12f));
        add(tokenLabel);
    }

    private static TokenUsage calcOllamaInputTokenCount(@NotNull ChatMessageContext chatMessageContext, TokenUsage tokenUsage) {
        if (chatMessageContext.getLanguageModel().getProvider().equals(ModelProvider.Ollama)) {
            int inputContextTokens = 0;
            if (chatMessageContext.getFilesContext() != null) {
                Encoding encodingForProvider = ProjectContentService.getEncodingForProvider(chatMessageContext.getLanguageModel().getProvider());
                inputContextTokens = encodingForProvider.encode(chatMessageContext.getFilesContext()).size();
            }
            tokenUsage = new TokenUsage(tokenUsage.inputTokenCount() + inputContextTokens, tokenUsage.outputTokenCount());
        }
        return tokenUsage;
    }
}
