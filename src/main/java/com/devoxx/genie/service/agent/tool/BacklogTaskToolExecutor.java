package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecContextBuilder;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.service.spec.search.SpecSearchService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Executes the 7 backlog task tools: create, list, search, view, edit, complete, archive.
 */
@Slf4j
public class BacklogTaskToolExecutor implements ToolExecutor {

    private final Project project;

    public BacklogTaskToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            return switch (request.name()) {
                case "backlog_task_create" -> createTask(request.arguments());
                case "backlog_task_list" -> listTasks(request.arguments());
                case "backlog_task_search" -> searchTasks(request.arguments());
                case "backlog_task_view" -> viewTask(request.arguments());
                case "backlog_task_edit" -> editTask(request.arguments());
                case "backlog_task_complete" -> completeTask(request.arguments());
                case "backlog_task_archive" -> archiveTask(request.arguments());
                case "backlog_task_find_related" -> findRelated(request.arguments());
                default -> "Error: Unknown task tool: " + request.name();
            };
        } catch (Exception e) {
            log.warn("Error executing backlog task tool: {}", request.name(), e);
            return "Error: " + e.getMessage();
        }
    }

    private @NotNull String createTask(@NotNull String arguments) throws Exception {
        String title = ToolArgumentParser.getString(arguments, "title");
        if (title == null || title.isEmpty()) {
            return "Error: 'title' parameter is required.";
        }

        TaskSpec.TaskSpecBuilder builder = TaskSpec.builder().title(title);

        String description = ToolArgumentParser.getString(arguments, "description");
        if (description != null) builder.description(description);

        String priority = ToolArgumentParser.getString(arguments, "priority");
        if (priority != null) builder.priority(priority);

        String status = ToolArgumentParser.getString(arguments, "status");
        if (status != null) builder.status(status);

        String milestone = ToolArgumentParser.getString(arguments, "milestone");
        if (milestone != null) builder.milestone(milestone);

        String parentTaskId = ToolArgumentParser.getString(arguments, "parentTaskId");
        if (parentTaskId != null) builder.parentTaskId(parentTaskId);

        List<String> labels = ToolArgumentParser.getStringArray(arguments, "labels");
        if (!labels.isEmpty()) builder.labels(new ArrayList<>(labels));

        List<String> assignees = ToolArgumentParser.getStringArray(arguments, "assignee");
        if (!assignees.isEmpty()) builder.assignees(new ArrayList<>(assignees));

        List<String> dependencies = ToolArgumentParser.getStringArray(arguments, "dependencies");
        if (!dependencies.isEmpty()) builder.dependencies(new ArrayList<>(dependencies));

        List<String> references = ToolArgumentParser.getStringArray(arguments, "references");
        if (!references.isEmpty()) builder.references(new ArrayList<>(references));

        List<String> documentation = ToolArgumentParser.getStringArray(arguments, "documentation");
        if (!documentation.isEmpty()) builder.documentation(new ArrayList<>(documentation));

        List<String> acTexts = ToolArgumentParser.getStringArray(arguments, "acceptanceCriteria");
        if (!acTexts.isEmpty()) {
            List<AcceptanceCriterion> criteria = new ArrayList<>();
            for (int i = 0; i < acTexts.size(); i++) {
                criteria.add(AcceptanceCriterion.builder()
                        .index(i).text(acTexts.get(i)).checked(false).build());
            }
            builder.acceptanceCriteria(criteria);
        }

        TaskSpec spec = builder.build();
        SpecService specService = SpecService.getInstance(project);
        TaskSpec created = specService.createTask(spec);

        return "Created task " + created.getId() + ": " + created.getTitle() + "\nFile: " + created.getFilePath();
    }

    private @NotNull String listTasks(@NotNull String arguments) {
        String status = ToolArgumentParser.getString(arguments, "status");
        String assignee = ToolArgumentParser.getString(arguments, "assignee");
        List<String> labels = ToolArgumentParser.getStringArray(arguments, "labels");
        String search = ToolArgumentParser.getString(arguments, "search");
        int limit = ToolArgumentParser.getInt(arguments, "limit", 0);

        SpecService specService = SpecService.getInstance(project);
        List<TaskSpec> specs = specService.getSpecsByFilters(status, assignee, labels.isEmpty() ? null : labels, search, limit);

        if (specs.isEmpty()) {
            return "No tasks found matching the given filters.";
        }

        // Group by status
        Map<String, List<TaskSpec>> grouped = specs.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus() != null ? s.getStatus() : "Unknown"));

        StringBuilder sb = new StringBuilder();
        sb.append("Tasks (").append(specs.size()).append(" total):\n\n");

        for (Map.Entry<String, List<TaskSpec>> entry : grouped.entrySet()) {
            sb.append("## ").append(entry.getKey()).append("\n");
            for (TaskSpec spec : entry.getValue()) {
                sb.append("- ").append(spec.getDisplayLabel());
                if (spec.getPriority() != null) {
                    sb.append(" [").append(spec.getPriority()).append("]");
                }
                if (spec.getMilestone() != null) {
                    sb.append(" (").append(spec.getMilestone()).append(")");
                }
                if (!spec.getAcceptanceCriteria().isEmpty()) {
                    sb.append(" AC: ")
                            .append(spec.getCheckedAcceptanceCriteriaCount())
                            .append("/")
                            .append(spec.getAcceptanceCriteria().size());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private @NotNull String searchTasks(@NotNull String arguments) {
        String query = ToolArgumentParser.getString(arguments, "query");
        if (query == null || query.isEmpty()) {
            return "Error: 'query' parameter is required.";
        }

        String status = ToolArgumentParser.getString(arguments, "status");
        String priority = ToolArgumentParser.getString(arguments, "priority");
        int limit = ToolArgumentParser.getInt(arguments, "limit", 0);

        SpecService specService = SpecService.getInstance(project);
        List<TaskSpec> results = specService.searchSpecs(query, status, priority, limit);

        if (results.isEmpty()) {
            return "No tasks found matching query: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Search results (").append(results.size()).append("):\n\n");
        for (TaskSpec spec : results) {
            sb.append("- ").append(spec.getDisplayLabel());
            sb.append(" [").append(spec.getStatus()).append("]");
            if (spec.getPriority() != null) {
                sb.append(" [").append(spec.getPriority()).append("]");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private @NotNull String viewTask(@NotNull String arguments) {
        String id = ToolArgumentParser.getString(arguments, "id");
        if (id == null || id.isEmpty()) {
            return "Error: 'id' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        TaskSpec spec = specService.getSpec(id);

        if (spec == null) {
            return "Error: Task with ID '" + id + "' not found.";
        }

        return SpecContextBuilder.buildContext(spec);
    }

    private @NotNull String editTask(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, "id");
        if (id == null || id.isEmpty()) {
            return "Error: 'id' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        TaskSpec spec = specService.getSpec(id);
        if (spec == null) {
            return "Error: Task with ID '" + id + "' not found.";
        }

        // Apply scalar updates
        String title = ToolArgumentParser.getString(arguments, "title");
        if (title != null) spec.setTitle(title);

        String description = ToolArgumentParser.getString(arguments, "description");
        if (description != null) spec.setDescription(description);

        String status = ToolArgumentParser.getString(arguments, "status");
        if (status != null) spec.setStatus(status);

        String priority = ToolArgumentParser.getString(arguments, "priority");
        if (priority != null) spec.setPriority(priority);

        String milestone = ToolArgumentParser.getString(arguments, "milestone");
        if (milestone != null) spec.setMilestone(milestone);

        String finalSummary = ToolArgumentParser.getString(arguments, "finalSummary");
        if (finalSummary != null) spec.setFinalSummary(finalSummary);

        // Apply list replacements
        List<String> assignees = ToolArgumentParser.getStringArray(arguments, "assignee");
        if (!assignees.isEmpty()) spec.setAssignees(new ArrayList<>(assignees));

        List<String> labels = ToolArgumentParser.getStringArray(arguments, "labels");
        if (!labels.isEmpty()) spec.setLabels(new ArrayList<>(labels));

        List<String> dependencies = ToolArgumentParser.getStringArray(arguments, "dependencies");
        if (!dependencies.isEmpty()) spec.setDependencies(new ArrayList<>(dependencies));

        // Acceptance criteria modifications
        List<String> acAdd = ToolArgumentParser.getStringArray(arguments, "acceptanceCriteriaAdd");
        if (!acAdd.isEmpty()) {
            List<AcceptanceCriterion> existing = spec.getAcceptanceCriteria();
            if (existing == null) existing = new ArrayList<>();
            int startIdx = existing.size();
            for (int i = 0; i < acAdd.size(); i++) {
                existing.add(AcceptanceCriterion.builder()
                        .index(startIdx + i).text(acAdd.get(i)).checked(false).build());
            }
            spec.setAcceptanceCriteria(existing);
        }

        List<Integer> acCheck = ToolArgumentParser.getIntArray(arguments, "acceptanceCriteriaCheck");
        for (int idx : acCheck) {
            int zeroIdx = idx - 1; // Convert 1-based to 0-based
            if (zeroIdx >= 0 && zeroIdx < spec.getAcceptanceCriteria().size()) {
                spec.getAcceptanceCriteria().get(zeroIdx).setChecked(true);
            }
        }

        List<Integer> acUncheck = ToolArgumentParser.getIntArray(arguments, "acceptanceCriteriaUncheck");
        for (int idx : acUncheck) {
            int zeroIdx = idx - 1;
            if (zeroIdx >= 0 && zeroIdx < spec.getAcceptanceCriteria().size()) {
                spec.getAcceptanceCriteria().get(zeroIdx).setChecked(false);
            }
        }

        // Implementation plan
        if (ToolArgumentParser.getBoolean(arguments, "planClear", false)) {
            spec.setImplementationPlan(null);
        } else {
            String planSet = ToolArgumentParser.getString(arguments, "planSet");
            if (planSet != null) {
                spec.setImplementationPlan(planSet);
            }
            List<String> planAppend = ToolArgumentParser.getStringArray(arguments, "planAppend");
            if (!planAppend.isEmpty()) {
                String existing = spec.getImplementationPlan() != null ? spec.getImplementationPlan() : "";
                spec.setImplementationPlan(existing + (existing.isEmpty() ? "" : "\n\n") + String.join("\n\n", planAppend));
            }
        }

        // Implementation notes
        if (ToolArgumentParser.getBoolean(arguments, "notesClear", false)) {
            spec.setImplementationNotes(null);
        } else {
            String notesSet = ToolArgumentParser.getString(arguments, "notesSet");
            if (notesSet != null) {
                spec.setImplementationNotes(notesSet);
            }
            List<String> notesAppend = ToolArgumentParser.getStringArray(arguments, "notesAppend");
            if (!notesAppend.isEmpty()) {
                String existing = spec.getImplementationNotes() != null ? spec.getImplementationNotes() : "";
                spec.setImplementationNotes(existing + (existing.isEmpty() ? "" : "\n\n") + String.join("\n\n", notesAppend));
            }
        }

        specService.updateTask(spec);
        return "Updated task " + id + " successfully.";
    }

    private @NotNull String completeTask(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, "id");
        if (id == null || id.isEmpty()) {
            return "Error: 'id' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        specService.completeTask(id);
        return "Task " + id + " marked as Done.";
    }

    private @NotNull String archiveTask(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, "id");
        if (id == null || id.isEmpty()) {
            return "Error: 'id' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        specService.archiveTask(id);
        return "Task " + id + " archived.";
    }

    private @NotNull String findRelated(@NotNull String arguments) {
        String id = ToolArgumentParser.getString(arguments, "id");
        String query = ToolArgumentParser.getString(arguments, "query");
        int limit = ToolArgumentParser.getInt(arguments, "limit", 3);

        if ((id == null || id.isEmpty()) && (query == null || query.isEmpty())) {
            return "Error: Either 'id' or 'query' parameter is required.";
        }

        SpecSearchService searchService = new SpecSearchService(project);
        List<SpecSearchService.ScoredSpec> results;

        if (id != null && !id.isEmpty()) {
            results = searchService.findRelatedByTaskId(id, limit);
        } else {
            results = searchService.findRelatedByQuery(query, limit);
        }

        if (results.isEmpty()) {
            return id != null && !id.isEmpty()
                    ? "No related tasks found for " + id + "."
                    : "No tasks found matching query: " + query;
        }

        StringBuilder sb = new StringBuilder();
        if (id != null && !id.isEmpty()) {
            sb.append("Tasks related to ").append(id).append(" (").append(results.size()).append(" results):\n\n");
        } else {
            sb.append("Tasks matching \"").append(query).append("\" (").append(results.size()).append(" results):\n\n");
        }

        for (int i = 0; i < results.size(); i++) {
            SpecSearchService.ScoredSpec scored = results.get(i);
            TaskSpec spec = scored.spec();
            sb.append(i + 1).append(". **").append(spec.getDisplayLabel()).append("**");
            sb.append(" (score: ").append(String.format("%.2f", scored.score())).append(")\n");
            sb.append("   Status: ").append(spec.getStatus());
            if (spec.getPriority() != null) {
                sb.append(" | Priority: ").append(spec.getPriority());
            }
            if (spec.getMilestone() != null) {
                sb.append(" | Milestone: ").append(spec.getMilestone());
            }
            sb.append("\n");
            if (spec.getDescription() != null && !spec.getDescription().isEmpty()) {
                String desc = spec.getDescription();
                if (desc.length() > 150) {
                    desc = desc.substring(0, 147) + "...";
                }
                sb.append("   ").append(desc).append("\n");
            }
            if (spec.getLabels() != null && !spec.getLabels().isEmpty()) {
                sb.append("   Labels: ").append(String.join(", ", spec.getLabels())).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
