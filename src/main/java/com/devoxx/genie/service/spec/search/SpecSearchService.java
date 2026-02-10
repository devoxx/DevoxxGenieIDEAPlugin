package com.devoxx.genie.service.spec.search;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Finds semantically related task specs using a two-pass ranking strategy:
 * <ol>
 *   <li><b>BM25</b> — ranks by term frequency-inverse document frequency with length normalization.
 *       Catches exact and stemmed term overlap.</li>
 *   <li><b>Fuzzy fallback</b> — when BM25 produces fewer results than requested, a Levenshtein
 *       edit-distance scorer fills in matches where terminology differs slightly
 *       (e.g. "auth" &rarr; "authentication", typos).</li>
 * </ol>
 * <p>
 * Both engines build a searchable text representation of each spec (title + description +
 * labels + acceptance criteria) and the index is rebuilt on each search call to ensure
 * freshness against the live SpecService cache.  This is efficient because the number of
 * specs in a typical backlog is small (tens to low hundreds).
 */
@Slf4j
public class SpecSearchService {

    private final Project project;

    public SpecSearchService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Finds tasks related to the given task ID by building a query from the task's
     * title, description, and labels, then running BM25 against all other specs.
     *
     * @param taskId the source task ID (excluded from results)
     * @param limit  maximum number of related tasks to return
     * @return ranked list of related specs with scores, highest relevance first
     */
    public @NotNull List<ScoredSpec> findRelatedByTaskId(@NotNull String taskId, int limit) {
        SpecService specService = SpecService.getInstance(project);
        TaskSpec sourceSpec = specService.getSpec(taskId);
        if (sourceSpec == null) {
            return Collections.emptyList();
        }

        String query = buildSearchPayload(sourceSpec);
        List<TaskSpec> allSpecs = specService.getAllSpecs();

        return rankSpecs(query, allSpecs, taskId, limit);
    }

    /**
     * Finds tasks related to a free-text query using BM25 ranking.
     *
     * @param query the search query
     * @param limit maximum number of results
     * @return ranked list of matching specs with scores
     */
    public @NotNull List<ScoredSpec> findRelatedByQuery(@NotNull String query, int limit) {
        SpecService specService = SpecService.getInstance(project);
        List<TaskSpec> allSpecs = specService.getAllSpecs();

        return rankSpecs(query, allSpecs, null, limit);
    }

    /**
     * Weight applied to fuzzy scores when merging with BM25 scores.
     * Fuzzy matches are less precise, so they contribute less to the final ranking.
     */
    private static final double FUZZY_WEIGHT = 0.3;

    private @NotNull List<ScoredSpec> rankSpecs(@NotNull String query,
                                                 @NotNull List<TaskSpec> specs,
                                                 String excludeId,
                                                 int limit) {
        if (specs.isEmpty() || query.isBlank()) {
            return Collections.emptyList();
        }

        BM25SearchEngine bm25 = new BM25SearchEngine();
        FuzzySearchEngine fuzzy = new FuzzySearchEngine();
        Map<String, TaskSpec> specById = new HashMap<>();

        for (TaskSpec spec : specs) {
            if (spec.getId() == null) continue;
            if (excludeId != null && excludeId.equalsIgnoreCase(spec.getId())) continue;

            String docText = buildSearchPayload(spec);
            bm25.index(spec.getId(), docText);
            fuzzy.index(spec.getId(), docText);
            specById.put(spec.getId(), spec);
        }

        // Pass 1: BM25 exact-term ranking
        List<BM25SearchEngine.ScoredResult> bm25Results = bm25.search(query, limit);

        // If BM25 filled the requested limit, return those — no fallback needed
        if (bm25Results.size() >= limit) {
            return bm25Results.stream()
                    .map(r -> new ScoredSpec(specById.get(r.docId()), r.score()))
                    .filter(s -> s.spec() != null)
                    .collect(Collectors.toList());
        }

        // Pass 2: Fuzzy fallback — find additional candidates
        // Request more from fuzzy to have room after deduplication
        List<BM25SearchEngine.ScoredResult> fuzzyResults = fuzzy.search(query, limit * 2);

        // Merge: BM25 scores take priority, fuzzy fills gaps
        Map<String, Double> mergedScores = new LinkedHashMap<>();
        for (BM25SearchEngine.ScoredResult r : bm25Results) {
            mergedScores.put(r.docId(), r.score());
        }
        for (BM25SearchEngine.ScoredResult r : fuzzyResults) {
            mergedScores.merge(r.docId(), r.score() * FUZZY_WEIGHT, Double::sum);
        }

        return mergedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new ScoredSpec(specById.get(e.getKey()), e.getValue()))
                .filter(s -> s.spec() != null)
                .collect(Collectors.toList());
    }

    /**
     * Builds a searchable text payload from a task spec by concatenating
     * its key textual fields. This gives BM25 term overlap to work with.
     */
    static @NotNull String buildSearchPayload(@NotNull TaskSpec spec) {
        StringBuilder sb = new StringBuilder();

        if (spec.getTitle() != null) {
            // Title is weighted higher by repeating it
            sb.append(spec.getTitle()).append(" ");
            sb.append(spec.getTitle()).append(" ");
        }
        if (spec.getDescription() != null) {
            sb.append(spec.getDescription()).append(" ");
        }
        if (spec.getLabels() != null) {
            sb.append(String.join(" ", spec.getLabels())).append(" ");
        }
        if (spec.getAcceptanceCriteria() != null) {
            spec.getAcceptanceCriteria().forEach(ac -> {
                if (ac.getText() != null) {
                    sb.append(ac.getText()).append(" ");
                }
            });
        }
        if (spec.getImplementationPlan() != null) {
            sb.append(spec.getImplementationPlan()).append(" ");
        }
        if (spec.getMilestone() != null) {
            sb.append(spec.getMilestone()).append(" ");
        }

        return sb.toString().trim();
    }

    /**
     * A task spec paired with its relevance score.
     */
    public record ScoredSpec(@NotNull TaskSpec spec, double score) {
    }
}
