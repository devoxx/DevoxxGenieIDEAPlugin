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
