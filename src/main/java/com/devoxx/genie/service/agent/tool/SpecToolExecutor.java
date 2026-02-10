package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecContextBuilder;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Built-in agent tool for managing task specs.
 * Provides list_specs, view_spec, update_spec_status, and check_acceptance_criterion operations.
 */
@Slf4j
public class SpecToolExecutor implements ToolExecutor {

    private final Project project;

    public SpecToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String arguments = request.arguments();
            String operation = ToolArgumentParser.getString(arguments, "operation");

            if (operation == null) {
                return "Error: 'operation' parameter is required. Valid operations: list_specs, view_spec, update_spec_status, check_acceptance_criterion";
            }

            return switch (operation) {
                case "list_specs" -> listSpecs();
                case "view_spec" -> viewSpec(arguments);
                case "update_spec_status" -> updateSpecStatus(arguments);
                case "check_acceptance_criterion" -> checkAcceptanceCriterion(arguments);
                default -> "Error: Unknown operation '" + operation + "'. Valid operations: list_specs, view_spec, update_spec_status, check_acceptance_criterion";
            };
        } catch (Exception e) {
            log.warn("Error executing manage_spec tool", e);
            return "Error: " + e.getMessage();
        }
    }

    private @NotNull String listSpecs() {
        SpecService specService = SpecService.getInstance(project);
        List<TaskSpec> specs = specService.getAllSpecs();

        if (specs.isEmpty()) {
            return "No task specs found in the project.";
        }

        // Group by status
        Map<String, List<TaskSpec>> grouped = specs.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus() != null ? s.getStatus() : "Unknown"));

        StringBuilder sb = new StringBuilder();
        sb.append("Task Specs (").append(specs.size()).append(" total):\n\n");

        for (Map.Entry<String, List<TaskSpec>> entry : grouped.entrySet()) {
            sb.append("## ").append(entry.getKey()).append("\n");
            for (TaskSpec spec : entry.getValue()) {
                sb.append("- ").append(spec.getDisplayLabel());
                if (spec.getPriority() != null) {
                    sb.append(" [").append(spec.getPriority()).append("]");
                }
                if (!spec.getAcceptanceCriteria().isEmpty()) {
                    sb.append(" (AC: ")
                            .append(spec.getCheckedAcceptanceCriteriaCount())
                            .append("/")
                            .append(spec.getAcceptanceCriteria().size())
                            .append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private @NotNull String viewSpec(@NotNull String arguments) {
        String taskId = ToolArgumentParser.getString(arguments, "task_id");
        if (taskId == null || taskId.isEmpty()) {
            return "Error: 'task_id' parameter is required for view_spec operation.";
        }

        SpecService specService = SpecService.getInstance(project);
        TaskSpec spec = specService.getSpec(taskId);

        if (spec == null) {
            return "Error: Task spec with ID '" + taskId + "' not found.";
        }

        return SpecContextBuilder.buildContext(spec);
    }

    private @NotNull String updateSpecStatus(@NotNull String arguments) {
        String taskId = ToolArgumentParser.getString(arguments, "task_id");
        String newStatus = ToolArgumentParser.getString(arguments, "status");

        if (taskId == null || taskId.isEmpty()) {
            return "Error: 'task_id' parameter is required.";
        }
        if (newStatus == null || newStatus.isEmpty()) {
            return "Error: 'status' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        TaskSpec spec = specService.getSpec(taskId);

        if (spec == null) {
            return "Error: Task spec with ID '" + taskId + "' not found.";
        }

        try {
            Path filePath = Paths.get(spec.getFilePath());
            String content = Files.readString(filePath, StandardCharsets.UTF_8);

            // Replace the status field in frontmatter
            String updated = content.replaceFirst(
                    "(?m)^status:\\s*.*$",
                    "status: \"" + newStatus + "\""
            );

            Files.writeString(filePath, updated, StandardCharsets.UTF_8);
            specService.refresh();

            return "Updated task " + taskId + " status to '" + newStatus + "'.";
        } catch (IOException e) {
            return "Error updating spec file: " + e.getMessage();
        }
    }

    private @NotNull String checkAcceptanceCriterion(@NotNull String arguments) {
        String taskId = ToolArgumentParser.getString(arguments, "task_id");
        int criterionIndex = ToolArgumentParser.getInt(arguments, "criterion_index", -1);

        if (taskId == null || taskId.isEmpty()) {
            return "Error: 'task_id' parameter is required.";
        }
        if (criterionIndex < 0) {
            return "Error: 'criterion_index' parameter is required and must be >= 0.";
        }

        SpecService specService = SpecService.getInstance(project);
        TaskSpec spec = specService.getSpec(taskId);

        if (spec == null) {
            return "Error: Task spec with ID '" + taskId + "' not found.";
        }

        if (criterionIndex >= spec.getAcceptanceCriteria().size()) {
            return "Error: criterion_index " + criterionIndex + " is out of range (0-" + (spec.getAcceptanceCriteria().size() - 1) + ").";
        }

        try {
            Path filePath = Paths.get(spec.getFilePath());
            String content = Files.readString(filePath, StandardCharsets.UTF_8);

            // Find and replace the nth checkbox
            String[] lines = content.split("\\n");
            int checkboxCount = 0;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].matches("^\\s*-\\s+\\[[ xX]]\\s+.+$")) {
                    if (checkboxCount == criterionIndex) {
                        lines[i] = lines[i].replaceFirst("\\[[ ]]", "[x]");
                        break;
                    }
                    checkboxCount++;
                }
            }

            Files.writeString(filePath, String.join("\n", lines), StandardCharsets.UTF_8);
            specService.refresh();

            String criterionText = spec.getAcceptanceCriteria().get(criterionIndex).getText();
            return "Checked acceptance criterion #" + criterionIndex + ": \"" + criterionText + "\"";
        } catch (IOException e) {
            return "Error updating spec file: " + e.getMessage();
        }
    }
}
