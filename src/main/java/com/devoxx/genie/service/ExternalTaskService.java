package com.devoxx.genie.service;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.BacklogConfigService;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Project-level service that allows external plugins to create backlog tasks
 * in DevoxxGenie's Backlog.md-compatible task management system.
 */
@Service(Service.Level.PROJECT)
public final class ExternalTaskService {

    private final Project project;

    public ExternalTaskService(@NotNull Project project) {
        this.project = project;
    }

    public static ExternalTaskService getInstance(@NotNull Project project) {
        return project.getService(ExternalTaskService.class);
    }

    /**
     * Creates a backlog task. Initializes the backlog if needed.
     * Returns the created task ID (e.g., "TASK-5").
     *
     * THIS METHOD IS NOT DEAD CODE. It is the public integration API called by
     * external plugins (e.g. SonarLint) via runtime reflection. Removing it
     * silently breaks the "Create DevoxxGenie Tasks" action.
     *
     * @see org.sonarlint.intellij.actions.CreateDevoxxGenieTasksFromNodeAction (SonarLint plugin caller)
     */
    public String createBacklogTask(String title, String description,
                                    String priority, List<String> labels) throws IOException {
        BacklogConfigService config = BacklogConfigService.getInstance(project);
        config.ensureInitialized();
        String nextId = config.getNextTaskId();
        TaskSpec spec = TaskSpec.builder()
                .id(nextId)
                .title(title)
                .description(description)
                .priority(priority)
                .labels(labels != null ? new ArrayList<>(labels) : new ArrayList<>())
                .status("To Do")
                .build();
        TaskSpec created = SpecService.getInstance(project).createTask(spec, true);
        return created.getId();
    }
}
