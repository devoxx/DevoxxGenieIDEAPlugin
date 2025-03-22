package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.devoxx.genie.model.Constant.COMMAND_PREFIX;
import static com.devoxx.genie.model.Constant.FIND_COMMAND;

/**
 * Command processor for the /find command.
 */
public class FindCommand implements PromptCommand {

    @Override
    public boolean matches(@NotNull String prompt) {
        return prompt.trim().startsWith(COMMAND_PREFIX + FIND_COMMAND);
    }

    @Override
    public Optional<String> process(@NotNull ChatMessageContext chatMessageContext, 
                                  @NotNull PromptOutputPanel promptOutputPanel) {
        // Check if RAG is enabled in settings
        if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getRagEnabled())) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                    "The /find command requires RAG to be enabled in settings");
            return Optional.empty();
        }

        // Check if RAG is activated
        if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getRagActivated())) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                    "The /find command requires RAG to be turned on");
            return Optional.empty();
        }

        // Set command name and extract search query
        chatMessageContext.setCommandName(FIND_COMMAND);
        String searchQuery = chatMessageContext.getUserPrompt().substring(COMMAND_PREFIX.length() + FIND_COMMAND.length()).trim();
        
        return Optional.of(searchQuery);
    }
}
