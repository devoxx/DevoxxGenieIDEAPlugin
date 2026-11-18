package com.devoxx.genie.service.prompt.websearch;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.service.websearch.WebSearchNotConfiguredException;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.web.search.WebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Slf4j
public class WebSearchPromptExecutionService {

    // Package-private to allow injection in tests
    ChatModelProvider chatModelProvider = new ChatModelProvider();

    public static WebSearchPromptExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(WebSearchPromptExecutionService.class);
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
     * @throws WebSearchNotConfiguredException when no provider is selected or its credentials are
     *                                         missing; nothing is sent to any provider in that case
     */
    public @NotNull Optional<AiMessage> searchWeb(@NotNull ChatMessageContext chatMessageContext) {
        log.debug("Searching the web for: " + chatMessageContext.getUserPrompt());

        WebSearchConfig config = WebSearchConfig.fromState(DevoxxGenieStateService.getInstance());
        WebSearchEngine engine = WebSearchService.createEngine(config);

        return executeSearchCommand(engine, config.maxResults(), chatMessageContext);
    }

    /**
     * Execute the search command.
     *
     * @param webSearchEngine    the web search engine
     * @param maxResults         maximum number of results to retrieve
     * @param chatMessageContext the chat message context
     * @return the AI message
     */
    private @NotNull Optional<AiMessage> executeSearchCommand(WebSearchEngine webSearchEngine,
                                                              int maxResults,
                                                              @NotNull ChatMessageContext chatMessageContext) {
        log.debug("Executing search command for: " + chatMessageContext.getUserPrompt());

        ContentRetriever contentRetriever = WebSearchContentRetriever.builder()
            .webSearchEngine(webSearchEngine)
            .maxResults(maxResults)
            .build();

        // AiServices with a synchronous String-returning method requires chatModel, not streamingChatModel.
        // In streaming mode only streamingChatModel is set on the context, so we create a non-streaming
        // model from the same provider/settings on demand.
        dev.langchain4j.model.chat.ChatModel chatModel = chatMessageContext.getChatModel();
        if (chatModel == null) {
            chatModel = chatModelProvider.getChatLanguageModel(chatMessageContext);
        }

        SearchWebsite website = AiServices.builder(SearchWebsite.class)
            .chatModel(chatModel)
            .contentRetriever(contentRetriever)
            .build();

        return Optional.of(new AiMessage(website.search(chatMessageContext.getUserPrompt())));
    }
}
