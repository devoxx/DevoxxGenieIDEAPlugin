package com.devoxx.genie.model.agent;

import org.jetbrains.annotations.Nullable;

/**
 * Where a delegated Agent Team task executes (Phase A of per-agent isolation).
 * Per-agent instead of global because risk is asymmetric: read-only agents gain nothing
 * from container overhead, while an implementer running build commands is exactly what
 * you want sandboxed. One delegate_task fan-out can mix targets freely.
 */
public enum AgentExecutionTarget {

    /** AgentRunner thread inside the IDE — fast, works on the open project (default). */
    IN_PROCESS("In-process"),

    /**
     * One-shot container session on a DockerAgents orchestrator-api (TASK-248 backend):
     * full isolation, works on a fresh clone, returns a summary (+ pushed branch).
     * Requires the compose stack and an agent of the same name in its /agents directory.
     */
    DOCKER_AGENTS("DockerAgents container"),

    /**
     * Docker container spawned directly by the plugin (docker-java) with the PROJECT
     * BIND-MOUNTED at /session/repo — read-only for read-only agents, read-write
     * otherwise (TASK-251, Phase B). Sandboxes the process/host while keeping in-editor
     * edit semantics; requires Docker and the DockerAgents runner image, but no
     * orchestrator-api.
     */
    LOCAL_CONTAINER("Local container (project mounted)");

    private final String displayName;

    AgentExecutionTarget(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Null/unknown-safe parse for values persisted as strings; defaults to IN_PROCESS. */
    public static AgentExecutionTarget fromString(@Nullable String value) {
        if (value != null) {
            for (AgentExecutionTarget target : values()) {
                if (target.name().equalsIgnoreCase(value.trim())) {
                    return target;
                }
            }
        }
        return IN_PROCESS;
    }
}
