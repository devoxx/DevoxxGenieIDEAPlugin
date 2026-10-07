package com.devoxx.genie.service.prompt.websearch.cli;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import java.time.Duration;
import java.util.Locale;

/**
 * Standalone diagnostic CLI for the web search integration. Exercises the same Tavily/Google +
 * LLM pipeline that the plugin uses, but bypasses the IntelliJ service layer so it can run from
 * {@code ./gradlew webSearch}.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew webSearch --args="tavily 'latest news on Iran'"
 *   ./gradlew webSearch --args="google 'IntelliJ IDEA 2025 release'"
 * </pre>
 *
 * <p>LLM selection via {@code LLM_PROVIDER} env var (default: {@code ollama}):
 * <ul>
 *   <li>{@code ollama}  — local Ollama instance (no API key needed)</li>
 *   <li>{@code openai}  — OpenAI cloud API</li>
 * </ul>
 *
 * <p>Ollama env vars (when LLM_PROVIDER=ollama or unset):
 * <ul>
 *   <li>{@code OLLAMA_BASE_URL} — base URL (default {@code http://localhost:11434})</li>
 *   <li>{@code OLLAMA_MODEL}    — model name (default {@code qwen3.6:35b-mlx})</li>
 * </ul>
 *
 * <p>OpenAI env vars (when LLM_PROVIDER=openai):
 * <ul>
 *   <li>{@code OPENAI_API_KEY} — required</li>
 *   <li>{@code OPENAI_MODEL}   — model name (default {@code gpt-4o-mini})</li>
 * </ul>
 *
 * <p>Web-search env vars:
 * <ul>
 *   <li>{@code TAVILY_API_KEY}              — required for Tavily</li>
 *   <li>{@code GOOGLE_API_KEY + GOOGLE_CSI_KEY} — required for Google</li>
 * </ul>
 */
public final class WebSearchCli {

    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final String DEFAULT_OLLAMA_URL   = "http://localhost:11434";
    private static final String DEFAULT_OLLAMA_MODEL = "qwen3.6:35b-mlx";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";

    private WebSearchCli() {}

    interface SearchWebsite {
        @SystemMessage("""
                Provide a paragraph-long answer, not a long step by step explanation.
                Reply with "I don't know the answer" if the provided information isn't relevant.
            """)
        String search(String query);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            System.exit(2);
        }

        String searchProvider = args[0].toLowerCase(Locale.ROOT);
        String query          = args[1];
        int maxResults        = args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_MAX_RESULTS;

        System.out.printf("Query      : %s%n", query);
        System.out.printf("MaxResults : %d%n", maxResults);

        WebSearchEngine engine = buildSearchEngine(searchProvider);

        ContentRetriever contentRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(engine)
                .maxResults(maxResults)
                .build();

        ChatModel chatModel = buildChatModel();

        SearchWebsite service = AiServices.builder(SearchWebsite.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        System.out.println("\nSearching...");
        long start = System.currentTimeMillis();
        String answer = service.search(query);
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("%nAnswer (%dms):%n%s%n", elapsed, answer);
    }

    private static WebSearchEngine buildSearchEngine(String provider) {
        return switch (provider) {
            case "tavily" -> {
                String key = requireEnv("TAVILY_API_KEY");
                System.out.println("Search     : Tavily");
                yield TavilyWebSearchEngine.builder()
                        .apiKey(key)
                        .build();
            }
            case "google" -> {
                String apiKey = requireEnv("GOOGLE_API_KEY");
                String csiKey = requireEnv("GOOGLE_CSI_KEY");
                System.out.println("Search     : Google Custom Search");
                yield GoogleCustomWebSearchEngine.builder()
                        .apiKey(apiKey)
                        .csi(csiKey)
                        .build();
            }
            default -> {
                System.err.println("Unknown search provider: " + provider + " (use 'tavily' or 'google')");
                printUsage();
                System.exit(2);
                yield null;
            }
        };
    }

    private static ChatModel buildChatModel() {
        String llmProvider = env("LLM_PROVIDER", "ollama").toLowerCase(Locale.ROOT);
        return switch (llmProvider) {
            case "ollama" -> buildOllamaModel();
            case "openai" -> buildOpenAiModel();
            default -> {
                System.err.println("Unknown LLM_PROVIDER: " + llmProvider + " (use 'ollama' or 'openai')");
                System.exit(1);
                yield null;
            }
        };
    }

    private static ChatModel buildOllamaModel() {
        String baseUrl = env("OLLAMA_BASE_URL", DEFAULT_OLLAMA_URL);
        String model   = env("OLLAMA_MODEL",    DEFAULT_OLLAMA_MODEL);
        System.out.printf("LLM        : Ollama %s @ %s%n", model, baseUrl);
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    private static ChatModel buildOpenAiModel() {
        String key   = requireEnv("OPENAI_API_KEY");
        String model = env("OPENAI_MODEL", DEFAULT_OPENAI_MODEL);
        System.out.printf("LLM        : OpenAI %s%n", model);
        return OpenAiChatModel.builder()
                .apiKey(key)
                .modelName(model)
                .build();
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            System.err.println("Missing required env var: " + key);
            System.exit(1);
        }
        return value;
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static void printUsage() {
        System.err.println("""
                Usage:
                  ./gradlew webSearch --args="<search-provider> '<query>' [maxResults=5]"

                  search-provider: tavily | google

                Examples:
                  ./gradlew webSearch --args="tavily 'latest news on Iran'"
                  OLLAMA_MODEL=qwen3.6:35b-mlx ./gradlew webSearch --args="tavily 'latest news on Iran'"
                  LLM_PROVIDER=openai OPENAI_API_KEY=xxx ./gradlew webSearch --args="google 'IntelliJ 2025' 10"

                LLM selection (LLM_PROVIDER env var, default: ollama):
                  ollama  — OLLAMA_BASE_URL (default http://localhost:11434), OLLAMA_MODEL (default qwen3.6:35b-mlx)
                  openai  — OPENAI_API_KEY (required), OPENAI_MODEL (default gpt-4o-mini)

                Search provider env vars:
                  TAVILY_API_KEY                    (for tavily)
                  GOOGLE_API_KEY + GOOGLE_CSI_KEY   (for google)
                """);
    }
}
