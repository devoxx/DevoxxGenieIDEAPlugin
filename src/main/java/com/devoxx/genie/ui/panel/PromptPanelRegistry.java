package com.devoxx.genie.ui.panel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry to keep track of all PromptOutputPanels in the application.
 * This allows finding panels for a specific project when needed.
 */
@Service
public final class PromptPanelRegistry {

    private final Map<String, List<PromptOutputPanel>> projectPanels = new HashMap<>();

    /**
     * Get the singleton instance of the registry.
     *
     * @return The PromptPanelRegistry instance
     */
    @NotNull
    public static PromptPanelRegistry getInstance() {
        return ApplicationManager.getApplication().getService(PromptPanelRegistry.class);
    }

    /**
     * Register a panel for a specific project.
     *
     * @param project The project
     * @param panel The panel to register
     */
    public void registerPanel(@NotNull Project project, @NotNull PromptOutputPanel panel) {
        String projectId = project.getLocationHash();
        List<PromptOutputPanel> panels = projectPanels.computeIfAbsent(projectId, k -> new ArrayList<>());
        if (!panels.contains(panel)) {
            panels.add(panel);
        }
    }

    /**
     * Unregister a panel for a specific project.
     *
     * @param project The project
     * @param panel The panel to unregister
     */
    public void unregisterPanel(@NotNull Project project, @NotNull PromptOutputPanel panel) {
        String projectId = project.getLocationHash();
        List<PromptOutputPanel> panels = projectPanels.get(projectId);
        if (panels != null) {
            panels.remove(panel);
            if (panels.isEmpty()) {
                projectPanels.remove(projectId);
            }
        }
    }

    /**
     * Get all panels for a specific project.
     *
     * @param project The project
     * @return List of panels for the project
     */
    @NotNull
    public List<PromptOutputPanel> getPanels(@NotNull Project project) {
        String projectId = project.getLocationHash();
        return projectPanels.computeIfAbsent(projectId, k -> new ArrayList<>());
    }

    /**
     * Clear all registered panels for a project.
     *
     * @param project The project to clear panels for
     */
    public void clearPanels(@NotNull Project project) {
        String projectId = project.getLocationHash();
        projectPanels.remove(projectId);
    }
}
