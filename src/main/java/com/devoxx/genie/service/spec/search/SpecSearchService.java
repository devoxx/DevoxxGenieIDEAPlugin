package com.devoxx.genie.service.spec.search;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Finds semantically related task specs using BM25 ranking over the in-memory spec cache.
 * <p>
 * This service builds a searchable text representation of each spec (title + description +
 * labels + acceptance criteria) and uses BM25 to rank specs against a query derived from
 * a source task or free-text query.
 * <p>
 * The index is rebuilt on each search call to ensure freshness against the live SpecService cache.
 * This is efficient because the number of specs in a typical backlog is small (tens to low hundreds).
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

    private @NotNull List<ScoredSpec> rankSpecs(@NotNull String query,
                                                 @NotNull List<TaskSpec> specs,
                                                 String excludeId,
                                                 int limit) {
        if (specs.isEmpty() || query.isBlank()) {
            return Collections.emptyList();
        }

        BM25SearchEngine engine = new BM25SearchEngine();
        Map<String, TaskSpec> specById = new HashMap<>();

        for (TaskSpec spec : specs) {
            if (spec.getId() == null) continue;
            if (excludeId != null && excludeId.equalsIgnoreCase(spec.getId())) continue;

            String docText = buildSearchPayload(spec);
            engine.index(spec.getId(), docText);
            specById.put(spec.getId(), spec);
        }

        List<BM25SearchEngine.ScoredResult> results = engine.search(query, limit);

        return results.stream()
                .map(r -> new ScoredSpec(specById.get(r.docId()), r.score()))
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
