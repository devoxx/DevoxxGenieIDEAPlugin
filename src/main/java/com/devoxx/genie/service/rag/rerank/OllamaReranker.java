package com.devoxx.genie.service.rag.rerank;

import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

/**
 * Reranker backed by an Ollama-hosted <em>generative</em> chat model.
 *
 * <p><strong>Why a generative model and not a cross-encoder?</strong> At the time of writing,
 * Ollama does not expose a dedicated rerank endpoint (no {@code /api/rerank} or cross-encoder
 * primitive). True cross-encoder reranker models such as {@code bge-reranker} are served
 * through {@code /api/embeddings} as scoring models and cannot be invoked through
 * {@code /api/generate}. This implementation therefore uses <em>chat-completion scoring</em>
 * against {@code /api/generate}: for each candidate chunk, a small generative model is asked
 * to assign an integer relevance score 0–10 against the query, and the scores are normalized
 * to [0.0, 1.0] before reordering.
 *
 * <p>For best results pick a small instruction-tuned generative model — the defaults the UI
 * suggests ({@code llama3.2:1b}, {@code qwen2.5:0.5b}) are fast enough to score 30 candidates
 * within the default 2000 ms budget on modest hardware. Reranker-style models like
 * {@code bge-reranker} <strong>will not work</strong> with this implementation because
 * {@code /api/generate} returns empty/malformed responses for them.
 *
 * <p>Candidates are scored in parallel on a shared executor (one worker per CPU core, capped
 * at 8). When a dedicated rerank endpoint becomes available, only {@link #scoreCandidate}
 * needs to change.
 */
@Slf4j
public class OllamaReranker implements Reranker {

    /** Max workers used to score candidates in parallel; chosen to be Ollama-friendly. */
    private static final int MAX_PARALLELISM = Math.min(8, Math.max(2, Runtime.getRuntime().availableProcessors()));

    /**
     * Captures an optional minus sign followed by digits so that "-3" parses as -3 (and is
     * then clamped to 0) instead of silently becoming +3. See task-214 review item M4.
     */
    private static final Pattern SCORE_PATTERN = Pattern.compile("(-?\\d+)");

    /**
     * How many leading characters of the chunk to send to the reranker. Smaller values let
     * the call comfortably score a 30-candidate shortlist inside the default 2000 ms budget;
     * users with large shortlists or slow hardware should either lower this or raise
     * {@code rerankerTimeoutMs} in the RAG settings.
     */
    private static final int CONTENT_PREVIEW_CHARS = 500;

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Gson GSON = new Gson();

    /**
     * Shared executor reused across calls to avoid spawning a fresh pool every retrieval.
     * Threads are daemons so they never block JVM shutdown; idle workers are reaped after
     * 60 s so the pool is essentially free when reranking is disabled or unused. A bounded
     * LinkedBlockingQueue (rather than SynchronousQueue) lets bursty calls queue up to a
     * reasonable depth without rejection — important when multiple retrievals are inflight.
     */
    private static final ExecutorService POOL = new ThreadPoolExecutor(
            MAX_PARALLELISM, MAX_PARALLELISM,
            60L, TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(256),
            r -> {
                Thread t = new Thread(r, "devoxxgenie-reranker");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()) {{
        allowCoreThreadTimeOut(true);
    }};

    /**
     * Dedicated OkHttpClient with a short read timeout and <strong>no</strong> retry
     * interceptor. The shared {@code HttpClientProvider.getClient()} retries up to 3 times
     * with exponential backoff and uses a 30 s read timeout — those defaults would let a
     * single misbehaving candidate block well beyond the reranker's wall-clock budget, so we
     * cannot use the shared client here. See task-214 review item C1.
     */
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(2))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(2))
            .connectionPool(new ConnectionPool(8, 30, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false)
            .build();

    @Override
    public @NotNull List<SearchResult> rerank(@NotNull String query,
                                              @NotNull List<SearchResult> candidates,
                                              int topN,
                                              long timeoutMs) {
        if (candidates.isEmpty() || topN <= 0) {
            return List.of();
        }

        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);

        // Dispatch all candidate-scoring tasks up-front so they run in parallel on the
        // shared pool. Each future yields a SearchResult carrying the reranker annotations.
        AtomicInteger rank = new AtomicInteger(1);
        List<CompletableFuture<SearchResult>> futures = new ArrayList<>(candidates.size());
        for (SearchResult c : candidates) {
            int preRank = rank.getAndIncrement();
            futures.add(CompletableFuture.supplyAsync(() -> {
                double s = scoreCandidate(query, c);
                return c.withRerankerAnnotations(preRank, s);
            }, POOL));
        }

        // Collect results with a per-future deadline derived from the overall budget.
        // We track which candidates completed by their original retrieval index so that on
        // timeout we can mix the scored results with the unscored remainder in retrieval
        // order (partial-progress fallback — review item M3). A pure "throw away everything
        // on first timeout" policy discards work we already paid for.
        List<SearchResult> scored = new ArrayList<>(candidates.size());
        Set<Integer> completedRetrievalIndices = new HashSet<>();
        boolean timedOut = false;
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<SearchResult> f = futures.get(i);
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                timedOut = true;
                break;
            }
            try {
                SearchResult r = f.get(remainingNanos, TimeUnit.NANOSECONDS);
                scored.add(r);
                completedRetrievalIndices.add(i);
            } catch (TimeoutException te) {
                timedOut = true;
                break;
            } catch (ExecutionException ee) {
                // One candidate's scoring failed — drop it, the rest still rank.
                log.debug("Skipping a candidate that failed scoring: {}",
                        ee.getCause() == null ? ee.getMessage() : ee.getCause().getMessage());
                completedRetrievalIndices.add(i); // treat as "handled" so it's not back-filled
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Reranker interrupted; returning whatever was scored so far");
                timedOut = true;
                break;
            }
        }

        if (timedOut) {
            log.warn("Reranker exceeded {} ms timeout after scoring {}/{} candidates; "
                    + "blending scored results with the remainder in retrieval order",
                    timeoutMs, scored.size(), candidates.size());
            // Cancel any futures we never picked up so they don't keep firing /api/generate
            // requests after we've already returned the result list to the caller.
            for (int i = 0; i < futures.size(); i++) {
                if (!completedRetrievalIndices.contains(i)) {
                    futures.get(i).cancel(true);
                }
            }
        }

        // Sort whatever we managed to score by reranker score descending.
        scored.sort(Comparator.comparingDouble((SearchResult r) ->
                r.rerankerScore() != null ? r.rerankerScore() : 0.0).reversed());

        // If we timed out partway through, pad the result list with the unscored remainder
        // in retrieval order so we still fill topN with useful candidates rather than
        // returning a degraded shortlist.
        if (timedOut && scored.size() < topN) {
            for (int i = 0; i < candidates.size() && scored.size() < topN; i++) {
                if (!completedRetrievalIndices.contains(i)) {
                    scored.add(candidates.get(i));
                }
            }
        }

        int n = Math.min(topN, scored.size());
        return new ArrayList<>(scored.subList(0, n));
    }

    /**
     * Score one candidate against the query in [0.0, 1.0]. Override in tests / subclasses to
     * avoid hitting Ollama. The default implementation issues a single chat-completion call
     * to the configured Ollama instance.
     *
     * @return a score in [0.0, 1.0]; on any failure returns 0.0 so the candidate drops to the
     *         bottom of the ranking without taking the whole call down.
     */
    protected double scoreCandidate(@NotNull String query, @NotNull SearchResult candidate) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        if (state == null) {
            return 0.0;
        }
        String baseUrl = state.getOllamaModelUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            return 0.0;
        }
        String model = state.getRerankerModelName();
        if (model == null || model.isEmpty()) {
            return 0.0;
        }

        String content = candidate.content() != null ? candidate.content() : "";
        String preview = content.length() > CONTENT_PREVIEW_CHARS
                ? content.substring(0, CONTENT_PREVIEW_CHARS)
                : content;

        String prompt = """
                You are a code-search relevance grader.
                Score how relevant the CHUNK is to the QUERY on an integer scale from 0 to 10,
                where 0 means "completely unrelated" and 10 means "directly answers the query".
                Respond with ONLY the integer score on a single line — no explanation, no punctuation.

                QUERY:
                %s

                CHUNK:
                %s
                """.formatted(query, preview);

        try {
            String response = callOllamaGenerate(baseUrl, model, prompt);
            return normalizeScore(response);
        } catch (Exception e) {
            log.debug("Reranker scoring call failed for chunk {}: {}",
                    candidate.filePath(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Posts a single non-streaming request to Ollama's {@code /api/generate} endpoint and
     * returns the response text. Marked package-private so tests can substitute a fake
     * response. Uses the dedicated short-timeout client {@link #HTTP} — never the shared
     * {@code HttpClientProvider} client (which retries 3× with 30 s timeouts).
     */
    @Nullable
    String callOllamaGenerate(@NotNull String baseUrl, @NotNull String model, @NotNull String prompt)
            throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("prompt", prompt);
        body.addProperty("stream", false);

        Request request = new Request.Builder()
                .url(ensureEndsWithSlash(baseUrl) + "api/generate")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            ResponseBody respBody = response.body();
            if (respBody == null) {
                return null;
            }
            JsonObject json = GSON.fromJson(respBody.string(), JsonObject.class);
            if (json == null || !json.has("response")) {
                return null;
            }
            return json.get("response").getAsString();
        }
    }

    /**
     * Parse the first integer 0–10 from the model's response and normalize to [0.0, 1.0].
     * Negative integers and out-of-range integers are clamped to the valid range; anything
     * we can't parse becomes 0.0.
     */
    static double normalizeScore(@Nullable String response) {
        if (response == null) return 0.0;
        Matcher m = SCORE_PATTERN.matcher(response);
        if (!m.find()) return 0.0;
        try {
            int raw = Integer.parseInt(m.group(1));
            int clamped = Math.max(0, Math.min(10, raw));
            return clamped / 10.0;
        } catch (NumberFormatException nfe) {
            return 0.0;
        }
    }
}
