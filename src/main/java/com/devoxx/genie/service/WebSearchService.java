package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
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
import org.jetbrains.annotations.Nullable;

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
        WebSearchEngine engine = createWebSearchEngine(chatMessageContext.getContext());
        return Optional.ofNullable(engine)
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
            .maxResults(DevoxxGenieSettingsServiceProvider.getInstance().getMaxSearchResults())
            .build();

        SearchWebsite website = AiServices.builder(SearchWebsite.class)
            .chatLanguageModel(chatMessageContext.getChatLanguageModel())
            .contentRetriever(contentRetriever)
            .build();

        return Optional.of(new AiMessage(website.search(chatMessageContext.getUserPrompt())));
    }

    /**
     * Get the web search engine.
     * @param searchType the search type
     * @return the web search engine
     */
    private @Nullable WebSearchEngine createWebSearchEngine(@NotNull String searchType) {
        DevoxxGenieSettingsService settings = DevoxxGenieSettingsServiceProvider.getInstance();

        if (searchType.equals(TAVILY_SEARCH_ACTION) && settings.getTavilySearchKey() != null) {
            return TavilyWebSearchEngine.builder()
                .apiKey(settings.getTavilySearchKey())
                .build();
        } else if (searchType.equals(GOOGLE_SEARCH_ACTION) &&
            settings.getGoogleSearchKey() != null &&
            settings.getGoogleCSIKey() != null) {
            return GoogleCustomWebSearchEngine.builder()
                .apiKey(settings.getGoogleSearchKey())
                .csi(settings.getGoogleCSIKey())
                .build();
        }
        return null;
    }
}
