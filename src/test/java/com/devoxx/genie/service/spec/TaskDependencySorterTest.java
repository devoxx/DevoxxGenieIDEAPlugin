package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class TaskDependencySorterTest {

    @Test
    void sortEmptyList() throws CircularDependencyException {
        List<TaskSpec> result = TaskDependencySorter.sort(Collections.emptyList(), Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    void sortSingleTask() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First task");
        List<TaskSpec> result = TaskDependencySorter.sort(List.of(t1), List.of(t1));
        assertThat(result).containsExactly(t1);
    }

    @Test
    void sortNoDependencies() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second");
        TaskSpec t3 = createTask("TASK-3", "Third");

        List<TaskSpec> result = TaskDependencySorter.sort(
                List.of(t3, t1, t2), List.of(t1, t2, t3));

        // Should be sorted by numeric ID
        assertThat(result).containsExactly(t1, t2, t3);
    }

    @Test
    void sortWithDependencies() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");
        TaskSpec t3 = createTask("TASK-3", "Third", "TASK-2");

        List<TaskSpec> all = List.of(t1, t2, t3);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        assertThat(result).containsExactly(t1, t2, t3);
    }

    @Test
    void sortWithDependenciesReversedInput() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");
        TaskSpec t3 = createTask("TASK-3", "Third", "TASK-2");

        List<TaskSpec> all = List.of(t3, t2, t1);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        // Even with reversed input, topological sort ensures correct order
        assertThat(result).containsExactly(t1, t2, t3);
    }

    @Test
    void sortWithDiamondDependencies() throws CircularDependencyException {
        // t1 -> t2, t3 -> t4
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");
        TaskSpec t3 = createTask("TASK-3", "Third", "TASK-1");
        TaskSpec t4 = createTask("TASK-4", "Fourth", "TASK-2", "TASK-3");

        List<TaskSpec> all = List.of(t1, t2, t3, t4);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        // t1 must come first, t4 must come last
        assertThat(result.get(0)).isEqualTo(t1);
        assertThat(result.get(3)).isEqualTo(t4);
        // t2 and t3 can be in any order but both before t4
        assertThat(result.subList(1, 3)).containsExactlyInAnyOrder(t2, t3);
    }

    @Test
    void sortDetectsCircularDependency() {
        TaskSpec t1 = createTask("TASK-1", "First", "TASK-2");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");

        List<TaskSpec> all = List.of(t1, t2);
        assertThatThrownBy(() -> TaskDependencySorter.sort(all, all))
                .isInstanceOf(CircularDependencyException.class)
                .hasMessageContaining("TASK");
    }

    @Test
    void sortWithExternalDoneDependency() throws CircularDependencyException {
        // t2 depends on t1, but t1 is not in the selected set — it's already Done
        TaskSpec t1 = createTask("TASK-1", "First");
        t1.setStatus("Done");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");

        List<TaskSpec> selected = List.of(t2);
        List<TaskSpec> all = List.of(t1, t2);
        List<TaskSpec> result = TaskDependencySorter.sort(selected, all);

        // t2 should be included since its external dep is Done
        assertThat(result).containsExactly(t2);
    }

    @Test
    void getUnsatisfiedDependenciesAllSatisfied() {
        TaskSpec t1 = createTask("TASK-1", "First");
        t1.setStatus("Done");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");

        Set<String> completed = Set.of("task-1");
        Set<String> selected = Set.of("task-1", "task-2");
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                t2, completed, selected, List.of(t1, t2));

        assertThat(unsatisfied).isEmpty();
    }

    @Test
    void getUnsatisfiedDependenciesExternalNotDone() {
        TaskSpec t1 = createTask("TASK-1", "First");
        t1.setStatus("To Do");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");

        Set<String> completed = Collections.emptySet();
        Set<String> selected = Set.of("task-2"); // t1 not in selected set
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                t2, completed, selected, List.of(t1, t2));

        assertThat(unsatisfied).containsExactly("TASK-1");
    }

    @Test
    void sortRespectsOrdinal() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        t1.setOrdinal(100);
        TaskSpec t2 = createTask("TASK-2", "Second");
        t2.setOrdinal(50);
        TaskSpec t3 = createTask("TASK-3", "Third");
        t3.setOrdinal(50);

        List<TaskSpec> all = List.of(t1, t2, t3);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        // t2 and t3 (ordinal 50) should come before t1 (ordinal 100)
        // Within same ordinal, sort by numeric ID
        assertThat(result).containsExactly(t2, t3, t1);
    }

    @Test
    void circularDependencyExceptionContainsTaskIds() {
        TaskSpec t1 = createTask("TASK-1", "First", "TASK-3");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");
        TaskSpec t3 = createTask("TASK-3", "Third", "TASK-2");

        List<TaskSpec> all = List.of(t1, t2, t3);
        assertThatThrownBy(() -> TaskDependencySorter.sort(all, all))
                .isInstanceOf(CircularDependencyException.class)
                .satisfies(ex -> {
                    CircularDependencyException cde = (CircularDependencyException) ex;
                    assertThat(cde.getTaskIds()).hasSize(3);
                });
    }

    // --- getUnsatisfiedDependencies() branch coverage ---

    @Test
    void getUnsatisfiedDependenciesNullDeps() {
        TaskSpec task = createTask("TASK-1", "No deps");
        task.setDependencies(null);

        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                task, Collections.emptySet(), Collections.emptySet(), Collections.emptyList());

        assertThat(unsatisfied).isEmpty();
    }

    @Test
    void getUnsatisfiedDependenciesEmptyDeps() {
        TaskSpec task = createTask("TASK-1", "Empty deps");

        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                task, Collections.emptySet(), Collections.emptySet(), Collections.emptyList());

        assertThat(unsatisfied).isEmpty();
    }

    @Test
    void getUnsatisfiedDependenciesInternalNotCompleted() {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");

        Set<String> completed = Collections.emptySet();
        Set<String> selected = Set.of("task-1", "task-2");
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                t2, completed, selected, List.of(t1, t2));

        // TASK-1 is in selected set but not completed
        assertThat(unsatisfied).containsExactly("TASK-1");
    }

    @Test
    void getUnsatisfiedDependenciesExternalDone() {
        TaskSpec t1 = createTask("TASK-1", "First");
        t1.setStatus("Done");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");

        Set<String> completed = Collections.emptySet();
        Set<String> selected = Set.of("task-2"); // t1 not selected
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                t2, completed, selected, List.of(t1, t2));

        // External dep is Done, so satisfied
        assertThat(unsatisfied).isEmpty();
    }

    @Test
    void getUnsatisfiedDependenciesUnknownDep() {
        TaskSpec t1 = createTask("TASK-1", "First", "TASK-UNKNOWN");

        Set<String> completed = Collections.emptySet();
        Set<String> selected = Set.of("task-1");
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                t1, completed, selected, Collections.emptyList());

        // Unknown dep (not in allSpecs) is unsatisfied
        assertThat(unsatisfied).containsExactly("TASK-UNKNOWN");
    }

    @Test
    void getUnsatisfiedDependenciesExternalNotDoneInAllSpecs() {
        TaskSpec ext = createTask("TASK-EXT", "External");
        ext.setStatus("In Progress");
        TaskSpec t1 = createTask("TASK-1", "First", "TASK-EXT");

        Set<String> completed = Collections.emptySet();
        Set<String> selected = Set.of("task-1");
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                t1, completed, selected, List.of(ext, t1));

        // External dep is In Progress, not Done
        assertThat(unsatisfied).containsExactly("TASK-EXT");
    }

    @Test
    void getUnsatisfiedDependenciesMixedSatisfaction() {
        TaskSpec done = createTask("TASK-1", "Done");
        done.setStatus("Done");
        TaskSpec notDone = createTask("TASK-2", "Not done");
        notDone.setStatus("To Do");
        TaskSpec task = createTask("TASK-3", "Depends on both", "TASK-1", "TASK-2");

        Set<String> completed = Set.of("task-1");
        Set<String> selected = Set.of("task-3"); // neither dep is in selected
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                task, completed, selected, List.of(done, notDone, task));

        // TASK-1 is completed, TASK-2 is external and not Done
        assertThat(unsatisfied).containsExactly("TASK-2");
    }

    @Test
    void getUnsatisfiedDependenciesAllSpecsWithNullId() {
        TaskSpec nullIdSpec = new TaskSpec();
        nullIdSpec.setStatus("Done");
        TaskSpec t1 = createTask("TASK-1", "First", "TASK-2");

        Set<String> completed = Collections.emptySet();
        Set<String> selected = Set.of("task-1");
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                t1, completed, selected, List.of(nullIdSpec, t1));

        // TASK-2 not found in allSpecs (only null-id spec there)
        assertThat(unsatisfied).containsExactly("TASK-2");
    }

    // --- sort() branch coverage ---

    @Test
    void sortWithNullDependencyList() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        t1.setDependencies(null);
        TaskSpec t2 = createTask("TASK-2", "Second");
        t2.setDependencies(null);

        List<TaskSpec> all = List.of(t2, t1);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        assertThat(result).containsExactly(t1, t2);
    }

    @Test
    void sortWithNullIdTask() throws CircularDependencyException {
        TaskSpec nullId = new TaskSpec();
        nullId.setTitle("No ID");
        nullId.setOrdinal(1000);
        nullId.setDependencies(new ArrayList<>());

        TaskSpec t1 = createTask("TASK-1", "First");

        List<TaskSpec> all = List.of(nullId, t1);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        // Task with null ID isn't added to selectedIds, so it won't appear in sorted output
        assertThat(result).containsExactly(t1);
    }

    @Test
    void sortWithNonNumericIds() throws CircularDependencyException {
        TaskSpec ta = createTask("alpha", "Alpha task");
        TaskSpec tb = createTask("beta", "Beta task");

        List<TaskSpec> all = List.of(tb, ta);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        // Both have same ordinal (1000) and no numeric part → extractNumber returns MAX_VALUE
        // Tiebreak by extractNumber is equal, so insertion order in layer is preserved
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(ta, tb);
    }

    @Test
    void sortAllSpecsWithNullId() throws CircularDependencyException {
        TaskSpec nullIdSpec = new TaskSpec();
        nullIdSpec.setStatus("Done");

        TaskSpec t1 = createTask("TASK-1", "First");

        List<TaskSpec> result = TaskDependencySorter.sort(
                List.of(t1), List.of(nullIdSpec, t1));

        assertThat(result).containsExactly(t1);
    }

    @Test
    void sortWithCaseInsensitiveDeps() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second");
        // Use mixed case for dependency reference
        t2.setDependencies(new ArrayList<>(List.of("task-1")));

        List<TaskSpec> all = List.of(t2, t1);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        assertThat(result).containsExactly(t1, t2);
    }

    @Test
    void sortWithEqualOrdinalDifferentNumericIds() throws CircularDependencyException {
        TaskSpec t10 = createTask("TASK-10", "Tenth");
        t10.setOrdinal(500);
        TaskSpec t2 = createTask("TASK-2", "Second");
        t2.setOrdinal(500);

        List<TaskSpec> all = List.of(t10, t2);
        List<TaskSpec> result = TaskDependencySorter.sort(all, all);

        // Same ordinal, tiebreak by numeric ID: 2 < 10
        assertThat(result).containsExactly(t2, t10);
    }

    // ===== sortByLayers tests =====

    @Test
    void layersEmptyList() throws CircularDependencyException {
        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(
                Collections.emptyList(), Collections.emptyList());
        assertThat(layers).isEmpty();
    }

    @Test
    void layersSingleTask() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(List.of(t1), List.of(t1));
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0)).containsExactly(t1);
    }

    @Test
    void layersNoDependenciesAllInOneLayer() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second");
        TaskSpec t3 = createTask("TASK-3", "Third");

        List<TaskSpec> all = List.of(t3, t1, t2);
        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(all, all);

        // All independent tasks should be in a single layer
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0)).containsExactly(t1, t2, t3);
    }

    @Test
    void layersLinearChainProducesOneLayerPerTask() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");
        TaskSpec t3 = createTask("TASK-3", "Third", "TASK-2");

        List<TaskSpec> all = List.of(t1, t2, t3);
        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(all, all);

        assertThat(layers).hasSize(3);
        assertThat(layers.get(0)).containsExactly(t1);
        assertThat(layers.get(1)).containsExactly(t2);
        assertThat(layers.get(2)).containsExactly(t3);
    }

    @Test
    void layersDiamondDependency() throws CircularDependencyException {
        // Diamond: T1 -> T2, T1 -> T3, T2 -> T4, T3 -> T4
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");
        TaskSpec t3 = createTask("TASK-3", "Third", "TASK-1");
        TaskSpec t4 = createTask("TASK-4", "Fourth", "TASK-2", "TASK-3");

        List<TaskSpec> all = List.of(t1, t2, t3, t4);
        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(all, all);

        assertThat(layers).hasSize(3);
        assertThat(layers.get(0)).containsExactly(t1);
        assertThat(layers.get(1)).containsExactlyInAnyOrder(t2, t3);
        assertThat(layers.get(2)).containsExactly(t4);
    }

    @Test
    void layersCircularDependencyThrows() {
        TaskSpec t1 = createTask("TASK-1", "First", "TASK-2");
        TaskSpec t2 = createTask("TASK-2", "Second", "TASK-1");

        List<TaskSpec> all = List.of(t1, t2);

        assertThatThrownBy(() -> TaskDependencySorter.sortByLayers(all, all))
                .isInstanceOf(CircularDependencyException.class);
    }

    @Test
    void layersPartialSelectionWithExternalDoneDependency() throws CircularDependencyException {
        // T1 is Done (not selected), T2 depends on T1 (selected), T3 is independent (selected)
        TaskSpec t1 = createTask("TASK-1", "Done task");
        t1.setStatus("Done");
        TaskSpec t2 = createTask("TASK-2", "Depends on Done", "TASK-1");
        TaskSpec t3 = createTask("TASK-3", "Independent");

        List<TaskSpec> selected = List.of(t2, t3);
        List<TaskSpec> allSpecs = List.of(t1, t2, t3);

        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(selected, allSpecs);

        // T2 and T3 are both in layer 0 since T1 is external/done
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0)).containsExactlyInAnyOrder(t2, t3);
    }

    @Test
    void layersWideGraphIndependentTasksGroupedTogether() throws CircularDependencyException {
        // A root task with 4 independent children
        TaskSpec root = createTask("ROOT", "Root");
        TaskSpec a = createTask("A", "Task A", "ROOT");
        TaskSpec b = createTask("B", "Task B", "ROOT");
        TaskSpec c = createTask("C", "Task C", "ROOT");
        TaskSpec d = createTask("D", "Task D", "ROOT");
        TaskSpec finalTask = createTask("FINAL", "Final", "A", "B", "C", "D");

        List<TaskSpec> all = List.of(root, a, b, c, d, finalTask);
        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(all, all);

        assertThat(layers).hasSize(3);
        assertThat(layers.get(0)).containsExactly(root);
        assertThat(layers.get(1)).containsExactlyInAnyOrder(a, b, c, d);
        assertThat(layers.get(2)).containsExactly(finalTask);
    }

    @Test
    void layersWithCaseInsensitiveDeps() throws CircularDependencyException {
        TaskSpec t1 = createTask("TASK-1", "First");
        TaskSpec t2 = createTask("TASK-2", "Second");
        t2.setDependencies(new ArrayList<>(List.of("task-1"))); // lowercase

        List<TaskSpec> all = List.of(t2, t1);
        List<List<TaskSpec>> layers = TaskDependencySorter.sortByLayers(all, all);

        assertThat(layers).hasSize(2);
        assertThat(layers.get(0)).containsExactly(t1);
        assertThat(layers.get(1)).containsExactly(t2);
    }

    // ===== Helpers =====

    private static TaskSpec createTask(String id, String title, String... dependencies) {
        TaskSpec spec = new TaskSpec();
        spec.setId(id);
        spec.setTitle(title);
        spec.setStatus("To Do");
        spec.setOrdinal(1000);
        if (dependencies.length > 0) {
            spec.setDependencies(new ArrayList<>(Arrays.asList(dependencies)));
        } else {
            spec.setDependencies(new ArrayList<>());
        }
        return spec;
    }
}
