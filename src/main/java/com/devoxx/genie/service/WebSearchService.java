package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;

import com.devoxx.genie.service.settings.SettingsStateService;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.devoxx.genie.model.Constant.GOOGLE_SEARCH_ACTION;
import static com.devoxx.genie.model.Constant.TAVILY_SEARCH_ACTION;

public class WebSearchService {

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
     * @param chatMessageContext the chat message context
     * @return the AI message
     */
    public @NotNull Optional<AiMessage> searchWeb(@NotNull ChatMessageContext chatMessageContext) {
        return getWebSearchEngine(chatMessageContext)
            .flatMap(webSearchEngine -> executeSearchCommand(webSearchEngine, chatMessageContext));
    }

    /**
     * Execute the search command.
     * @param webSearchEngine the web search engine
     * @param chatMessageContext the chat message context
     * @return the AI message
     */
    private @NotNull Optional<AiMessage> executeSearchCommand(WebSearchEngine webSearchEngine,
                                                              @NotNull ChatMessageContext chatMessageContext) {
        ContentRetriever contentRetriever = WebSearchContentRetriever.builder()
            .webSearchEngine(webSearchEngine)
            .maxResults(3)      // TODO Move value to Settings page
            .build();

        SearchWebsite website = AiServices.builder(SearchWebsite.class)
            .chatLanguageModel(chatMessageContext.getChatLanguageModel())
            .contentRetriever(contentRetriever)
            .build();

        return Optional.of(new AiMessage(website.search(chatMessageContext.getUserPrompt())));
    }

    /**
     * Get the web search engine.
     * @param chatMessageContext the chat message context
     * @return the web search engine
     */
    private static Optional<WebSearchEngine> getWebSearchEngine(@NotNull ChatMessageContext chatMessageContext) {
        if (chatMessageContext.getContext().equals(TAVILY_SEARCH_ACTION) &&
            SettingsStateService.getInstance().getTavilySearchKey() != null) {
            return Optional.of(TavilyWebSearchEngine.builder()
                .apiKey(SettingsStateService.getInstance().getTavilySearchKey())
                .build());
        } else if (SettingsStateService.getInstance().getGoogleSearchKey() != null &&
                   chatMessageContext.getContext().equals(GOOGLE_SEARCH_ACTION)) {
            return Optional.of(GoogleCustomWebSearchEngine.builder()
                .apiKey(SettingsStateService.getInstance().getGoogleSearchKey())
                .csi(SettingsStateService.getInstance().getGoogleCSIKey())
                .build());
        } else {
            return Optional.empty();
        }
    }
}
