package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Topological sort utility for task specs using Kahn's algorithm (BFS).
 * Orders tasks so that dependencies are executed before dependents.
 */
public final class TaskDependencySorter {

    private TaskDependencySorter() {
    }

    /**
     * Sort the given tasks in dependency order using Kahn's algorithm.
     * Dependencies referencing tasks outside the selected set are checked against allSpecs:
     * if already "Done", they are considered satisfied; otherwise the dependent task will
     * have unsatisfied dependencies (tracked for runtime skipping).
     *
     * @param tasks    the selected tasks to sort
     * @param allSpecs all known specs (for checking external dependency status)
     * @return sorted list of tasks in execution order
     * @throws CircularDependencyException if a cycle is detected
     */
    public static @NotNull List<TaskSpec> sort(@NotNull List<TaskSpec> tasks,
                                                @NotNull List<TaskSpec> allSpecs)
            throws CircularDependencyException {

        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        // Build lookup maps
        Set<String> selectedIds = tasks.stream()
                .map(TaskSpec::getId)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Map<String, TaskSpec> selectedById = new LinkedHashMap<>();
        for (TaskSpec t : tasks) {
            if (t.getId() != null) {
                selectedById.put(t.getId().toLowerCase(), t);
            }
        }

        Map<String, TaskSpec> allById = new HashMap<>();
        for (TaskSpec s : allSpecs) {
            if (s.getId() != null) {
                allById.put(s.getId().toLowerCase(), s);
            }
        }

        // Build adjacency list and in-degree map (only within selected set)
        Map<String, Set<String>> dependents = new HashMap<>();  // dep -> tasks that depend on it
        Map<String, Integer> inDegree = new HashMap<>();

        for (String id : selectedIds) {
            inDegree.put(id, 0);
            dependents.put(id, new HashSet<>());
        }

        for (TaskSpec task : tasks) {
            if (task.getId() == null) continue;
            String taskId = task.getId().toLowerCase();
            List<String> deps = task.getDependencies();
            if (deps == null) continue;

            for (String dep : deps) {
                String depLower = dep.toLowerCase();
                if (selectedIds.contains(depLower)) {
                    // Internal dependency: add edge dep -> task
                    dependents.computeIfAbsent(depLower, k -> new HashSet<>()).add(taskId);
                    inDegree.merge(taskId, 1, Integer::sum);
                }
                // External dependencies are not edges in the graph;
                // they are checked at runtime by the runner
            }
        }

        // BFS (Kahn's algorithm)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<TaskSpec> sorted = new ArrayList<>();
        // Process layer by layer; within a layer, sort by ordinal then numeric ID
        while (!queue.isEmpty()) {
            List<String> layer = new ArrayList<>(queue);
            queue.clear();

            layer.sort((a, b) -> {
                TaskSpec ta = selectedById.get(a);
                TaskSpec tb = selectedById.get(b);
                int cmp = Integer.compare(
                        ta != null ? ta.getOrdinal() : 1000,
                        tb != null ? tb.getOrdinal() : 1000);
                if (cmp != 0) return cmp;
                return Integer.compare(extractNumber(a), extractNumber(b));
            });

            for (String id : layer) {
                TaskSpec spec = selectedById.get(id);
                if (spec != null) {
                    sorted.add(spec);
                }
                for (String dependent : dependents.getOrDefault(id, Collections.emptySet())) {
                    int newDegree = inDegree.merge(dependent, -1, Integer::sum);
                    if (newDegree == 0) {
                        queue.add(dependent);
                    }
                }
            }
        }

        // Check for cycle
        if (sorted.size() < selectedIds.size()) {
            List<String> cycleIds = selectedIds.stream()
                    .filter(id -> inDegree.getOrDefault(id, 0) > 0)
                    .map(id -> {
                        TaskSpec t = selectedById.get(id);
                        return t != null && t.getId() != null ? t.getId() : id;
                    })
                    .collect(Collectors.toList());
            throw new CircularDependencyException(cycleIds);
        }

        return sorted;
    }

    /**
     * Check whether all dependencies of a task are satisfied.
     * A dependency is satisfied if:
     * - It's in the completed set, OR
     * - It's not in the selected set but is "Done" in allSpecs
     *
     * @return list of unsatisfied dependency IDs (empty if all satisfied)
     */
    public static @NotNull List<String> getUnsatisfiedDependencies(@NotNull TaskSpec task,
                                                                    @NotNull Set<String> completedIds,
                                                                    @NotNull Set<String> selectedIds,
                                                                    @NotNull List<TaskSpec> allSpecs) {
        List<String> deps = task.getDependencies();
        if (deps == null || deps.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, TaskSpec> allById = new HashMap<>();
        for (TaskSpec s : allSpecs) {
            if (s.getId() != null) {
                allById.put(s.getId().toLowerCase(), s);
            }
        }

        List<String> unsatisfied = new ArrayList<>();
        for (String dep : deps) {
            String depLower = dep.toLowerCase();
            if (completedIds.contains(depLower)) {
                continue; // completed in this run
            }
            if (!selectedIds.contains(depLower)) {
                // External dep: check if Done
                TaskSpec ext = allById.get(depLower);
                if (ext != null && "Done".equalsIgnoreCase(ext.getStatus())) {
                    continue;
                }
            }
            unsatisfied.add(dep);
        }
        return unsatisfied;
    }

    private static int extractNumber(String id) {
        if (id == null) return Integer.MAX_VALUE;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(id);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return Integer.MAX_VALUE;
    }
}
