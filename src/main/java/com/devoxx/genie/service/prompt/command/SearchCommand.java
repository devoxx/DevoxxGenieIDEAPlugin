package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.devoxx.genie.model.Constant.COMMAND_PREFIX;
import static com.devoxx.genie.model.Constant.SEARCH_COMMAND;

/**
 * Command processor for the /search command.
 * Triggers a web search using the configured Tavily or Google Custom Search engine.
 * Requires Web Search to be enabled and at least one provider configured in settings.
 */
public class SearchCommand implements PromptCommand {

    @Override
    public boolean matches(@NotNull String prompt) {
        return prompt.trim().startsWith(COMMAND_PREFIX + SEARCH_COMMAND);
    }

    @Override
    public Optional<String> process(@NotNull ChatMessageContext chatMessageContext,
                                    @NotNull PromptOutputPanel promptOutputPanel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        if (!isWebSearchConfigured(state)) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                    "The /search command requires Web Search to be enabled in Settings → Web search");
            return Optional.empty();
        }

        chatMessageContext.setCommandName(SEARCH_COMMAND);
        chatMessageContext.setWebSearchRequested(true);

        String searchQuery = chatMessageContext.getUserPrompt()
                .substring(COMMAND_PREFIX.length() + SEARCH_COMMAND.length()).trim();

        return Optional.of(searchQuery);
    }

    private boolean isWebSearchConfigured(@NotNull DevoxxGenieStateService state) {
        if (!Boolean.TRUE.equals(state.getIsWebSearchEnabled())) {
            return false;
        }
        boolean tavilyReady = state.isTavilySearchEnabled()
                && state.getTavilySearchKey() != null && !state.getTavilySearchKey().isBlank();
        boolean googleReady = state.isGoogleSearchEnabled()
                && state.getGoogleSearchKey() != null && !state.getGoogleSearchKey().isBlank()
                && state.getGoogleCSIKey() != null && !state.getGoogleCSIKey().isBlank();
        return tavilyReady || googleReady;
    }
}
