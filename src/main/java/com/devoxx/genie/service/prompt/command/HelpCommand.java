package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.devoxx.genie.model.Constant.COMMAND_PREFIX;
import static com.devoxx.genie.model.Constant.HELP_COMMAND;

/**
 * Command processor for the /help command.
 */
public class HelpCommand implements PromptCommand {

    @Override
    public boolean matches(@NotNull String prompt) {
        return prompt.trim().startsWith(COMMAND_PREFIX + HELP_COMMAND);
    }

    @Override
    public Optional<String> process(@NotNull ChatMessageContext chatMessageContext, 
                                  @NotNull PromptOutputPanel promptOutputPanel) {
        // Display help text in the output panel
        promptOutputPanel.showHelpText();
        
        // Empty Optional means we don't continue with execution
        return Optional.empty();
    }
}
