package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecContextBuilder;
import com.devoxx.genie.service.spec.SpecService;
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
 * Executes the 10 backlog task tools: create, list, search, view, edit, complete, archive,
 * archive_done, unarchive, list_archived.
 */
@Slf4j
public class BacklogTaskToolExecutor implements ToolExecutor {

    public static final String PRIORITY = "priority";
    public static final String STATUS = "status";
    public static final String MILESTONE = "milestone";
    public static final String PARENT_TASK_ID = "parentTaskId";
    public static final String LABELS = "labels";
    public static final String DESCRIPTION = "description";
    public static final String ASSIGNEE = "assignee";
    public static final String DEPENDENCIES = "dependencies";
    public static final String REFERENCES = "references";
    public static final String DOCUMENTATION = "documentation";
    public static final String ACCEPTANCE_CRITERIA = "acceptanceCriteria";
    public static final String DEFINITION_OF_DONE = "definitionOfDone";
    public static final String SKIP_DOD_DEFAULTS = "skipDodDefaults";
    public static final String SEARCH = "search";
    public static final String LIMIT = "limit";
    public static final String ERROR_ID_PARAMETER_IS_REQUIRED = "Error: 'id' parameter is required.";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String TASK = "Task ";
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
                case "backlog_task_archive_done" -> archiveDoneTasks();
                case "backlog_task_unarchive" -> unarchiveTask(request.arguments());
                case "backlog_task_list_archived" -> listArchivedTasks();
                default -> "Error: Unknown task tool: " + request.name();
            };
        } catch (Exception e) {
            log.warn("Error executing backlog task tool: {}", request.name(), e);
            return "Error: " + e.getMessage();
        }
    }

    private @NotNull String createTask(@NotNull String arguments) throws Exception {
        String title = ToolArgumentParser.getString(arguments, TITLE);
        if (title == null || title.isEmpty()) {
            return "Error: 'title' parameter is required.";
        }

        TaskSpec.TaskSpecBuilder builder = TaskSpec.builder().title(title);

        String description = ToolArgumentParser.getString(arguments, DESCRIPTION);
        if (description != null) builder.description(description);

        String priority = ToolArgumentParser.getString(arguments, PRIORITY);
        if (priority != null) builder.priority(priority);

        String status = ToolArgumentParser.getString(arguments, STATUS);
        if (status != null) builder.status(status);

        String milestone = ToolArgumentParser.getString(arguments, MILESTONE);
        if (milestone != null) builder.milestone(milestone);

        String parentTaskId = ToolArgumentParser.getString(arguments, PARENT_TASK_ID);
        if (parentTaskId != null) builder.parentTaskId(parentTaskId);

        List<String> labels = ToolArgumentParser.getStringArray(arguments, LABELS);
        if (!labels.isEmpty()) builder.labels(new ArrayList<>(labels));

        List<String> assignees = ToolArgumentParser.getStringArray(arguments, ASSIGNEE);
        if (!assignees.isEmpty()) builder.assignees(new ArrayList<>(assignees));

        List<String> dependencies = ToolArgumentParser.getStringArray(arguments, DEPENDENCIES);
        if (!dependencies.isEmpty()) builder.dependencies(new ArrayList<>(dependencies));

        List<String> references = ToolArgumentParser.getStringArray(arguments, REFERENCES);
        if (!references.isEmpty()) builder.references(new ArrayList<>(references));

        List<String> documentation = ToolArgumentParser.getStringArray(arguments, DOCUMENTATION);
        if (!documentation.isEmpty()) builder.documentation(new ArrayList<>(documentation));

        List<String> acTexts = ToolArgumentParser.getStringArray(arguments, ACCEPTANCE_CRITERIA);
        if (!acTexts.isEmpty()) {
            List<AcceptanceCriterion> criteria = new ArrayList<>();
            for (int i = 0; i < acTexts.size(); i++) {
                criteria.add(AcceptanceCriterion.builder()
                        .index(i).text(acTexts.get(i)).checked(false).build());
            }
            builder.acceptanceCriteria(criteria);
        }

        // Support explicit DoD items provided by the agent
        List<String> dodTexts = ToolArgumentParser.getStringArray(arguments, DEFINITION_OF_DONE);
        if (!dodTexts.isEmpty()) {
            List<DefinitionOfDoneItem> dodItems = new ArrayList<>();
            for (int i = 0; i < dodTexts.size(); i++) {
                dodItems.add(DefinitionOfDoneItem.builder()
                        .index(i).text(dodTexts.get(i)).checked(false).build());
            }
            builder.definitionOfDone(dodItems);
        }

        boolean skipDodDefaults = Boolean.parseBoolean(
                ToolArgumentParser.getString(arguments, SKIP_DOD_DEFAULTS));

        TaskSpec spec = builder.build();
        SpecService specService = SpecService.getInstance(project);
        TaskSpec created = specService.createTask(spec, skipDodDefaults);

        return "Created task " + created.getId() + ": " + created.getTitle() + "\nFile: " + created.getFilePath();
    }

    private @NotNull String listTasks(@NotNull String arguments) {
        String status = ToolArgumentParser.getString(arguments, STATUS);
        String assignee = ToolArgumentParser.getString(arguments, ASSIGNEE);
        List<String> labels = ToolArgumentParser.getStringArray(arguments, LABELS);
        String search = ToolArgumentParser.getString(arguments, SEARCH);
        int limit = ToolArgumentParser.getInt(arguments, LIMIT, 0);

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

        String status = ToolArgumentParser.getString(arguments, STATUS);
        String priority = ToolArgumentParser.getString(arguments, PRIORITY);
        int limit = ToolArgumentParser.getInt(arguments, LIMIT, 0);

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
        String id = ToolArgumentParser.getString(arguments, ID);
        if (id == null || id.isEmpty()) {
            return ERROR_ID_PARAMETER_IS_REQUIRED;
        }

        SpecService specService = SpecService.getInstance(project);
        TaskSpec spec = specService.getSpec(id);

        if (spec == null) {
            return "Error: Task with ID '" + id + "' not found.";
        }

        return SpecContextBuilder.buildContext(spec);
    }

    private @NotNull String editTask(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, ID);
        if (id == null || id.isEmpty()) {
            return ERROR_ID_PARAMETER_IS_REQUIRED;
        }

        SpecService specService = SpecService.getInstance(project);
        TaskSpec spec = specService.getSpec(id);
        if (spec == null) {
            return "Error: Task with ID '" + id + "' not found.";
        }

        applyScalarUpdates(spec, arguments);
        applyListUpdates(spec, arguments);
        applyAcceptanceCriteriaUpdates(spec, arguments);
        applyPlanUpdates(spec, arguments);
        applyNotesUpdates(spec, arguments);

        specService.updateTask(spec);
        return "Updated task " + id + " successfully.";
    }

    private void applyScalarUpdates(@NotNull TaskSpec spec, @NotNull String arguments) {
        String title = ToolArgumentParser.getString(arguments, TITLE);
        if (title != null) spec.setTitle(title);

        String description = ToolArgumentParser.getString(arguments, DESCRIPTION);
        if (description != null) spec.setDescription(description);

        String status = ToolArgumentParser.getString(arguments, STATUS);
        if (status != null) spec.setStatus(status);

        String priority = ToolArgumentParser.getString(arguments, PRIORITY);
        if (priority != null) spec.setPriority(priority);

        String milestone = ToolArgumentParser.getString(arguments, MILESTONE);
        if (milestone != null) spec.setMilestone(milestone);

        String finalSummary = ToolArgumentParser.getString(arguments, "finalSummary");
        if (finalSummary != null) spec.setFinalSummary(finalSummary);
    }

    private void applyListUpdates(@NotNull TaskSpec spec, @NotNull String arguments) {
        List<String> assignees = ToolArgumentParser.getStringArray(arguments, ASSIGNEE);
        if (!assignees.isEmpty()) spec.setAssignees(new ArrayList<>(assignees));

        List<String> labels = ToolArgumentParser.getStringArray(arguments, LABELS);
        if (!labels.isEmpty()) spec.setLabels(new ArrayList<>(labels));

        List<String> dependencies = ToolArgumentParser.getStringArray(arguments, DEPENDENCIES);
        if (!dependencies.isEmpty()) spec.setDependencies(new ArrayList<>(dependencies));
    }

    private void applyAcceptanceCriteriaUpdates(@NotNull TaskSpec spec, @NotNull String arguments) {
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
    }

    private void applyPlanUpdates(@NotNull TaskSpec spec, @NotNull String arguments) {
        if (ToolArgumentParser.getBoolean(arguments, "planClear", false)) {
            spec.setImplementationPlan(null);
            return;
        }
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

    private void applyNotesUpdates(@NotNull TaskSpec spec, @NotNull String arguments) {
        if (ToolArgumentParser.getBoolean(arguments, "notesClear", false)) {
            spec.setImplementationNotes(null);
            return;
        }
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

    private @NotNull String completeTask(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, ID);
        if (id == null || id.isEmpty()) {
            return ERROR_ID_PARAMETER_IS_REQUIRED;
        }

        SpecService specService = SpecService.getInstance(project);
        specService.completeTask(id);
        return TASK + id + " marked as Done.";
    }

    private @NotNull String archiveTask(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, ID);
        if (id == null || id.isEmpty()) {
            return ERROR_ID_PARAMETER_IS_REQUIRED;
        }

        SpecService specService = SpecService.getInstance(project);
        specService.archiveTask(id);
        return TASK + id + " archived.";
    }

    private @NotNull String archiveDoneTasks() throws Exception {
        SpecService specService = SpecService.getInstance(project);
        int count = specService.archiveDoneTasks();
        if (count == 0) {
            return "No completed tasks to archive.";
        }
        return count + " completed task(s) archived successfully.";
    }

    private @NotNull String unarchiveTask(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, ID);
        if (id == null || id.isEmpty()) {
            return ERROR_ID_PARAMETER_IS_REQUIRED;
        }

        SpecService specService = SpecService.getInstance(project);
        specService.unarchiveTask(id);
        return TASK + id + " restored from archive.";
    }

    private @NotNull String listArchivedTasks() {
        SpecService specService = SpecService.getInstance(project);
        List<TaskSpec> archived = specService.getArchivedTasks();

        if (archived.isEmpty()) {
            return "No archived tasks found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Archived tasks (").append(archived.size()).append("):\n\n");
        for (TaskSpec spec : archived) {
            sb.append("- ").append(spec.getDisplayLabel());
            if (spec.getStatus() != null) {
                sb.append(" [").append(spec.getStatus()).append("]");
            }
            if (spec.getPriority() != null) {
                sb.append(" [").append(spec.getPriority()).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
