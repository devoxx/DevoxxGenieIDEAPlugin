package com.devoxx.genie.service.prompt.websearch;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.service.websearch.WebSearchNotConfiguredException;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.service.websearch.WebSearchSources;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Runs {@code /search}: one search with the selected provider, an LLM answer grounded in those results, and
 * a "Sources" list linking the pages the answer was based on (TASK-277.2).
 */
@Slf4j
public class WebSearchPromptExecutionService {

    static final String SYSTEM_PROMPT = """
            Provide a paragraph-long answer, not a long step by step explanation.
            Base the answer only on the web search results provided.
            Reply with "I don't know the answer" if the provided information isn't relevant.
            """;

    static final int MAX_SNIPPET_CHARS = 1000;

    // Package-private to allow injection in tests
    ChatModelProvider chatModelProvider = new ChatModelProvider();

    public static WebSearchPromptExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(WebSearchPromptExecutionService.class);
    }

    /**
     * Search the web for the given query and answer from the results.
     *
     * @param chatMessageContext the chat message context
     * @return the answer followed by its sources, or a "no results" message (no LLM call) when the
     *         provider found nothing
     * @throws WebSearchNotConfiguredException when no provider is selected or its credentials are
     *                                         missing; nothing is sent to any provider in that case
     */
    public @NotNull Optional<AiMessage> searchWeb(@NotNull ChatMessageContext chatMessageContext) {
        String query = chatMessageContext.getUserPrompt();
        log.debug("Searching the web for: {}", query);

        WebSearchConfig config = WebSearchConfig.fromState(DevoxxGenieStateService.getInstance());
        List<WebSearchOrganicResult> results = WebSearchService.search(config, query).results();
        if (results == null || results.isEmpty()) {
            return Optional.of(AiMessage.from(WebSearchSources.noResultsMessage(query)));
        }
        List<WebSearchOrganicResult> used = results.subList(0, Math.min(config.maxResults(), results.size()));

        String answer = chatModel(chatMessageContext).chat(ChatRequest.builder()
                        .messages(SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(groundedPrompt(query, used)))
                        .build())
                .aiMessage()
                .text();

        String sources = WebSearchSources.toMarkdown(used);
        String text = answer == null ? "" : answer.strip();
        return Optional.of(AiMessage.from(sources.isEmpty() ? text : text + "\n\n" + sources));
    }

    /**
     * AiServices-free call needs a non-streaming chatModel. In streaming mode only streamingChatModel is
     * set on the context, so create a non-streaming model from the same provider/settings on demand.
     */
    private @NotNull ChatModel chatModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = chatMessageContext.getChatModel();
        return chatModel != null ? chatModel : chatModelProvider.getChatLanguageModel(chatMessageContext);
    }

    /**
     * The question plus the numbered results the answer must be based on. An instance method on purpose:
     * tests that mockStatic this class (for getInstance) would otherwise stub it to null.
     */
    private @NotNull String groundedPrompt(@NotNull String query, @NotNull List<WebSearchOrganicResult> results) {
        StringBuilder prompt = new StringBuilder("Web search results:\n\n");
        int i = 1;
        for (WebSearchOrganicResult result : results) {
            prompt.append('[').append(i++).append("] ")
                    .append(result.title() == null ? "(no title)" : result.title()).append('\n');
            if (result.url() != null) {
                prompt.append("URL: ").append(result.url()).append('\n');
            }
            String snippet = result.content() != null && !result.content().isBlank() ? result.content() : result.snippet();
            if (snippet != null && !snippet.isBlank()) {
                prompt.append(snippet, 0, Math.min(snippet.length(), MAX_SNIPPET_CHARS)).append('\n');
            }
            prompt.append('\n');
        }
        return prompt.append("Question: ").append(query).toString();
    }
}
