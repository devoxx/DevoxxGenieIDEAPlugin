package com.devoxx.genie.service.spec;

import java.util.List;

/**
 * Thrown when a circular dependency is detected among task specs during topological sorting.
 */
public class CircularDependencyException extends Exception {

    private final List<String> taskIds;

    public CircularDependencyException(List<String> taskIds) {
        super("Circular dependency detected among tasks: " + String.join(", ", taskIds));
        this.taskIds = taskIds;
    }

    public List<String> getTaskIds() {
        return taskIds;
    }
}
