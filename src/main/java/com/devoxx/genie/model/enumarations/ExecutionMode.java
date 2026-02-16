package com.devoxx.genie.model.enumarations;

/**
 * Execution mode for spec task runner.
 * Controls whether tasks within the same dependency layer are executed
 * sequentially or in parallel.
 */
public enum ExecutionMode {
    SEQUENTIAL,
    PARALLEL
}
