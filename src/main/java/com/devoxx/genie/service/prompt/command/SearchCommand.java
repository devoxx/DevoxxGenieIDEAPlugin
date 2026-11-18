package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.service.websearch.WebSearchReadiness;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.devoxx.genie.model.Constant.COMMAND_PREFIX;
import static com.devoxx.genie.model.Constant.SEARCH_COMMAND;

/**
 * Command processor for the /search command.
 * Triggers a web search with the provider selected in Settings → Web search.
 * Requires Web search to be enabled and the selected provider's credentials to be present.
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

        String problem = configurationProblem(state);
        if (problem != null) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(), problem);
            return Optional.empty();
        }

        chatMessageContext.setCommandName(SEARCH_COMMAND);
        chatMessageContext.setWebSearchRequested(true);

        String searchQuery = chatMessageContext.getUserPrompt()
                .substring(COMMAND_PREFIX.length() + SEARCH_COMMAND.length()).trim();

        return Optional.of(searchQuery);
    }

    /** Why /search cannot run, or null when it can. Uses the shared web search readiness rule. */
    private static @Nullable String configurationProblem(@NotNull DevoxxGenieStateService state) {
        if (!WebSearchService.isEnabled(state)) {
            return "The /search command requires Web search to be enabled in "
                    + WebSearchReadiness.SETTINGS_PATH + ".";
        }
        WebSearchReadiness readiness = WebSearchService.readiness(WebSearchConfig.fromState(state));
        return readiness.isReady() ? null : readiness.message();
    }
}
