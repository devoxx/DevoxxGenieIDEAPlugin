package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.BacklogConfig;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.BacklogConfigService;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes the 5 backlog milestone tools: list, add, rename, remove, archive.
 */
@Slf4j
public class BacklogMilestoneToolExecutor implements ToolExecutor {

    private final Project project;

    public BacklogMilestoneToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            return switch (request.name()) {
                case "backlog_milestone_list" -> listMilestones();
                case "backlog_milestone_add" -> addMilestone(request.arguments());
                case "backlog_milestone_rename" -> renameMilestone(request.arguments());
                case "backlog_milestone_remove" -> removeMilestone(request.arguments());
                case "backlog_milestone_archive" -> archiveMilestone(request.arguments());
                default -> "Error: Unknown milestone tool: " + request.name();
            };
        } catch (Exception e) {
            log.warn("Error executing backlog milestone tool: {}", request.name(), e);
            return "Error: " + e.getMessage();
        }
    }

    private @NotNull String listMilestones() {
        BacklogConfigService configService = BacklogConfigService.getInstance(project);
        BacklogConfig config = configService.getConfig();
        List<BacklogConfig.BacklogMilestone> milestones = config.getMilestones();

        if (milestones == null || milestones.isEmpty()) {
            return "No milestones configured.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Milestones (").append(milestones.size()).append("):\n\n");
        for (BacklogConfig.BacklogMilestone m : milestones) {
            sb.append("- ").append(m.getName());
            if (m.getDescription() != null && !m.getDescription().isEmpty()) {
                sb.append(": ").append(m.getDescription());
            }
            sb.append("\n");
        }

        // Also show milestones found on tasks that might not be in config
        SpecService specService = SpecService.getInstance(project);
        List<String> taskMilestones = specService.getAllSpecs().stream()
                .map(TaskSpec::getMilestone)
                .filter(m -> m != null && !m.isEmpty())
                .distinct()
                .toList();

        List<String> configNames = milestones.stream()
                .map(BacklogConfig.BacklogMilestone::getName)
                .toList();

        List<String> extraMilestones = taskMilestones.stream()
                .filter(m -> configNames.stream().noneMatch(c -> c.equalsIgnoreCase(m)))
                .toList();

        if (!extraMilestones.isEmpty()) {
            sb.append("\nMilestones found on tasks (not in config):\n");
            for (String m : extraMilestones) {
                sb.append("- ").append(m).append("\n");
            }
        }

        return sb.toString();
    }

    private @NotNull String addMilestone(@NotNull String arguments) throws Exception {
        String name = ToolArgumentParser.getString(arguments, "name");
        if (name == null || name.isEmpty()) {
            return "Error: 'name' parameter is required.";
        }

        String description = ToolArgumentParser.getString(arguments, "description");

        BacklogConfigService configService = BacklogConfigService.getInstance(project);
        BacklogConfig config = configService.getConfig();

        // Check for duplicate
        if (config.getMilestones() != null) {
            boolean exists = config.getMilestones().stream()
                    .anyMatch(m -> m.getName().equalsIgnoreCase(name));
            if (exists) {
                return "Error: Milestone '" + name + "' already exists.";
            }
        }

        List<BacklogConfig.BacklogMilestone> milestones = config.getMilestones() != null
                ? new ArrayList<>(config.getMilestones()) : new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder()
                .name(name).description(description).build());
        config.setMilestones(milestones);

        configService.saveConfig(config);
        return "Added milestone: " + name;
    }

    private @NotNull String renameMilestone(@NotNull String arguments) throws Exception {
        String from = ToolArgumentParser.getString(arguments, "from");
        String to = ToolArgumentParser.getString(arguments, "to");
        boolean updateTasks = ToolArgumentParser.getBoolean(arguments, "updateTasks", true);

        if (from == null || from.isEmpty()) {
            return "Error: 'from' parameter is required.";
        }
        if (to == null || to.isEmpty()) {
            return "Error: 'to' parameter is required.";
        }

        BacklogConfigService configService = BacklogConfigService.getInstance(project);
        BacklogConfig config = configService.getConfig();

        if (config.getMilestones() == null) {
            return "Error: No milestones configured.";
        }

        boolean found = false;
        for (BacklogConfig.BacklogMilestone m : config.getMilestones()) {
            if (m.getName().equalsIgnoreCase(from)) {
                m.setName(to);
                found = true;
                break;
            }
        }

        if (!found) {
            return "Error: Milestone '" + from + "' not found.";
        }

        configService.saveConfig(config);

        // Update tasks if requested
        int updatedCount = 0;
        if (updateTasks) {
            SpecService specService = SpecService.getInstance(project);
            for (TaskSpec spec : specService.getAllSpecs()) {
                if (from.equalsIgnoreCase(spec.getMilestone())) {
                    spec.setMilestone(to);
                    specService.updateTask(spec);
                    updatedCount++;
                }
            }
        }

        return "Renamed milestone '" + from + "' to '" + to + "'." +
                (updateTasks ? " Updated " + updatedCount + " task(s)." : "");
    }

    private @NotNull String removeMilestone(@NotNull String arguments) throws Exception {
        String name = ToolArgumentParser.getString(arguments, "name");
        if (name == null || name.isEmpty()) {
            return "Error: 'name' parameter is required.";
        }

        String taskHandling = ToolArgumentParser.getString(arguments, "taskHandling");
        if (taskHandling == null) taskHandling = "clear";

        String reassignTo = ToolArgumentParser.getString(arguments, "reassignTo");

        // Validate reassign parameters early, before modifying state
        if ("reassign".equals(taskHandling) && (reassignTo == null || reassignTo.isEmpty())) {
            return "Error: 'reassignTo' is required when taskHandling is 'reassign'.";
        }

        BacklogConfigService configService = BacklogConfigService.getInstance(project);
        BacklogConfig config = configService.getConfig();

        if (config.getMilestones() == null) {
            return "Error: No milestones configured.";
        }

        boolean removed = config.getMilestones().removeIf(m -> m.getName().equalsIgnoreCase(name));
        if (!removed) {
            return "Error: Milestone '" + name + "' not found.";
        }

        configService.saveConfig(config);

        int updatedCount = applyTaskHandling(name, taskHandling, reassignTo);
        return "Removed milestone '" + name + "'. " + taskHandling + " handling applied to " + updatedCount + " task(s).";
    }

    private int applyTaskHandling(@NotNull String milestoneName, @NotNull String taskHandling, String reassignTo) throws IOException {
        return switch (taskHandling) {
            case "clear" -> updateMatchingTasks(milestoneName, null);
            case "reassign" -> updateMatchingTasks(milestoneName, reassignTo);
            default -> 0; // "keep" — do nothing to tasks
        };
    }

    private int updateMatchingTasks(@NotNull String milestoneName, String newMilestone) throws IOException {
        SpecService specService = SpecService.getInstance(project);
        int count = 0;
        for (TaskSpec spec : specService.getAllSpecs()) {
            if (milestoneName.equalsIgnoreCase(spec.getMilestone())) {
                spec.setMilestone(newMilestone);
                specService.updateTask(spec);
                count++;
            }
        }
        return count;
    }

    private @NotNull String archiveMilestone(@NotNull String arguments) throws Exception {
        String name = ToolArgumentParser.getString(arguments, "name");
        if (name == null || name.isEmpty()) {
            return "Error: 'name' parameter is required.";
        }

        BacklogConfigService configService = BacklogConfigService.getInstance(project);
        BacklogConfig config = configService.getConfig();

        if (config.getMilestones() == null) {
            return "Error: No milestones configured.";
        }

        boolean removed = config.getMilestones().removeIf(m -> m.getName().equalsIgnoreCase(name));
        if (!removed) {
            return "Error: Milestone '" + name + "' not found.";
        }

        configService.saveConfig(config);
        return "Archived milestone: " + name;
    }
}
