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

        Set<String> selectedIds = buildSelectedIds(tasks);
        Map<String, TaskSpec> selectedById = buildSelectedById(tasks);

        Map<String, Set<String>> dependents = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        initializeGraphNodes(selectedIds, dependents, inDegree);
        addInternalEdges(tasks, selectedIds, dependents, inDegree);

        Queue<String> queue = buildInitialQueue(inDegree);
        List<TaskSpec> sorted = new ArrayList<>();

        while (!queue.isEmpty()) {
            List<String> layer = drainQueue(queue);
            layer.sort(layerComparator(selectedById));
            for (String id : layer) {
                TaskSpec spec = selectedById.get(id);
                if (spec != null) {
                    sorted.add(spec);
                }
                processLayerDependents(id, dependents, inDegree, queue);
            }
        }

        checkForCycles(sorted.size(), selectedIds, selectedById, inDegree);
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

        Map<String, TaskSpec> allById = buildAllById(allSpecs);

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

    /**
     * Sort the given tasks into topological layers using Kahn's algorithm.
     * Each layer contains tasks whose dependencies are all in prior layers (or
     * are external and already "Done"). Tasks within a layer are independent
     * of each other and may safely execute in parallel.
     *
     * @param tasks    the selected tasks to sort
     * @param allSpecs all known specs (for checking external dependency status)
     * @return list of layers; each layer is a list of tasks that can run concurrently
     * @throws CircularDependencyException if a cycle is detected
     */
    public static @NotNull List<List<TaskSpec>> sortByLayers(@NotNull List<TaskSpec> tasks,
                                                              @NotNull List<TaskSpec> allSpecs)
            throws CircularDependencyException {

        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> selectedIds = buildSelectedIds(tasks);
        Map<String, TaskSpec> selectedById = buildSelectedById(tasks);

        Map<String, Set<String>> dependents = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        initializeGraphNodes(selectedIds, dependents, inDegree);
        addInternalEdges(tasks, selectedIds, dependents, inDegree);

        Queue<String> queue = buildInitialQueue(inDegree);
        List<List<TaskSpec>> layers = new ArrayList<>();
        int processedCount = 0;

        while (!queue.isEmpty()) {
            List<String> layerIds = drainQueue(queue);
            layerIds.sort(layerComparator(selectedById));

            List<TaskSpec> layer = new ArrayList<>();
            for (String id : layerIds) {
                TaskSpec spec = selectedById.get(id);
                if (spec != null) {
                    layer.add(spec);
                    processedCount++;
                }
                processLayerDependents(id, dependents, inDegree, queue);
            }

            if (!layer.isEmpty()) {
                layers.add(layer);
            }
        }

        checkForCycles(processedCount, selectedIds, selectedById, inDegree);
        return layers;
    }

    // ---- Private helpers ----

    private static @NotNull Set<String> buildSelectedIds(@NotNull List<TaskSpec> tasks) {
        return tasks.stream()
                .map(TaskSpec::getId)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private static @NotNull Map<String, TaskSpec> buildSelectedById(@NotNull List<TaskSpec> tasks) {
        Map<String, TaskSpec> map = new LinkedHashMap<>();
        for (TaskSpec t : tasks) {
            if (t.getId() != null) {
                map.put(t.getId().toLowerCase(), t);
            }
        }
        return map;
    }

    private static @NotNull Map<String, TaskSpec> buildAllById(@NotNull List<TaskSpec> allSpecs) {
        Map<String, TaskSpec> map = new HashMap<>();
        for (TaskSpec s : allSpecs) {
            if (s.getId() != null) {
                map.put(s.getId().toLowerCase(), s);
            }
        }
        return map;
    }

    private static void initializeGraphNodes(@NotNull Set<String> ids,
                                              @NotNull Map<String, Set<String>> dependents,
                                              @NotNull Map<String, Integer> inDegree) {
        for (String id : ids) {
            inDegree.put(id, 0);
            dependents.put(id, new HashSet<>());
        }
    }

    private static void addInternalEdges(@NotNull List<TaskSpec> tasks,
                                          @NotNull Set<String> selectedIds,
                                          @NotNull Map<String, Set<String>> dependents,
                                          @NotNull Map<String, Integer> inDegree) {
        for (TaskSpec task : tasks) {
            if (task.getId() == null) continue;
            String taskId = task.getId().toLowerCase();
            List<String> deps = task.getDependencies();
            if (deps == null) continue;
            for (String dep : deps) {
                String depLower = dep.toLowerCase();
                if (selectedIds.contains(depLower)) {
                    dependents.computeIfAbsent(depLower, k -> new HashSet<>()).add(taskId);
                    inDegree.merge(taskId, 1, Integer::sum);
                }
            }
        }
    }

    private static @NotNull Queue<String> buildInitialQueue(@NotNull Map<String, Integer> inDegree) {
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        return queue;
    }

    private static @NotNull List<String> drainQueue(@NotNull Queue<String> queue) {
        List<String> snapshot = new ArrayList<>(queue);
        queue.clear();
        return snapshot;
    }

    private static @NotNull Comparator<String> layerComparator(@NotNull Map<String, TaskSpec> selectedById) {
        return (a, b) -> {
            TaskSpec ta = selectedById.get(a);
            TaskSpec tb = selectedById.get(b);
            int cmp = Integer.compare(
                    ta != null ? ta.getOrdinal() : 1000,
                    tb != null ? tb.getOrdinal() : 1000);
            if (cmp != 0) return cmp;
            return Integer.compare(extractNumber(a), extractNumber(b));
        };
    }

    private static void processLayerDependents(@NotNull String id,
                                                @NotNull Map<String, Set<String>> dependents,
                                                @NotNull Map<String, Integer> inDegree,
                                                @NotNull Queue<String> queue) {
        for (String dependent : dependents.getOrDefault(id, Collections.emptySet())) {
            int newDegree = inDegree.merge(dependent, -1, Integer::sum);
            if (newDegree == 0) {
                queue.add(dependent);
            }
        }
    }

    private static void checkForCycles(int processedCount,
                                        @NotNull Set<String> selectedIds,
                                        @NotNull Map<String, TaskSpec> selectedById,
                                        @NotNull Map<String, Integer> inDegree)
            throws CircularDependencyException {
        if (processedCount < selectedIds.size()) {
            List<String> cycleIds = selectedIds.stream()
                    .filter(id -> inDegree.getOrDefault(id, 0) > 0)
                    .map(id -> resolveTaskId(selectedById, id))
                    .toList();
            throw new CircularDependencyException(cycleIds);
        }
    }

    private static @NotNull String resolveTaskId(@NotNull Map<String, TaskSpec> selectedById,
                                                  @NotNull String id) {
        TaskSpec t = selectedById.get(id);
        return t != null && t.getId() != null ? t.getId() : id;
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
