package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/** Marks compaction for background execution through the normal prompt lifecycle. */
public final class CompactCommand implements PromptCommand {
    @Override
    public boolean matches(@NotNull String prompt) {
        return "/compact".equalsIgnoreCase(prompt.trim());
    }

    @Override
    public Optional<String> process(@NotNull ChatMessageContext context, @NotNull PromptOutputPanel panel) {
        context.setCommandName("compact");
        return Optional.of(context.getUserPrompt());
    }
}
