package com.devoxx.genie.service.websearch;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WebSearchService {

    private static final Logger LOG = Logger.getInstance(WebSearchService.class);

    public static WebSearchService getInstance() {
        return ApplicationManager.getApplication().getService(WebSearchService.class);
    }

    interface SearchWebsite {
        @SystemMessage("""
                Provide a paragraph-long answer, not a long step by step explanation.
                Reply with "I don't know the answer" if the provided information isn't relevant.
            """)
        String search(String query);
    }

    /**
     * Search the web for the given query.
     *
     * @param chatMessageContext the chat message context
     * @return the AI message
     */
    public @NotNull Optional<AiMessage> searchWeb(@NotNull ChatMessageContext chatMessageContext) {
        LOG.debug("Searching the web for: " + chatMessageContext.getUserPrompt());

        WebSearchEngine engine = createWebSearchEngine();

        return Optional.ofNullable(engine)
            .flatMap(webSearchEngine -> executeSearchCommand(webSearchEngine, chatMessageContext));
    }

    /**
     * Execute the search command.
     *
     * @param webSearchEngine    the web search engine
     * @param chatMessageContext the chat message context
     * @return the AI message
     */
    private @NotNull Optional<AiMessage> executeSearchCommand(WebSearchEngine webSearchEngine,
                                                              @NotNull ChatMessageContext chatMessageContext) {
        LOG.debug("Executing search command for: " + chatMessageContext.getUserPrompt());

        ContentRetriever contentRetriever = WebSearchContentRetriever.builder()
            .webSearchEngine(webSearchEngine)
            .maxResults(DevoxxGenieStateService.getInstance().getMaxSearchResults())
            .build();

        SearchWebsite website = AiServices.builder(SearchWebsite.class)
            .chatLanguageModel(chatMessageContext.getChatLanguageModel())
            .contentRetriever(contentRetriever)
            .build();

        return Optional.of(new AiMessage(website.search(chatMessageContext.getUserPrompt())));
    }

    /**
     * Get the web search engine.
     * @return the web search engine
     */
    private @Nullable WebSearchEngine createWebSearchEngine() {
        LOG.debug("Creating web search engine");
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        if (stateService.isTavilySearchEnabled()) {
            return TavilyWebSearchEngine.builder()
                .apiKey(stateService.getTavilySearchKey())
                .build();
        } else if (stateService.isGoogleSearchEnabled() &&
                stateService.getGoogleSearchKey() != null &&
                stateService.getGoogleCSIKey() != null) {
            return GoogleCustomWebSearchEngine.builder()
                .apiKey(stateService.getGoogleSearchKey())
                .csi(stateService.getGoogleCSIKey())
                .build();
        }
        LOG.info("Web search engine not found or all disabled");
        return null;
    }
}
