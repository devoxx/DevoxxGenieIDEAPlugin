package com.devoxx.genie.util;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-local holder for the current project context.
 * Used to pass project information to components that don't have direct access to it,
 * such as ChatModelListener implementations.
 */
public class ProjectContextHolder {
    private static final ThreadLocal<Project> PROJECT_CONTEXT = new ThreadLocal<>();

    /**
     * Sets the current project context for this thread.
     *
     * @param project the project to set
     */
    public static void setCurrentProject(@Nullable Project project) {
        if (project != null) {
            PROJECT_CONTEXT.set(project);
        } else {
            PROJECT_CONTEXT.remove();
        }
    }

    /**
     * Gets the current project context for this thread.
     *
     * @return the current project, or null if not set
     */
    @Nullable
    public static Project getCurrentProject() {
        return PROJECT_CONTEXT.get();
    }

    /**
     * Clears the current project context for this thread.
     * Should be called in a finally block to prevent memory leaks.
     */
    public static void clear() {
        PROJECT_CONTEXT.remove();
    }
}
